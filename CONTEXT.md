# 📌 Актуальный контекст проекта Space (для нового чата)

> **Дата фиксации:** 26 сентября 2026  
> **Хост-система:** Ноутбук Lenovo T16 (Ubuntu 24.04 / Lubuntu, LXQt + Openbox, X11)  
> **Пользователь:** Олежка (Владелец проекта)  
> **Инженер-ассистент:** Машенька (GEMINI.md)

---

## 🖥️ 1. Двухмониторное рабочее место (ноут eDP-1 + внешний HDMI DP-1)
* **Телепортация курсора мыши:** `Super + Пробел` (или `Super + o`) -> скрипт `/home/oleg/.local/bin/switch_monitor.py`.
* **Переброс окон между экранами:** `Super + Shift + Right` / `Super + Shift + Left` -> скрипт `/home/oleg/.local/bin/manage_window.py monitor next/prev`.
  * Корректно переносит даже развернутые на весь экран (maximized) окна с сохранением состояния и переносит курсор мыши.
* **Рабочие столы:**
  * `Super + 1..4` / `Super + Left/Right` — переключение рабочих столов.
  * `Super + Shift + 1..4` — перенос активного окна на указанный стол с автоматическим переходом туда.
  * `Super + p` — приколоть окно на всех столах (omnipresent).
* **Конфиги:** `~/.config/openbox/rc.xml`, `~/.config/lxqt/globalkeyshortcuts.conf`.

---

## 📦 2. Система снимков и восстановления настроек (/home/work/Space/system/)
* **Скрипты в `system/scripts/`:**
  * `backup.sh` — создание/обновление полного снимка (APT пакеты, pip, dotfiles, /etc сервисы, udev, systemd).
  * `diff.sh` — проверка расхождений между живой системой и сохраненным снимком.
  * `restore.sh` — пошаговое восстановление на новой системе (с поддержкой `--dry-run`).
  * `apply_sys_configs.sh` — безопасное накатывание конфигураций в `/etc` через `sudo`.
* **Что сохранено в снимке:**
  * Манифесты: 422 пакета APT, 371 пакет Python3 pip, snap, реестр `/opt`.
  * Dotfiles: Openbox, LXQt, `.bashrc`, `.profile`, `.gitconfig`, `~/.local/bin/`.
  * Сервисы `/etc`: `isc-dhcp-server`, `dhcpd.conf`, `udev/rules.d/` (Arduino, Zephyr, OpenMV, VBox), `systemd/system/` (`happd`, `ollama`), `sysctl.d/`.
  * Таблица прав доступа: `system-configs/meta/permissions.manifest`.

---

## 🌐 3. Инфраструктура и сервисы
* **VPS стенд:** `46.8.221.179:4101` (`732477.cloud4box.ru`).
* **Веб-интерфейс Машеньки:** `https://systemio.ru/masha.php` (PIN: `711`, модели Gemini 3.8 / 3.5).
* **Реверс-туннель к ноутбуку:** `http://lora.systemio.ru` -> локальный агент `/home/work/Space/my_agent`.
* **Периферия:** Orange Pi 5, Raspberry Pi, контроллеры и датчики.

---

## 📋 4. Планы на следующие шаги
1. Провести инвентаризацию SSH-хостов (`~/.ssh/config`) и проверить доступ к Orange Pi 5 / Raspberry Pi.
2. Выбрать следующий ключевой этап разработки (сервис, веб-интерфейс, железки или телеметрия).
