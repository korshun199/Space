#!/usr/bin/env python3
"""
Скрипт переключения фокуса и курсора мыши между мониторами
(Экран 1: eDP-1 1920x1200, Экран 2: DP-1 1920x1080)
"""
import ctypes
import subprocess

def get_mouse_and_switch():
    x11 = ctypes.cdll.LoadLibrary('libX11.so.6')
    d = x11.XOpenDisplay(None)
    if not d:
        return
    
    root = x11.XDefaultRootWindow(d)
    root_ret = ctypes.c_ulong()
    child_ret = ctypes.c_ulong()
    root_x = ctypes.c_int()
    root_y = ctypes.c_int()
    win_x = ctypes.c_int()
    win_y = ctypes.c_int()
    mask = ctypes.c_uint()

    x11.XQueryPointer(d, root, ctypes.byref(root_ret), ctypes.byref(child_ret),
                      ctypes.byref(root_x), ctypes.byref(root_y),
                      ctypes.byref(win_x), ctypes.byref(win_y),
                      ctypes.byref(mask))

    cur_x = root_x.value
    cur_y = root_y.value

    # Граница экранов: 1920 по X
    if cur_x < 1920:
        # Перескакиваем на внешний монитор DP-1 (1920x1080)
        target_x = 1920 + 960
        target_y = 540
    else:
        # Перескакиваем на экран ноутбука eDP-1 (1920x1200)
        target_x = 960
        target_y = 600

    # Перемещаем курсор мыши
    x11.XWarpPointer(d, 0, root, 0, 0, 0, 0, target_x, target_y)
    x11.XFlush(d)
    x11.XCloseDisplay(d)

if __name__ == '__main__':
    get_mouse_and_switch()
