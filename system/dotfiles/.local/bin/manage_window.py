#!/usr/bin/env python3
"""
Утилита управления окнами под X11/Openbox:
- Перемещение активного окна между мониторами (с поддержкой максимизированных окон!)
- Перемещение активного окна на рабочий стол (с переходом или без)
"""

import sys
import time
import ctypes
import subprocess

# Структура ClientMessage для EWMH
class XClientMessageData(ctypes.Union):
    _fields_ = [
        ('b', ctypes.c_char * 20),
        ('s', ctypes.c_short * 10),
        ('l', ctypes.c_long * 5)
    ]

class XClientMessageEvent(ctypes.Structure):
    _fields_ = [
        ('type', ctypes.c_int),
        ('serial', ctypes.c_ulong),
        ('send_event', ctypes.c_int),
        ('display', ctypes.c_void_p),
        ('window', ctypes.c_ulong),
        ('message_type', ctypes.c_ulong),
        ('format', ctypes.c_int),
        ('data', XClientMessageData)
    ]

class XEvent(ctypes.Union):
    _fields_ = [
        ('type', ctypes.c_int),
        ('xclient', XClientMessageEvent),
        ('pad', ctypes.c_long * 24)
    ]

x11 = ctypes.cdll.LoadLibrary('libX11.so.6')

def send_client_message(d, root, win, msg_type, data):
    event = XEvent()
    event.type = 33 # ClientMessage
    event.xclient.type = 33
    event.xclient.serial = 0
    event.xclient.send_event = 1
    event.xclient.display = d
    event.xclient.window = win
    event.xclient.message_type = msg_type
    event.xclient.format = 32
    for i in range(min(len(data), 5)):
        event.xclient.data.l[i] = data[i]
    
    mask = (1 << 20) | (1 << 19) # SubstructureRedirectMask | SubstructureNotifyMask
    x11.XSendEvent(d, root, False, mask, ctypes.byref(event))
    x11.XFlush(d)

def get_active_window(d, root):
    atom_active = x11.XInternAtom(d, b'_NET_ACTIVE_WINDOW', False)
    actual_type = ctypes.c_ulong()
    actual_format = ctypes.c_int()
    nitems = ctypes.c_ulong()
    bytes_after = ctypes.c_ulong()
    prop = ctypes.c_void_p()

    status = x11.XGetWindowProperty(d, root, atom_active, 0, 1, False, 33,
                                    ctypes.byref(actual_type), ctypes.byref(actual_format),
                                    ctypes.byref(nitems), ctypes.byref(bytes_after),
                                    ctypes.byref(prop))
    if prop.value and nitems.value > 0:
        win_id = ctypes.cast(prop, ctypes.POINTER(ctypes.c_ulong)).contents.value
        x11.XFree(prop)
        return win_id
    return None

def is_window_maximized(d, win):
    atom_state = x11.XInternAtom(d, b'_NET_WM_STATE', False)
    actual_type = ctypes.c_ulong()
    actual_format = ctypes.c_int()
    nitems = ctypes.c_ulong()
    bytes_after = ctypes.c_ulong()
    prop = ctypes.c_void_p()

    x11.XGetWindowProperty(d, win, atom_state, 0, 10, False, 4,
                           ctypes.byref(actual_type), ctypes.byref(actual_format),
                           ctypes.byref(nitems), ctypes.byref(bytes_after),
                           ctypes.byref(prop))
    
    max_h = False
    max_v = False
    if prop.value and nitems.value > 0:
        atom_max_h = x11.XInternAtom(d, b'_NET_WM_STATE_MAXIMIZED_HORZ', False)
        atom_max_v = x11.XInternAtom(d, b'_NET_WM_STATE_MAXIMIZED_VERT', False)
        atoms = ctypes.cast(prop, ctypes.POINTER(ctypes.c_ulong))
        for i in range(nitems.value):
            if atoms[i] == atom_max_h:
                max_h = True
            elif atoms[i] == atom_max_v:
                max_v = True
        x11.XFree(prop)
    return max_h and max_v

def set_window_maximized(d, root, win, state_action):
    # state_action: 0=remove, 1=add, 2=toggle
    atom_state = x11.XInternAtom(d, b'_NET_WM_STATE', False)
    atom_max_h = x11.XInternAtom(d, b'_NET_WM_STATE_MAXIMIZED_HORZ', False)
    atom_max_v = x11.XInternAtom(d, b'_NET_WM_STATE_MAXIMIZED_VERT', False)
    send_client_message(d, root, win, atom_state, [state_action, atom_max_h, atom_max_v, 1, 0])

def move_window_to_monitor(direction="next"):
    d = x11.XOpenDisplay(None)
    if not d:
        return
    root = x11.XDefaultRootWindow(d)
    win = get_active_window(d, root)
    if not win:
        x11.XCloseDisplay(d)
        return

    # Получаем координаты окна через XTranslateCoordinates к Root
    root_ret = ctypes.c_ulong()
    child_ret = ctypes.c_ulong()
    win_x = ctypes.c_int()
    win_y = ctypes.c_int()
    x11.XTranslateCoordinates(d, win, root, 0, 0, ctypes.byref(win_x), ctypes.byref(win_y), ctypes.byref(child_ret))

    cur_x = win_x.value
    cur_y = win_y.value
    maximized = is_window_maximized(d, win)

    # Мониторы: eDP-1: 0..1920 (1920x1200), DP-1: 1920..3840 (1920x1080)
    if cur_x < 1920:
        # Перемещаем на Монитор 2 (DP-1)
        target_x = 1920 + 100
        target_y = 100
        warp_x = 1920 + 960
        warp_y = 540
    else:
        # Перемещаем на Монитор 1 (eDP-1)
        target_x = 100
        target_y = 100
        warp_x = 960
        warp_y = 600

    if maximized:
        set_window_maximized(d, root, win, 0) # unmaximize
        time.sleep(0.05)

    # Перемещаем окно
    x11.XMoveWindow(d, win, target_x, target_y)
    x11.XFlush(d)

    if maximized:
        time.sleep(0.05)
        set_window_maximized(d, root, win, 1) # re-maximize on new monitor

    # Перемещаем мышку в центр окна
    x11.XWarpPointer(d, 0, root, 0, 0, 0, 0, warp_x, warp_y)
    x11.XFlush(d)
    x11.XCloseDisplay(d)

def send_window_to_desktop(desktop_num, follow=True):
    d = x11.XOpenDisplay(None)
    if not d:
        return
    root = x11.XDefaultRootWindow(d)
    win = get_active_window(d, root)
    if not win:
        x11.XCloseDisplay(d)
        return

    # desktop_num 1-based -> 0-based in EWMH
    desk_idx = int(desktop_num) - 1
    atom_wm_desk = x11.XInternAtom(d, b'_NET_WM_DESKTOP', False)
    send_client_message(d, root, win, atom_wm_desk, [desk_idx, 1, 0, 0, 0])

    if follow:
        atom_cur_desk = x11.XInternAtom(d, b'_NET_CURRENT_DESKTOP', False)
        send_client_message(d, root, root, atom_cur_desk, [desk_idx, 0, 0, 0, 0])

    x11.XFlush(d)
    x11.XCloseDisplay(d)

if __name__ == '__main__':
    if len(sys.argv) < 2:
        sys.exit(0)
    
    cmd = sys.argv[1]
    if cmd == "monitor":
        direction = sys.argv[2] if len(sys.argv) > 2 else "next"
        move_window_to_monitor(direction)
    elif cmd == "desktop":
        num = sys.argv[2] if len(sys.argv) > 2 else 1
        follow = "--follow" in sys.argv
        send_window_to_desktop(num, follow)
