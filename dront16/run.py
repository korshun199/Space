#!/usr/bin/env python3
# ==============================================================================
# Скрипт Запуска Захвата Камеры и Окна Индикации (dront16/run.py)
# ==============================================================================
import cv2
import time
from config import *
from dront16 import CameraTracker

def main():
    tracker_app = CameraTracker()
    if not tracker_app.start():
        return

    print("==================================================")
    print("🚀 DronT16 — Слежение за объектом и Управление MSP")
    print("   [Пробел]  - Захватить объект (Режим ЗАХВАТ — Желтый)")
    print("   [1] / CH6 - Включить/выключить СЛЕЖЕНИЕ (Режим СЛЕЖЕНИЕ — Красный)")
    print("   [R]       - Сбросить захват")
    print("   [Q] / Esc - Выход")
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

        if key == 32:  # Пробел
            tracker_app.lock_target(frame)
        elif key == ord('1'):  # Клавиша '1'
            tracker_app.toggle_msp_tracking()
        elif key in (ord('r'), ord('R')):
            tracker_app.reset_target()
        elif key in (ord('q'), ord('Q'), 27):
            break

    tracker_app.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    main()
