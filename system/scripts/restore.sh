#!/usr/bin/env bash
# ==============================================================================
# Скрипт восстановления системы из снимка (Машенька для Олежки)
# ==============================================================================
set -euo pipefail

SYSTEM_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOTFILES_DIR="${SYSTEM_DIR}/dotfiles"
MANIFESTS_DIR="${SYSTEM_DIR}/manifests"
SYSCONFIG_DIR="${SYSTEM_DIR}/system-configs"

DRY_RUN=0
if [ "${1:-}" == "--dry-run" ]; then
    DRY_RUN=1
fi

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_step() { echo -e "\n${BLUE}=== $1 ===${NC}"; }
log_info() { echo -e "${BLUE}[ИНФО]${NC} $1"; }
log_ok() { echo -e "${GREEN}[УСПЕХ]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[ВНИМАНИЕ]${NC} $1"; }

if [ $DRY_RUN -eq 1 ]; then
    log_warn "РЕЖИМ DRY-RUN (симуляция). Никакие файлы и пакеты изменены не будут."
fi

log_step "1/5. Восстановление пользовательских Dotfiles и настроек редакторов"

restore_dotfile() {
    local rel="$1"
    local src="${DOTFILES_DIR}/${rel}"
    local dst="$HOME/${rel}"
    
    if [ -e "$src" ]; then
        if [ $DRY_RUN -eq 1 ]; then
            log_info "[DRY-RUN] Скопировать $src -> $dst"
        else
            mkdir -p "$(dirname "$dst")"
            if [ -d "$src" ]; then
                rsync -a "$src/" "$dst/"
            else
                cp -p "$src" "$dst"
            fi
            log_ok "Восстановлен: $dst"
        fi
    fi
}

restore_dotfile ".bashrc"
restore_dotfile ".profile"
restore_dotfile ".gitconfig"
restore_dotfile ".config/openbox"
restore_dotfile ".config/lxqt"
restore_dotfile ".config/Code/User/settings.json"
restore_dotfile ".config/Code/User/keybindings.json"
restore_dotfile ".config/Code/User/snippets"
restore_dotfile ".local/bin"

log_step "2/5. Восстановление расширений VS Code"

if [ -f "${MANIFESTS_DIR}/vscode-extensions.list" ] && command -v code >/dev/null 2>&1; then
    if [ $DRY_RUN -eq 1 ]; then
        log_info "[DRY-RUN] Установить расширения VS Code ($(wc -l < "${MANIFESTS_DIR}/vscode-extensions.list") шт.)"
    else
        log_info "Установка расширений VS Code..."
        while read -r ext || [ -n "$ext" ]; do
            [ -z "$ext" ] && continue
            code --install-extension "$ext" --force >/dev/null 2>&1 || log_warn "Не удалось поставить расширение: $ext"
        done < "${MANIFESTS_DIR}/vscode-extensions.list"
        log_ok "Расширения VS Code установлены."
    fi
fi

log_step "3/5. Восстановление пакетов Python (pip)"

if [ -f "${MANIFESTS_DIR}/python3-pip.list" ] && command -v pip3 >/dev/null 2>&1; then
    if [ $DRY_RUN -eq 1 ]; then
        log_info "[DRY-RUN] Установить pip-пакеты из ${MANIFESTS_DIR}/python3-pip.list"
    else
        log_info "Установка pip-пакетов (с флагом --break-system-packages при необходимости)..."
        pip3 install -r "${MANIFESTS_DIR}/python3-pip.list" --break-system-packages 2>/dev/null || \
        pip3 install -r "${MANIFESTS_DIR}/python3-pip.list" || log_warn "Некоторые pip пакеты потребовали сборки или были пропущены."
        log_ok "Пакеты pip обработаны."
    fi
fi

log_step "4/5. Инструкция по установке системных APT пакетов"

if [ -f "${MANIFESTS_DIR}/apt-manual.list" ]; then
    log_info "Для восстановления APT пакетов Олежке нужно выполнить команду:"
    echo -e "${YELLOW}sudo apt-get update && sudo apt-get install -y \$(cat ${MANIFESTS_DIR}/apt-manual.list | tr '\\n' ' ')${NC}"
fi

log_step "5/5. Применение конфигураций /etc"

log_info "Для наката udev-правил, systemd сервисов и настроек сети запусти скрипт через sudo:"
echo -e "${YELLOW}sudo ${SYSTEM_DIR}/scripts/apply_sys_configs.sh${NC}"

if [ $DRY_RUN -eq 0 ]; then
    log_step "Восстановление завершено!"
    echo -e "${GREEN}Пользовательские настройки и расширения VS Code успешно восстановлены.${NC}"
else
    log_step "Проверка (DRY-RUN) завершена!"
fi
