#!/usr/bin/env bash
# ==============================================================================
# Скрипт генерации/наката системных конфигураций в /etc (Машенька для Олежки)
# ==============================================================================
set -euo pipefail

SYSTEM_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SYSCONFIG_DIR="${SYSTEM_DIR}/system-configs"
PERM_MANIFEST="${SYSCONFIG_DIR}/meta/permissions.manifest"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

if [ ! -f "${PERM_MANIFEST}" ]; then
    echo -e "${RED}[ОШИБКА]${NC} Манифест прав не найден: ${PERM_MANIFEST}"
    exit 1
fi

# Проверка запуска с sudo
if [ "$EUID" -ne 0 ]; then
    echo -e "${YELLOW}Этот скрипт копирует системные файлы в /etc и требует прав root (sudo).${NC}"
    echo -e "Пожалуйста, запусти: ${GREEN}sudo $0${NC}"
    exit 1
fi

echo -e "${BLUE}=== Применение системных конфигураций в /etc ===${NC}\n"

while IFS='|' read -r filepath user group mode || [ -n "$filepath" ]; do
    [ -z "$filepath" ] && continue
    rel="${filepath#/etc/}"
    src="${SYSCONFIG_DIR}/etc/${rel}"
    
    if [ -f "$src" ]; then
        echo -e "Копирование: ${GREEN}${filepath}${NC} (Владелец: ${user}:${group}, Права: ${mode})"
        mkdir -p "$(dirname "$filepath")"
        cp "$src" "$filepath"
        chown "${user}:${group}" "$filepath"
        chmod "$mode" "$filepath"
    else
        echo -e "${YELLOW}[ПРОПУСК]${NC} Исходный файл не найден в снимке: ${src}"
    fi
done < "${PERM_MANIFEST}"

echo -e "\n${BLUE}Перезагрузка подсистем udev и systemd...${NC}"
if command -v udevadm >/dev/null 2>&1; then
    udevadm control --reload-rules || true
    udevadm trigger || true
    echo -e "${GREEN}[OK]${NC} Правила udev перезагружены."
fi

if command -v systemctl >/dev/null 2>&1; then
    systemctl daemon-reload || true
    echo -e "${GREEN}[OK]${NC} Конфигурация systemd перезагружена."
fi

if command -v sysctl >/dev/null 2>&1; then
    sysctl --system >/dev/null 2>&1 || true
    echo -e "${GREEN}[OK]${NC} Параметры sysctl применены."
fi

echo -e "\n${GREEN}=== Все системные конфигурации успешно применены! ===${NC}"
