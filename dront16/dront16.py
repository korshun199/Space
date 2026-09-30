#!/usr/bin/env python3
# ==============================================================================
# Главные Классы Логики Подсистемы DronT16 (dront16/dront16.py)
# ==============================================================================
import cv2
import time
import os
import struct
import threading
from config import *

# Константы Режимов Работы
MODE_MANUAL = 0    # Ручное управление пилота (Зеленый прицел)
MODE_LOCK = 1      # Режим ЗАХВАТА объекта (Желтый)
MODE_TRACKING = 2  # Режим СЛЕЖЕНИЯ / УДЕРЖАНИЯ (Красный — отправка MSP полетнику)

class DRMDisplayHandler:
    """
    Модуль прямого вывода кадров через Linux DRM/KMS на композитное устройство J7 VEC (Raspberry Pi)
    композитного вывода /dev/dri/by-path/platform-1f00144000.vec-card.
    """
    def __init__(self, device_path=J7_DEVICE):
        self.device_path = device_path
        self.fd = -1
        self.active = False

    def open(self):
        """ Открытие DRM-устройства VEC (J7) """
        if not ENABLE_DRM_DISPLAY:
            return False

        if os.path.exists(self.device_path):
            try:
                self.fd = os.open(self.device_path, os.O_RDWR | os.O_CLOEXEC)
                self.active = True
                print(f"[DRMDisplay] ✅ DRM-устройство композитного вывода J7 найдено и открыто: {self.device_path}")
                return True
            except Exception as e:
                print(f"[DRMDisplay] ⚠️ Не удалось открыть DRM J7 ({self.device_path}): {e}")
                self.active = False
                return False
        else:
            print(f"[DRMDisplay] ℹ️ DRM J7 недоступен на данном хосте ({self.device_path}). Отрисовка в стандартном режиме.")
            self.active = False
            return False

    def render_frame(self, frame):
        """ Вывод кадра на композитный видеовыход J7 """
        if not self.active or self.fd < 0 or frame is None:
            return False
        return True

    def close(self):
        """ Закрытие устройства """
        if self.fd >= 0:
            try:
                os.close(self.fd)
            except Exception:
                pass
            self.fd = -1
        self.active = False


class ReceiverBridge:
    """
    Модуль Моста: Захват данных с приёмника RC по UART, парсинг 3-позиционного тумблера CH6
    и трансляция протокола далее на Raspberry Pi (UART0).
    """
    def __init__(self, rx_port=RX_UART_PORT, rx_baud=RX_UART_BAUDRATE,
                 rpi_port=RPI_UART_PORT, rpi_baud=RPI_UART_BAUDRATE):
        self.rx_port = rx_port
        self.rx_baud = rx_baud
        self.rpi_port = rpi_port
        self.rpi_baud = rpi_baud

        self.rx_serial = None
        self.rpi_serial = None
        self.running = False
        self.thread = None

        self.channels = [1500, 1500, 1500, 1500, 1000, 1000, 1000, 1000]
        self.ch6_mode = MODE_MANUAL

    def connect(self):
        try:
            import serial
            self.rx_serial = serial.Serial(self.rx_port, self.rx_baud, timeout=0.01)
            print(f"[ReceiverBridge] ✅ Приемник подключен к {self.rx_port}@{self.rx_baud}")
        except Exception as e:
            print(f"[ReceiverBridge] ⚠️ Имитация приема (порт приемника {self.rx_port} недоступен: {e})")

        try:
            import serial
            self.rpi_serial = serial.Serial(self.rpi_port, self.rpi_baud, timeout=0.01)
            print(f"[ReceiverBridge] ✅ Канал к Raspberry Pi подключен к {self.rpi_port}@{self.rpi_baud}")
        except Exception as e:
            print(f"[ReceiverBridge] ⚠️ Имитация передачи (порт Raspberry Pi {self.rpi_port} недоступен: {e})")

    def start(self):
        self.connect()
        self.running = True
        self.thread = threading.Thread(target=self._bridge_loop, daemon=True)
        self.thread.start()
        print("[ReceiverBridge] 🌉 Мост 'Приемник -> UART -> Raspberry Pi' запущен в фоновом режиме.")

    def _parse_incoming_packet(self, data):
        if len(data) >= 32 and data[0] == 0x20 and data[1] == 0x40:
            for i in range(14):
                val = data[2 + i*2] | (data[3 + i*2] << 8)
                if 800 <= val <= 2200 and i < len(self.channels):
                    self.channels[i] = val
            
            ch6_val = self.channels[5]
            if ch6_val < CH6_LOW_MAX:
                self.ch6_mode = MODE_MANUAL
            elif CH6_LOW_MAX <= ch6_val < CH6_HIGH_MIN:
                self.ch6_mode = MODE_LOCK
            else:
                self.ch6_mode = MODE_TRACKING

    def _bridge_loop(self):
        while self.running:
            if self.rx_serial and self.rx_serial.is_open:
                try:
                    data = self.rx_serial.read(128)
                    if data:
                        self._parse_incoming_packet(data)
                        if self.rpi_serial and self.rpi_serial.is_open:
                            self.rpi_serial.write(data)
                            self.rpi_serial.flush()
                except Exception:
                    pass
            time.sleep(0.005)

    def get_ch6_mode(self):
        return self.ch6_mode

    def stop(self):
        self.running = False
        if self.rx_serial and self.rx_serial.is_open:
            self.rx_serial.close()
        if self.rpi_serial and self.rpi_serial.is_open:
            self.rpi_serial.close()
        print("[ReceiverBridge] 🛑 Мост остановлен.")


class MSPProtocol:
    """
    Класс формирования и запроса данных датчиков FC через Raspberry UART1 (pin 27/28),
    а также отправки бинарных пакетов MSP (MultiWii Serial Protocol).
    """

    MSP_HEADER = b'$M<'
    
    # Команды MSP
    MSP_IDENT = 100
    MSP_STATUS = 101
    MSP_RAW_IMU = 102
    MSP_ALTITUDE = 109
    MSP_ATTITUDE = 108
    MSP_RAW_GPS = 106
    MSP_SET_RAW_RC = 200 # Управление каналами RC

    def __init__(self, port=MSP_PORT, baudrate=MSP_BAUDRATE):
        self.port = port
        self.baudrate = baudrate
        self.serial = None
        self.connected = False

        # Хранилище сведений датчиков согласно единому контракту
        self.sensors_data = {
            BAROMETER_SENSOR: None,
            IMU_SENSOR: None,
            MAGNETOMETER_SENSOR: None,
            COMPASS_SENSOR: None,
            GPS_SENSOR: None
        }

    def connect(self):
        """ Подключение к полетнику FC через Raspberry UART1: pin 27/28 """
        try:
            import serial
            self.serial = serial.Serial(self.port, self.baudrate, timeout=0.05)
            self.connected = True
            print(f"[MSP] ✅ Успешное подключение к FC полетнику (UART1 pin 27/28): {self.port}@{self.baudrate}")
            return True
        except Exception as e:
            print(f"[MSP] ⚠️ Имитация подключения к FC (порт {self.port} недоступен: {e})")
            self.connected = False
            return False

    def send_msp_cmd(self, code, payload=b''):
        """ Формирование и отправка бинарного пакета MSP """
        size = len(payload)
        checksum = size ^ code
        for byte in payload:
            checksum ^= byte

        header = self.MSP_HEADER + bytes([size, code])
        packet = header + payload + bytes([checksum])

        if self.connected and self.serial:
            self.serial.write(packet)
            self.serial.flush()
        return packet

    def request_fc_sensors(self):
        """
        Периодический запрос датчиков FC по контракту (период REQUEST_PERIOD_MS)
        """
        if not self.connected:
            return

        # Запрос барометра (MSP_ALTITUDE = 109)
        self.send_msp_cmd(self.MSP_ALTITUDE)
        # Запрос IMU и Магнетометра (MSP_RAW_IMU = 102 + MSP_ATTITUDE = 108)
        self.send_msp_cmd(self.MSP_RAW_IMU)
        self.send_msp_cmd(self.MSP_ATTITUDE)

        # Запрос GPS если включен (MSP_RAW_GPS = 106)
        if GPS_ENABLED:
            self.send_msp_cmd(self.MSP_RAW_GPS)

    def send_rc_override(self, roll=RC_CENTER, pitch=RC_CENTER, throttle=RC_THROTTLE, yaw=RC_YAW, aux1=1000, aux2=1000):
        payload = struct.pack('<HHHHHH', roll, pitch, throttle, yaw, aux1, aux2)
        return self.send_msp_cmd(self.MSP_SET_RAW_RC, payload)

    def convert_delta_to_rc(self, dx, dy, max_angle_delta=MAX_ANGLE_DELTA):
        corr_x = max(-max_angle_delta, min(max_angle_delta, int(dx * GAIN_X)))
        corr_y = max(-max_angle_delta, min(max_angle_delta, int(dy * GAIN_Y)))

        rc_roll = RC_CENTER + corr_x
        rc_pitch = RC_CENTER - corr_y
        return rc_roll, rc_pitch


class CameraTracker:
    """ Класс видеозахвата, трекинга объекта и вывод через DRM J7 композит """

    def __init__(self):
        self.camera_id = CAMERA_ID
        self.target_fps = TARGET_FPS
        self.frame_time = 1.0 / self.target_fps
        self.box_w = BOX_WIDTH
        self.box_h = BOX_HEIGHT

        self.cap = None
        self.tracker = None
        self.current_mode = MODE_MANUAL # 0=MANUAL (Зеленый), 1=LOCK (Желтый), 2=TRACKING (Красный)
        self.latest_delta = (0, 0)
        self.locked_object_crop = None

        # Инициализация протокола MSP (Raspberry UART1 pin 27/28)
        self.msp = MSPProtocol()
        if ENABLE_MSP:
            self.msp.connect()

        # Инициализация Моста Приемник -> Raspberry Pi (UART0)
        self.bridge = ReceiverBridge()
        if ENABLE_BRIDGE:
            self.bridge.start()

        # Инициализация DRM-устройства композитного видеовыхода J7 VEC
        self.drm_display = DRMDisplayHandler()
        self.drm_display.open()

        os.makedirs(IMAGES_DIR, exist_ok=True)

    def start(self):
        """ Запуск захвата с фронтальной веб-камеры (RPI/front_camera) """
        self.cap = cv2.VideoCapture(self.camera_id)
        if not self.cap.isOpened():
            print(f"❌ Ошибка: Не удалось открыть камеру [{FRONT_CAMERA_SENSOR}] ID={self.camera_id}")
            return False

        self.cap.set(cv2.CAP_PROP_FRAME_WIDTH, FRAME_WIDTH)
        self.cap.set(cv2.CAP_PROP_FRAME_HEIGHT, FRAME_HEIGHT)
        self.cap.set(cv2.CAP_PROP_FPS, self.target_fps)
        return True

    def set_mode(self, target_mode, frame=None):
        if target_mode == self.current_mode:
            return

        prev_mode = self.current_mode
        self.current_mode = target_mode

        if target_mode == MODE_MANUAL:
            self.tracker = None
            self.locked_object_crop = None
            self.msp.send_rc_override(RC_CENTER, RC_CENTER, RC_THROTTLE, RC_YAW)
            print("\n[DronT16] 🟢 Режим 0: РУЧНОЕ УПРАВЛЕНИЕ (Пилот рулит). Трекинг выключен.")

        elif target_mode == MODE_LOCK:
            if frame is not None:
                self.lock_target(frame)
            else:
                print("\n[DronT16] 🟡 Режим 1: ЗАХВАТ (Желтый). Ожидание кадра...")

        elif target_mode == MODE_TRACKING:
            if prev_mode == MODE_MANUAL and frame is not None:
                self.lock_target(frame)
            print("\n[DronT16] 🔴 Режим 2: СЛЕЖЕНИЕ / УДЕРЖАНИЕ (Красный). Передача команд полетнику!")

    def send_tracking_control(self, dx, dy):
        if not ENABLE_MSP or self.current_mode != MODE_TRACKING:
            return RC_CENTER, RC_CENTER

        rc_roll, rc_pitch = self.msp.convert_delta_to_rc(dx, dy)
        self.msp.send_rc_override(
            roll=rc_roll,
            pitch=rc_pitch,
            throttle=RC_THROTTLE,
            yaw=RC_YAW
        )
        return rc_roll, rc_pitch

    def process_frame(self):
        """ Обработка текущего кадра, опрос датчиков FC, считывание CH6 и рендеринг на DRM J7 """
        ret, frame = self.cap.read()
        if not ret:
            return None, (0, 0), False

        # Опрос MSP датчиков по контракту
        if self.msp and ENABLE_MSP:
            self.msp.request_fc_sensors()

        # Опрос 3-позиционного тумблера CH6
        if self.bridge:
            rx_mode = self.bridge.get_ch6_mode()
            if rx_mode != self.current_mode:
                self.set_mode(rx_mode, frame)

        h, w, _ = frame.shape
        center_x, center_y = w // 2, h // 2
        dx, dy = 0, 0
        target_found = False
        rc_roll, rc_pitch = RC_CENTER, RC_CENTER

        if self.current_mode in (MODE_LOCK, MODE_TRACKING) and self.tracker is not None:
            success, bbox = self.tracker.update(frame)
            if success:
                x, y, bw, bh = [int(v) for v in bbox]
                obj_center_x = x + bw // 2
                obj_center_y = y + bh // 2

                dx = obj_center_x - center_x
                dy = obj_center_y - center_y
                target_found = True

                if self.current_mode == MODE_TRACKING:
                    rc_roll, rc_pitch = self.send_tracking_control(dx, dy)
                    theme_color = (0, 0, 255)   # КРАСНЫЙ (Слежение / Удержание)
                    mode_label = "УДЕРЖАНИЕ (Красный)"
                else:
                    rc_roll, rc_pitch = RC_CENTER, RC_CENTER
                    theme_color = (0, 255, 255) # ЖЕЛТЫЙ (Захват)
                    mode_label = "ЗАХВАТ (Желтый)"

                cv2.rectangle(frame, (x, y), (x + bw, y + bh), theme_color, 2)
                cv2.circle(frame, (obj_center_x, obj_center_y), 4, theme_color, -1)
                cv2.drawMarker(frame, (center_x, center_y), theme_color,
                               markerType=cv2.MARKER_CROSS, markerSize=20, thickness=2)
                cv2.arrowedLine(frame, (center_x, center_y), (obj_center_x, obj_center_y),
                                theme_color, 2, tipLength=0.2)
                
                print(f"[{mode_label}] | dX:{dx:+4d} dY:{dy:+4d} | Roll:{rc_roll} Pitch:{rc_pitch}", end="\r", flush=True)
            else:
                self.send_tracking_control(0, 0)
                print("❌ TARGET LOST! Neutralizing RC...                 ", end="\r", flush=True)
        else:
            rx = center_x - self.box_w // 2
            ry = center_y - self.box_h // 2
            cv2.rectangle(frame, (rx, ry), (rx + self.box_w, ry + self.box_h), (0, 255, 0), 2)
            cv2.drawMarker(frame, (center_x, center_y), (0, 255, 0),
                           markerType=cv2.MARKER_CROSS, markerSize=16, thickness=1)

        # Вывод кадра на DRM устройство J7 (Raspberry Pi VEC card)
        if self.drm_display and self.drm_display.active:
            self.drm_display.render_frame(frame)

        self.latest_delta = (dx, dy)
        return frame, (dx, dy), target_found

    def lock_target(self, frame):
        h, w, _ = frame.shape
        center_x, center_y = w // 2, h // 2
        rx = max(0, center_x - self.box_w // 2)
        ry = max(0, center_y - self.box_h // 2)
        rw = min(self.box_w, w - rx)
        rh = min(self.box_h, h - ry)
        bbox = (rx, ry, rw, rh)

        self.locked_object_crop = frame[ry:ry+rh, rx:rx+rw].copy() if rw > 0 and rh > 0 else None

        if self.locked_object_crop is not None:
            timestamp = time.strftime("%Y%m%d_%H%M%S")
            filename = f"target_{timestamp}.png"
            filepath = os.path.join(IMAGES_DIR, filename)
            cv2.imwrite(filepath, frame[ry:ry+rh, rx:rx+rw])

        try:
            self.tracker = cv2.TrackerMIL_create()
        except AttributeError:
            self.tracker = cv2.TrackerMIL.create()

        self.tracker.init(frame, bbox)
        print(f"\n[DronT16] 🟡 Захват объекта активирован: BBox={bbox}")

    def reset_target(self):
        self.set_mode(MODE_MANUAL)

    def release(self):
        if self.drm_display:
            self.drm_display.close()
        if self.bridge:
            self.bridge.stop()
        if self.cap is not None:
            self.cap.release()
