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

class ReceiverBridge:
    """
    Модуль Моста: Захват данных с приёмника RC по UART, парсинг 3-позиционного тумблера CH6
    и трансляция протокола далее на Raspberry Pi.
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

        # Текущие значения каналов (по умолчанию 1500, AUX=1000)
        self.channels = [1500, 1500, 1500, 1500, 1000, 1000, 1000, 1000]
        self.ch6_mode = MODE_MANUAL # Текущий режим с тумблера CH6 (0=MANUAL, 1=LOCK, 2=TRACKING)

    def connect(self):
        """ Инициализация подключения к портам приемника и Raspberry Pi """
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
        """ Запуск фонового потока чтения каналов и трансляции """
        self.connect()
        self.running = True
        self.thread = threading.Thread(target=self._bridge_loop, daemon=True)
        self.thread.start()
        print("[ReceiverBridge] 🌉 Мост 'Приемник -> UART -> Raspberry Pi' запущен в фоновом режиме.")

    def _parse_incoming_packet(self, data):
        """
        Разбор бинарного пакета команд от приёмника (iBUS / SBUS / CRSF).
        Определяет 3 положения тумблера CH6:
        - НИЗ (<1300): MANUAL (Ручной)
        - СРЕДНЕЕ (1300..1700): LOCK (Захват)
        - ВЕРХ (>=1700): TRACKING (Слежение/Удержание)
        """
        # Разбор iBUS пакета (32 байта, заголовок 0x20 0x40)
        if len(data) >= 32 and data[0] == 0x20 and data[1] == 0x40:
            for i in range(14):
                val = data[2 + i*2] | (data[3 + i*2] << 8)
                if 800 <= val <= 2200 and i < len(self.channels):
                    self.channels[i] = val
            
            # Чтение 6-го канала (CH6 - индекс 5)
            ch6_val = self.channels[5]
            if ch6_val < CH6_LOW_MAX:
                self.ch6_mode = MODE_MANUAL
            elif CH6_LOW_MAX <= ch6_val < CH6_HIGH_MIN:
                self.ch6_mode = MODE_LOCK
            else:
                self.ch6_mode = MODE_TRACKING

    def _bridge_loop(self):
        """ Чтение пакетов с приемника и передача на Raspberry Pi """
        while self.running:
            if self.rx_serial and self.rx_serial.is_open:
                try:
                    data = self.rx_serial.read(128)
                    if data:
                        # Разбор каналов для отслеживания 3 положений CH6
                        self._parse_incoming_packet(data)

                        # Транслируем принятые от приемника данные напрямую на Raspberry Pi
                        if self.rpi_serial and self.rpi_serial.is_open:
                            self.rpi_serial.write(data)
                            self.rpi_serial.flush()
                except Exception:
                    pass
            time.sleep(0.005) # ~200 Гц опрос

    def get_ch6_mode(self):
        """ Возвращает 3-позиционный режим CH6 (0=MANUAL, 1=LOCK, 2=TRACKING) """
        return self.ch6_mode

    def stop(self):
        """ Остановка моста и закрытие портов """
        self.running = False
        if self.rx_serial and self.rx_serial.is_open:
            self.rx_serial.close()
        if self.rpi_serial and self.rpi_serial.is_open:
            self.rpi_serial.close()
        print("[ReceiverBridge] 🛑 Мост остановлен.")


class MSPProtocol:
    """ Класс формирования и отправки пакетов протокола MSP (MultiWii Serial Protocol) """

    MSP_HEADER = b'$M<'
    
    # Команды MSP
    MSP_IDENT = 100
    MSP_STATUS = 101
    MSP_RAW_IMU = 102
    MSP_ATTITUDE = 108
    MSP_SET_RAW_RC = 200 # Управление каналами RC (Roll, Pitch, Yaw, Throttle)

    def __init__(self, port=MSP_PORT, baudrate=MSP_BAUDRATE):
        self.port = port
        self.baudrate = baudrate
        self.serial = None
        self.connected = False

    def connect(self):
        """ Подключение к последовательному порту полетного контроллера """
        try:
            import serial
            self.serial = serial.Serial(self.port, self.baudrate, timeout=0.1)
            self.connected = True
            print(f"[MSP] ✅ Успешное подключение к полетнику: {self.port}@{self.baudrate}")
            return True
        except Exception as e:
            print(f"[MSP] ⚠️ Имитация подключения (порт {self.port} недоступен: {e})")
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

    def send_rc_override(self, roll=RC_CENTER, pitch=RC_CENTER, throttle=RC_THROTTLE, yaw=RC_YAW, aux1=1000, aux2=1000):
        """
        Отправка RC каналов (MSP_SET_RAW_RC = 200)
        Значения в диапазоне 1000 .. 2000 (1500 - центр)
        """
        payload = struct.pack('<HHHHHH', roll, pitch, throttle, yaw, aux1, aux2)
        return self.send_msp_cmd(self.MSP_SET_RAW_RC, payload)

    def convert_delta_to_rc(self, dx, dy, max_angle_delta=MAX_ANGLE_DELTA):
        """
        Преобразование вектора смещения (dx, dy) в пикселях 
        в команды управления каналом Roll/Pitch для удержания центра.
        """
        corr_x = max(-max_angle_delta, min(max_angle_delta, int(dx * GAIN_X)))
        corr_y = max(-max_angle_delta, min(max_angle_delta, int(dy * GAIN_Y)))

        # Roll: +dx сдвигает вправо, Pitch: -dy сдвигает вперед
        rc_roll = RC_CENTER + corr_x
        rc_pitch = RC_CENTER - corr_y

        return rc_roll, rc_pitch


class CameraTracker:
    """ Класс видеозахвата, трекинга объекта и визуальной индикации """

    def __init__(self):
        self.camera_id = CAMERA_ID
        self.target_fps = TARGET_FPS
        self.frame_time = 1.0 / self.target_fps
        self.box_w = BOX_WIDTH
        self.box_h = BOX_HEIGHT

        self.cap = None
        self.tracker = None
        self.current_mode = MODE_MANUAL # Режим: 0=MANUAL (Зеленый), 1=LOCK (Желтый), 2=TRACKING (Красный)
        self.latest_delta = (0, 0)      # (dx, dy)
        self.locked_object_crop = None  # Вырезанное изображение объекта

        # Инициализация протокола MSP
        self.msp = MSPProtocol()
        if ENABLE_MSP:
            self.msp.connect()

        # Инициализация Моста Приемник -> Raspberry Pi
        self.bridge = ReceiverBridge()
        if ENABLE_BRIDGE:
            self.bridge.start()

        # Создаем каталог для сохранения изображений
        os.makedirs(IMAGES_DIR, exist_ok=True)

    def start(self):
        """ Запуск захвата с веб-камеры """
        self.cap = cv2.VideoCapture(self.camera_id)
        if not self.cap.isOpened():
            print(f"❌ Ошибка: Не удалось открыть камеру ID={self.camera_id}")
            return False

        self.cap.set(cv2.CAP_PROP_FRAME_WIDTH, FRAME_WIDTH)
        self.cap.set(cv2.CAP_PROP_FRAME_HEIGHT, FRAME_HEIGHT)
        self.cap.set(cv2.CAP_PROP_FPS, self.target_fps)
        return True

    def set_mode(self, target_mode, frame=None):
        """ Установка режима работы (0=MANUAL, 1=LOCK, 2=TRACKING) """
        if target_mode == self.current_mode:
            return

        prev_mode = self.current_mode
        self.current_mode = target_mode

        if target_mode == MODE_MANUAL:
            # Ручной режим пилота — сброс трекера и нейтраль MSP
            self.tracker = None
            self.locked_object_crop = None
            self.msp.send_rc_override(RC_CENTER, RC_CENTER, RC_THROTTLE, RC_YAW)
            print("\n[DronT16] 🟢 Режим 0: РУЧНОЕ УПРАВЛЕНИЕ (Пилот рулит). Трекинг выключен.")

        elif target_mode == MODE_LOCK:
            # Среднее положение: ЗАХВАТ объекта (Желтый)
            if frame is not None:
                self.lock_target(frame)
            else:
                print("\n[DronT16] 🟡 Режим 1: ЗАХВАТ (Желтый). Ожидание кадра...")

        elif target_mode == MODE_TRACKING:
            # Верхнее положение: СЛЕЖЕНИЕ / УДЕРЖАНИЕ (Красный)
            if prev_mode == MODE_MANUAL and frame is not None:
                self.lock_target(frame)
            print("\n[DronT16] 🔴 Режим 2: СЛЕЖЕНИЕ / УДЕРЖАНИЕ (Красный). Передача команд полетнику!")

    def send_tracking_control(self, dx, dy):
        """
        Преобразование вектора смещения (dx, dy) в RC каналы
        и отправка MSP пакета управления на полетный контроллер (в режиме СЛЕЖЕНИЯ).
        """
        if not ENABLE_MSP or self.current_mode != MODE_TRACKING:
            return RC_CENTER, RC_CENTER

        # Пересчет смещения пикселей в значения Roll и Pitch
        rc_roll, rc_pitch = self.msp.convert_delta_to_rc(dx, dy)

        # Отправка пакета MSP_SET_RAW_RC
        self.msp.send_rc_override(
            roll=rc_roll,
            pitch=rc_pitch,
            throttle=RC_THROTTLE,
            yaw=RC_YAW
        )

        return rc_roll, rc_pitch

    def process_frame(self):
        """ Обработка текущего кадра, считывание 3 положений CH6 и отрисовка чистой графики """
        ret, frame = self.cap.read()
        if not ret:
            return None, (0, 0), False

        # Опрос 3-позиционного тумблера CH6 с приемника
        if self.bridge:
            rx_mode = self.bridge.get_ch6_mode()
            if rx_mode != self.current_mode:
                self.set_mode(rx_mode, frame)

        h, w, _ = frame.shape
        center_x, center_y = w // 2, h // 2
        dx, dy = 0, 0
        target_found = False
        rc_roll, rc_pitch = RC_CENTER, RC_CENTER

        # 1. Если активирован режим ЗАХВАТА (1) или СЛЕЖЕНИЯ (2) и есть трекер
        if self.current_mode in (MODE_LOCK, MODE_TRACKING) and self.tracker is not None:
            success, bbox = self.tracker.update(frame)
            if success:
                x, y, bw, bh = [int(v) for v in bbox]
                obj_center_x = x + bw // 2
                obj_center_y = y + bh // 2

                dx = obj_center_x - center_x
                dy = obj_center_y - center_y
                target_found = True

                # Если режим СЛЕЖЕНИЯ (2) — посылаем MSP команды полетнику
                if self.current_mode == MODE_TRACKING:
                    rc_roll, rc_pitch = self.send_tracking_control(dx, dy)
                    theme_color = (0, 0, 255)   # КРАСНЫЙ (Слежение / Удержание)
                    mode_label = "УДЕРЖАНИЕ (Красный)"
                else:
                    rc_roll, rc_pitch = RC_CENTER, RC_CENTER
                    theme_color = (0, 255, 255) # ЖЕЛТЫЙ (Захват)
                    mode_label = "ЗАХВАТ (Желтый)"

                # Отрисовка ЧИСТОЙ ГРАФИКИ (только рамка, линия и прицельный крестик)
                cv2.rectangle(frame, (x, y), (x + bw, y + bh), theme_color, 2)
                cv2.circle(frame, (obj_center_x, obj_center_y), 4, theme_color, -1)
                cv2.drawMarker(frame, (center_x, center_y), theme_color,
                               markerType=cv2.MARKER_CROSS, markerSize=20, thickness=2)
                cv2.arrowedLine(frame, (center_x, center_y), (obj_center_x, obj_center_y),
                                theme_color, 2, tipLength=0.2)
                
                # Вывод статуса ИСКЛЮЧИТЕЛЬНО в консоль
                print(f"[{mode_label}] | dX:{dx:+4d} dY:{dy:+4d} | Roll:{rc_roll} Pitch:{rc_pitch}", end="\r", flush=True)
            else:
                # Потеря объекта
                self.send_tracking_control(0, 0)
                print("❌ TARGET LOST! Neutralizing RC...                 ", end="\r", flush=True)
        else:
            # 0. Режим РУЧНОЙ (Зеленый прицел по центру кадра)
            rx = center_x - self.box_w // 2
            ry = center_y - self.box_h // 2
            cv2.rectangle(frame, (rx, ry), (rx + self.box_w, ry + self.box_h), (0, 255, 0), 2)
            cv2.drawMarker(frame, (center_x, center_y), (0, 255, 0),
                           markerType=cv2.MARKER_CROSS, markerSize=16, thickness=1)

        self.latest_delta = (dx, dy)
        return frame, (dx, dy), target_found

    def lock_target(self, frame):
        """ Захват объекта по центру кадра и сохранение его изображения """
        h, w, _ = frame.shape
        center_x, center_y = w // 2, h // 2
        rx = max(0, center_x - self.box_w // 2)
        ry = max(0, center_y - self.box_h // 2)
        rw = min(self.box_w, w - rx)
        rh = min(self.box_h, h - ry)
        bbox = (rx, ry, rw, rh)

        # Вырезаем область захваченного объекта
        self.locked_object_crop = frame[ry:ry+rh, rx:rx+rw].copy() if rw > 0 and rh > 0 else None

        # Сохраняем кроп объекта в файл
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
        """ Сброс в ручной режим """
        self.set_mode(MODE_MANUAL)

    def release(self):
        """ Освобождение ресурсов камеры и моста """
        if self.bridge:
            self.bridge.stop()
        if self.cap is not None:
            self.cap.release()
