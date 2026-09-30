#!/usr/bin/env python3
# ==============================================================================
# Скрипт Запуска Захвата Камеры и Окна Индикации (dront16/run.py)
# ==============================================================================
import cv2
import time
from config import *
from dront16 import CameraTracker, MODE_MANUAL, MODE_LOCK, MODE_TRACKING

def main():
    tracker_app = CameraTracker()
    if not tracker_app.start():
        return

    print("==================================================")
    print("🚀 DronT16 — Слежение за объектом и Управление MSP")
    print("   Тумблер CH6 (3 Положения):")
    print("     [НИЗ]   - Пилот управляет вручную (Зеленый прицел)")
    print("     [СРЕД]  - ЗАХВАТ объекта (Жёлтая рамка)")
    print("     [ВЕРХ]  - УДЕРЖАНИЕ / СЛЕЖЕНИЕ (Красная рамка + MSP)")
    print("   Клавиатура: [0] Manual, [1] Lock, [2] Tracking, [R] Reset, [Q] Exit")
    print("==================================================")

    while True:
        start_t = time.time()
        frame, (dx, dy), locked = tracker_app.process_frame()
        if frame is None:
            break

        cv2.imshow("DronT16 Tracker & MSP (run.py)", frame)

        elapsed = time.time() - start_t
        delay_ms = max(1, int(((1.0 / TARGET_FPS) - elapsed) * 1000))
        key = cv2.waitKey(delay_ms) & 0xFF

        if key == ord('0'):  # 0 - Ручной
            tracker_app.set_mode(MODE_MANUAL, frame)
        elif key == ord('1'):  # 1 - Захват
            tracker_app.set_mode(MODE_LOCK, frame)
        elif key == ord('2'):  # 2 - Удержание
            tracker_app.set_mode(MODE_TRACKING, frame)
        elif key in (ord('r'), ord('R')):
            tracker_app.reset_target()
        elif key in (ord('q'), ord('Q'), 27):
            break

    tracker_app.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    main()
