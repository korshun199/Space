#!/usr/bin/env python3
# ==============================================================================
# Файл Конфигурации и Параметров Подсистемы (dront16/config.py)
# ==============================================================================
import os

# --- Параметры Камеры ---
CAMERA_ID = 0           # Индекс видеокамеры
FRAME_WIDTH = 640       # Ширина кадра (px)
FRAME_HEIGHT = 480      # Высота кадра (px)
TARGET_FPS = 25         # Скорость захвата кадров (FPS)

# --- DRM Устройство Композитного Видеовыхода J7 (Raspberry Pi VEC Card) ---
ENABLE_DRM_DISPLAY = True
J7_DEVICE = "/dev/dri/by-path/platform-1f00144000.vec-card" # DRM-устройство композита J7

# --- Единый Контракт Датчиков FC и Камеры ---
BAROMETER_SENSOR = "FC/MSP_ALTITUDE"
IMU_SENSOR = "FC/MSP_ATTITUDE+MSP_RAW_IMU"
MAGNETOMETER_SENSOR = "FC/MSP_RAW_IMU"
COMPASS_SENSOR = "FC/MSP_RAW_IMU_XY_DIAGNOSTIC"
GPS_SENSOR = "FC/MSP_RAW_GPS"
FRONT_CAMERA_SENSOR = "RPI/front_camera"

# --- Параметры OSD (On-Screen Display) ---
CAPTURE_BOX_SIZE = 53
CROSSHAIR_ARM = 8
LINE_THICKNESS = 2
TARGET_POINT_RADIUS = 4
MODE_FONT_SCALE = 0.8
MODE_FONT_THICKNESS = 2
MESSAGE_FONT_SCALE = 0.55
MESSAGE_FONT_THICKNESS = 2
MODE_X = 20
MODE_Y = 32
CENTER_X_PERCENT = 50.0
CENTER_Y_PERCENT = 50.0
CENTER_OFFSET_X = -10
CENTER_OFFSET_Y = -10

# --- Параметры Рамки Прицела / Захвата ---
BOX_WIDTH = CAPTURE_BOX_SIZE    # Ширина рамки прицела (px)
BOX_HEIGHT = CAPTURE_BOX_SIZE   # Высота рамки прицела (px)

# --- Хранение Изображений Захваченного Объект ---
IMAGES_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "images") # Каталог сохранения

# --- Параметры MSP и Связи с Полетником (FC через Raspberry UART1: pin 27/28) ---
ENABLE_MSP = True       # Включить отправку команд по MSP
MSP_PORT = "/dev/ttyAMA1" # Последовательный порт полетника FC (Raspberry UART1, pin 27/28)
MSP_BAUDRATE = 115200   # Скорость порта MSP
REQUEST_PERIOD_MS = 50  # Период запроса датчиков (мс)
SENSOR_MAX_AGE_MS = 250 # Максимальный возраст данных датчиков (мс)
GPS_ENABLED = True      # Запрашивать данные GPS через read-only MSP канал FC

RC_CENTER = 1500        # Нейтральное значение каналов RC (1000..2000)
RC_THROTTLE = 1500      # Канал газа Throttle
RC_YAW = 1500           # Канал рыскания Yaw
MAX_ANGLE_DELTA = 150   # Максимальная величина коррекции (px -> RC)
GAIN_X = 0.8            # Коэффициент усиления канала Roll (dX -> RC)
GAIN_Y = 0.8            # Коэффициент усиления канала Pitch (dY -> RC)

# --- Параметры Моста Приемника (Bridge: Receiver -> UART -> Raspberry Pi) ---
ENABLE_BRIDGE = True            # Флаг включения моста
RX_UART_PORT = "/dev/ttyUSB1"   # Порт подключения приемника RC
RX_UART_BAUDRATE = 115200       # Скорость порта приемника
RPI_UART_PORT = "/dev/ttyAMA0"  # Порт передачи далее на Raspberry Pi (UART0)
RPI_UART_BAUDRATE = 115200      # Скорость порта Raspberry Pi

# --- Пороги 3-позиционного тумблера CH6 ---
# CH6 < 1300      -> 0: MANUAL  (Нижнее положение — Управление пилота)
# 1300 <= CH6 < 1700 -> 1: LOCK    (Среднее положение — ЗАХВАТ объекта, Жёлтый)
# CH6 >= 1700     -> 2: TRACKING (Верхнее положение — СЛЕЖЕНИЕ / УДЕРЖАНИЕ, Красный)
CH6_LOW_MAX = 1300              # Верхняя граница нижнего положения
CH6_HIGH_MIN = 1700             # Нижная граница верхнего положения

