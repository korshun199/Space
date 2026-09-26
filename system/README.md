# 📦 Система снимков и восстановления окружения (System Backup & Restore)

> Инженерный модуль для фиксации состояния операционной системы ноутбука Lenovo T16 (Lubuntu 24.04, LXQt + Openbox, X11, VS Code) и его воспроизведения на чистой системе.

---

## 📂 Структура каталога `system/`

```text
system/
├── manifests/              # Списки и манифесты установленного ПО
│   ├── apt-manual.list     # Пакеты APT, установленные вручную
│   ├── apt-sources/        # Репозитории и PPA (/etc/apt/sources.list.d)
│   ├── vscode-extensions.list # Список установленных расширений VS Code
│   ├── python3-pip.list    # Список Python pip пакетов
│   ├── flatpak-packages.list
│   ├── snap-packages.list
│   ├── systemd-enabled.list # Список активных сервисов systemd
│   └── opt-inventory.txt   # Листинг каталога /opt
├── dotfiles/               # Пользовательские конфиги и скрипты (~/)
│   ├── .bashrc, .profile, .gitconfig
│   ├── .config/ (openbox, lxqt)
│   ├── .config/Code/User/ (settings.json, keybindings.json, snippets)
│   └── .local/bin/ (manage_window.py, switch_monitor.py и др.)
├── system-configs/         # Системные файлы (/etc)
│   ├── etc/ (dhcp, udev/rules.d, systemd/system, sysctl.d)
│   └── meta/
│       └── permissions.manifest # Таблица прав доступа и владельцев
├── scripts/                # Управляющие скрипты
│   ├── backup.sh           # Создание/обновление снимка
│   ├── diff.sh             # Сравнение снимка с живой системой
│   ├── restore.sh          # Восстановление пользователя, VS Code + инструкции
│   └── apply_sys_configs.sh# Накат конфигураций /etc через sudo
└── SNAPSHOT_TIMESTAMP      # Метка времени последнего снимка
```

---

## 🚀 Как пользоваться

### 1. Создать или обновить снимок системы
Собирает актуальные списки пакетов, расширений VS Code, копирует dotfiles и важные конфиги из `/etc`:
```bash
/home/work/Space/system/scripts/backup.sh
```

### 2. Проверить расхождения (Diff)
Показывает, что изменилось в системе относительно сохраненного снимка (dotfiles, `/etc`, VS Code расширения, APT):
```bash
/home/work/Space/system/scripts/diff.sh
```

### 3. Восстановление на новой машине

#### Шаг А: Проверка (Dry-run)
```bash
/home/work/Space/system/scripts/restore.sh --dry-run
```

#### Шаг Б: Восстановление dotfiles, VS Code настроек/плагинов и pip (без sudo)
```bash
/home/work/Space/system/scripts/restore.sh
```

#### Шаг В: Установка системных APT-пакетов (выполняет Олежка через sudo)
```bash
sudo apt-get update && sudo apt-get install -y $(cat /home/work/Space/system/manifests/apt-manual.list | tr '\n' ' ')
```

#### Шаг Г: Накат системных конфигураций /etc (выполняет Олежка через sudo)
```bash
sudo /home/work/Space/system/scripts/apply_sys_configs.sh
```
