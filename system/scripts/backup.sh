#!/usr/bin/env bash
# ==============================================================================
# Скрипт создания/обновления снимка конфигурации системы (Машенька для Олежки)
# ==============================================================================
set -euo pipefail

SYSTEM_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MANIFESTS_DIR="${SYSTEM_DIR}/manifests"
DOTFILES_DIR="${SYSTEM_DIR}/dotfiles"
SYSCONFIG_DIR="${SYSTEM_DIR}/system-configs"
TIMESTAMP_FILE="${SYSTEM_DIR}/SNAPSHOT_TIMESTAMP"
PERM_MANIFEST="${SYSCONFIG_DIR}/meta/permissions.manifest"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[ИНФО]${NC} $1"; }
log_ok() { echo -e "${GREEN}[УСПЕХ]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[ВНИМАНИЕ]${NC} $1"; }

mkdir -p "${MANIFESTS_DIR}/apt-sources"
mkdir -p "${DOTFILES_DIR}"
mkdir -p "${SYSCONFIG_DIR}/etc"
mkdir -p "${SYSCONFIG_DIR}/meta"

log_info "Начинаем создание снимка конфигурации ОС..."

# 1. Пакетные манифесты и расширения
log_info "1/4. Сбор манифестов пакетов и расширений..."

# APT ручная установка
if command -v apt-mark >/dev/null 2>&1; then
    apt-mark showmanual | sort > "${MANIFESTS_DIR}/apt-manual.list"
    log_ok "APT пакеты сохранены: $(wc -l < "${MANIFESTS_DIR}/apt-manual.list") шт."
fi

# APT репозитории (источники)
if [ -d /etc/apt/sources.list.d ]; then
    rm -rf "${MANIFESTS_DIR}/apt-sources"/*
    cp -r /etc/apt/sources.list.d/* "${MANIFESTS_DIR}/apt-sources/" 2>/dev/null || true
    if [ -f /etc/apt/sources.list ]; then
        cp /etc/apt/sources.list "${MANIFESTS_DIR}/apt-sources/sources.list"
    fi
    log_ok "Источники APT скопированы."
fi

# VS Code расширения
if command -v code >/dev/null 2>&1; then
    code --list-extensions 2>/dev/null | sort > "${MANIFESTS_DIR}/vscode-extensions.list" || true
    log_ok "Расширения VS Code сохранены: $(wc -l < "${MANIFESTS_DIR}/vscode-extensions.list") шт."
fi

# Python pip
if command -v pip3 >/dev/null 2>&1; then
    pip3 list --format=freeze 2>/dev/null | sort > "${MANIFESTS_DIR}/python3-pip.list" || true
    log_ok "Python3 pip пакеты сохранены: $(wc -l < "${MANIFESTS_DIR}/python3-pip.list") шт."
fi

# Flatpak
if command -v flatpak >/dev/null 2>&1; then
    flatpak list --app --columns=application,version,branch,origin > "${MANIFESTS_DIR}/flatpak-packages.list" 2>/dev/null || true
    log_ok "Flatpak пакеты сохранены."
fi

# Snap
if command -v snap >/dev/null 2>&1; then
    snap list > "${MANIFESTS_DIR}/snap-packages.list" 2>/dev/null || true
    log_ok "Snap пакеты сохранены."
fi

# Systemd активные сервисы
if command -v systemctl >/dev/null 2>&1; then
    systemctl list-unit-files --state=enabled --type=service --no-legend 2>/dev/null | awk '{print $1}' | sort > "${MANIFESTS_DIR}/systemd-enabled.list" || true
    log_ok "Список включенных systemd сервисов сохранен."
fi

# Реестр /opt
if [ -d /opt ]; then
    ls -la /opt > "${MANIFESTS_DIR}/opt-inventory.txt"
    log_ok "Инвентаризация /opt сохранена."
fi

# 2. Пользовательские Dotfiles и настройки редакторов
log_info "2/4. Сохранение пользовательских dotfiles и настроек..."

copy_user_file() {
    local src="$1"
    local rel="${src#$HOME/}"
    local dst="${DOTFILES_DIR}/${rel}"
    if [ -e "$src" ]; then
        mkdir -p "$(dirname "$dst")"
        if [ -d "$src" ]; then
            rsync -a --delete "$src/" "$dst/"
        else
            cp -p "$src" "$dst"
        fi
    fi
}

copy_user_file "$HOME/.bashrc"
copy_user_file "$HOME/.profile"
copy_user_file "$HOME/.gitconfig"
copy_user_file "$HOME/.config/openbox"
copy_user_file "$HOME/.config/lxqt"
copy_user_file "$HOME/.config/Code/User/settings.json"
copy_user_file "$HOME/.config/Code/User/keybindings.json"
copy_user_file "$HOME/.config/Code/User/snippets"
copy_user_file "$HOME/.local/bin"

# Вычищаем возможные токены из сохраненного .bashrc
if [ -f "${DOTFILES_DIR}/.bashrc" ]; then
    sed -i -E 's/(OPENAI_API_KEY=")[^"]+(")/\1\2/g' "${DOTFILES_DIR}/.bashrc"
fi

log_ok "Dotfiles (Openbox, LXQt, VS Code config, .bashrc, .local/bin) обновлены."

# 3. Конфигурации /etc и фиксация прав
log_info "3/4. Сохранение системных конфигураций /etc..."

> "${PERM_MANIFEST}"

save_etc_file() {
    local src="$1"
    if [ -f "$src" ]; then
        local rel="${src#/etc/}"
        local dst="${SYSCONFIG_DIR}/etc/${rel}"
        mkdir -p "$(dirname "$dst")"
        cp -p "$src" "$dst"
        
        # Запись прав: path|user|group|mode
        local user group mode
        user=$(stat -c '%U' "$src")
        group=$(stat -c '%G' "$src")
        mode=$(stat -c '%a' "$src")
        echo "${src}|${user}|${group}|${mode}" >> "${PERM_MANIFEST}"
    fi
}

# DHCP
save_etc_file "/etc/dhcp/dhcpd.conf"
save_etc_file "/etc/dhcp/dhcpd6.conf"
save_etc_file "/etc/default/isc-dhcp-server"

# Udev правила
if [ -d /etc/udev/rules.d ]; then
    for f in /etc/udev/rules.d/*; do
        [ -f "$f" ] && save_etc_file "$f"
    done
fi

# Systemd юниты в /etc
if [ -d /etc/systemd/system ]; then
    for f in /etc/systemd/system/*.service; do
        [ -f "$f" ] && save_etc_file "$f"
    done
fi

# Sysctl тюнинг
if [ -d /etc/sysctl.d ]; then
    for f in /etc/sysctl.d/*; do
        [ -f "$f" ] && save_etc_file "$f"
    done
fi

log_ok "Конфигурации /etc и манифест прав обновлены."

# 4. Метка времени
date -Iseconds > "${TIMESTAMP_FILE}"
log_ok "4/4. Снимок успешно зафиксирован: $(cat "${TIMESTAMP_FILE}")"
