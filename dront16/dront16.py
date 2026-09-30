#!/usr/bin/env python3
# ==============================================================================
# Главные Классы Логики Подсистемы DronT16 (dront16/dront16.py)
# ==============================================================================
import cv2
import time
import os
import struct
import threading
import config

class ReceiverBridge:
    """
    Модуль Моста: Захват данных с приёмника RC по UART, парсинг каналов (включая CH6)
    и трансляция протокола далее на Raspberry Pi.
    """
    def __init__(self, rx_port=config.RX_UART_PORT, rx_baud=config.RX_UART_BAUDRATE,
                 rpi_port=config.RPI_UART_PORT, rpi_baud=config.RPI_UART_BAUDRATE):
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
        self.ch6_active = False # Флаг активности канала CH6 (тумблер включен)

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
        Разбор бинарного пакета команд от приёмника (IBUS / SBUS / MSP).
        Извлекает каналы управления, проверяет тумблер CH6 (Канал 6).
        """
        # Разбор iBUS пакета (32 байта, заголовок 0x20 0x40)
        if len(data) >= 32 and data[0] == 0x20 and data[1] == 0x40:
            for i in range(14):
                val = data[2 + i*2] | (data[3 + i*2] << 8)
                if 800 <= val <= 2200 and i < len(self.channels):
                    self.channels[i] = val
            
            # Чтение 6-го канала (CH6 - индекс 5)
            ch6_val = self.channels[5]
            self.ch6_active = (ch6_val >= config.CH6_THRESHOLD)

    def _bridge_loop(self):
        """ Чтение пакетов с приемника и передача на Raspberry Pi """
        while self.running:
            if self.rx_serial and self.rx_serial.is_open:
                try:
                    data = self.rx_serial.read(128)
                    if data:
                        # Разбор каналов для отслеживания CH6
                        self._parse_incoming_packet(data)

                        # Транслируем принятые от приемника данные напрямую на Raspberry Pi
                        if self.rpi_serial and self.rpi_serial.is_open:
                            self.rpi_serial.write(data)
                            self.rpi_serial.flush()
                except Exception:
                    pass
            time.sleep(0.005) # ~200 Гц опрос

    def get_ch6_state(self):
        """ Возвращает состояние тумблера CH6 (True = Включен) """
        return self.ch6_active

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

    def __init__(self, port=config.MSP_PORT, baudrate=config.MSP_BAUDRATE):
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

    def send_rc_override(self, roll=config.RC_CENTER, pitch=config.RC_CENTER, throttle=config.RC_THROTTLE, yaw=config.RC_YAW, aux1=1000, aux2=1000):
        """
        Отправка RC каналов (MSP_SET_RAW_RC = 200)
        Значения в диапазоне 1000 .. 2000 (1500 - центр)
        """
        payload = struct.pack('<HHHHHH', roll, pitch, throttle, yaw, aux1, aux2)
        return self.send_msp_cmd(self.MSP_SET_RAW_RC, payload)

    def convert_delta_to_rc(self, dx, dy, max_angle_delta=config.MAX_ANGLE_DELTA):
        """
        Преобразование вектора смещения (dx, dy) в пикселях 
        в команды управления каналом Roll/Pitch для удержания центра.
        """
        corr_x = max(-max_angle_delta, min(max_angle_delta, int(dx * config.GAIN_X)))
        corr_y = max(-max_angle_delta, min(max_angle_delta, int(dy * config.GAIN_Y)))

        # Roll: +dx сдвигает вправо, Pitch: -dy сдвигает вперед
        rc_roll = config.RC_CENTER + corr_x
        rc_pitch = config.RC_CENTER - corr_y

        return rc_roll, rc_pitch


class CameraTracker:
    """ Класс видеозахвата, трекинга объекта и визуальной индикации """

    def __init__(self):
        self.camera_id = config.CAMERA_ID
        self.target_fps = config.TARGET_FPS
        self.frame_time = 1.0 / self.target_fps
        self.box_w = config.BOX_WIDTH
        self.box_h = config.BOX_HEIGHT

        self.cap = None
        self.tracker = None
        self.tracking = False
        self.msp_enabled_active = False # Флаг режима СЛЕЖЕНИЯ (по кнопке '1' или по CH6)
        self.latest_delta = (0, 0)      # (dx, dy)
        self.locked_object_crop = None  # Вырезанное изображение объекта

        # Инициализация протокола MSP
        self.msp = MSPProtocol()
        if config.ENABLE_MSP:
            self.msp.connect()

        # Инициализация Моста Приемник -> Raspberry Pi
        self.bridge = ReceiverBridge()
        if getattr(config, 'ENABLE_BRIDGE', False):
            self.bridge.start()

        # Создаем каталог для сохранения изображений
        os.makedirs(config.IMAGES_DIR, exist_ok=True)

    def start(self):
        """ Запуск захвата с веб-камеры """
        self.cap = cv2.VideoCapture(self.camera_id)
        if not self.cap.isOpened():
            print(f"❌ Ошибка: Не удалось открыть камеру ID={self.camera_id}")
            return False

        self.cap.set(cv2.CAP_PROP_FRAME_WIDTH, config.FRAME_WIDTH)
        self.cap.set(cv2.CAP_PROP_FRAME_HEIGHT, config.FRAME_HEIGHT)
        self.cap.set(cv2.CAP_PROP_FPS, self.target_fps)
        return True

    def toggle_msp_tracking(self, force_state=None):
        """
        Переключение между режимом ЗАХВАТ (Желтый) и СЛЕЖЕНИЕ (Красный).
        Может вызываться кнопкой '1' или по сигналу CH6 с приемника.
        """
        new_state = not self.msp_enabled_active if force_state is None else force_state
        if new_state != self.msp_enabled_active:
            self.msp_enabled_active = new_state
            if self.msp_enabled_active:
                print("\n[DronT16] 🔴 АКТИВИРОВАН Режим СЛЕЖЕНИЕ (Сигнал CH6 / Клавиша '1'). Управление передано Raspberry Pi!")
            else:
                self.msp.send_rc_override(config.RC_CENTER, config.RC_CENTER, config.RC_THROTTLE, config.RC_YAW)
                print("\n[DronT16] 🟡 Возврат в режим ЗАХВАТ. Каналы в нейтрали (1500).")

    def send_tracking_control(self, dx, dy):
        """
        Преобразование вектора смещения (dx, dy) в RC каналы
        и отправка MSP пакета управления на полетный контроллер (в режиме СЛЕЖЕНИЯ).
        """
        if not config.ENABLE_MSP or not self.msp_enabled_active:
            return config.RC_CENTER, config.RC_CENTER

        # Пересчет смещения пикселей в значения Roll и Pitch
        rc_roll, rc_pitch = self.msp.convert_delta_to_rc(dx, dy)

        # Отправка пакета MSP_SET_RAW_RC
        self.msp.send_rc_override(
            roll=rc_roll,
            pitch=rc_pitch,
            throttle=config.RC_THROTTLE,
            yaw=config.RC_YAW
        )

        return rc_roll, rc_pitch

    def process_frame(self):
        """ Обработка текущего кадра, трекинг, считывание CH6 и отрисовка графики """
        ret, frame = self.cap.read()
        if not ret:
            return None, (0, 0), False

        # Опрос приемника: если тумблер CH6 включен — автоматически активируем режим СЛЕЖЕНИЯ!
        if self.bridge and self.tracking:
            ch6_state = self.bridge.get_ch6_state()
            if ch6_state != self.msp_enabled_active:
                self.toggle_msp_tracking(force_state=ch6_state)

        h, w, _ = frame.shape
        center_x, center_y = w // 2, h // 2
        dx, dy = 0, 0
        target_found = False
        rc_roll, rc_pitch = config.RC_CENTER, config.RC_CENTER

        if self.tracking and self.tracker is not None:
            success, bbox = self.tracker.update(frame)
            if success:
                x, y, bw, bh = [int(v) for v in bbox]
                obj_center_x = x + bw // 2
                obj_center_y = y + bh // 2

                dx = obj_center_x - center_x
                dy = obj_center_y - center_y
                target_found = True

                # Функция слежения: отправка MSP команд коррекции в режиме СЛЕЖЕНИЕ
                rc_roll, rc_pitch = self.send_tracking_control(dx, dy)

                # Цветовая схема:
                # Режим СЛЕЖЕНИЕ (msp_enabled_active == True) -> КРАСНЫЙ (0, 0, 255)
                # Режим ЗАХВАТ (msp_enabled_active == False)  -> ЖЕЛТЫЙ (0, 255, 255)
                if self.msp_enabled_active:
                    theme_color = (0, 0, 255)   # Красный
                    mode_label = "СЛЕЖЕНИЕ (Raspberry)"
                else:
                    theme_color = (0, 255, 255) # Желтый
                    mode_label = "ЗАХВАТ"

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
                # Потеря объекта — возвращаем каналы в нейтральное положение (1500)
                self.send_tracking_control(0, 0)
                print("❌ TARGET LOST! Neutralizing RC...                 ", end="\r", flush=True)
        else:
            # Режим ожидания наведения (зеленая рамка и крестик прицела)
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
            filepath = os.path.join(config.IMAGES_DIR, filename)
            cv2.imwrite(filepath, frame[ry:ry+rh, rx:rx+rw])

        try:
            self.tracker = cv2.TrackerMIL_create()
        except AttributeError:
            self.tracker = cv2.TrackerMIL.create()

        self.tracker.init(frame, bbox)
        self.tracking = True
        print(f"\n[DronT16] 🟡 Захват объекта активирован (Режим ЗАХВАТ): BBox={bbox}")

    def reset_target(self):
        """ Сброс захвата объекта """
        self.tracking = False
        self.tracker = None
        self.msp_enabled_active = False # Выключаем режим СЛЕЖЕНИЯ
        self.latest_delta = (0, 0)
        self.locked_object_crop = None
        self.send_tracking_control(0, 0) # Сброс RC каналов в центр (1500)
        print("\n[DronT16] 🔄 Сброс захвата. Каналы переведены в нейтраль (1500).")

    def release(self):
        """ Освобождение ресурсов камеры и моста """
        if self.bridge:
            self.bridge.stop()
        if self.cap is not None:
            self.cap.release()
