#!/usr/bin/env bash
# ==============================================================================
# Скрипт сравнения живой системы с сохраненным снимком (Машенька для Олежки)
# ==============================================================================
set -euo pipefail

SYSTEM_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOTFILES_DIR="${SYSTEM_DIR}/dotfiles"
SYSCONFIG_DIR="${SYSTEM_DIR}/system-configs"
MANIFESTS_DIR="${SYSTEM_DIR}/manifests"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}=== Анализ расхождений (Diff) живой системы и снимка ===${NC}\n"

# 1. Сравнение Dotfiles
echo -e "${YELLOW}--- [1/4] Пользовательские Dotfiles (~/) ---${NC}"
has_dotfiles_diff=0

check_file_diff() {
    local live="$1"
    local snap="$2"
    local name="$3"
    
    if [ ! -e "$snap" ]; then
        return
    fi
    
    if [ ! -e "$live" ]; then
        echo -e "${RED}[УДАЛЕН В СИСТЕМЕ]${NC} $name"
        has_dotfiles_diff=1
        return
    fi
    
    if diff -u "$snap" "$live" >/tmp/dotfile_diff.tmp 2>&1; then
        :
    else
        echo -e "${YELLOW}[ИЗМЕНЕН]${NC} $name"
        diff -u --color=always "$snap" "$live" | head -n 15 || true
        echo ""
        has_dotfiles_diff=1
    fi
}

check_dir_diff() {
    local live_dir="$1"
    local snap_dir="$2"
    local name="$3"
    
    if [ -d "$snap_dir" ]; then
        local d_diff
        d_diff=$(diff -rq "$snap_dir" "$live_dir" 2>/dev/null || true)
        if [ -n "$d_diff" ]; then
            echo -e "${YELLOW}[РАСХОЖДЕНИЯ В КАТАЛОГЕ]${NC} $name:"
            echo "$d_diff" | sed 's/^/  /'
            echo ""
            has_dotfiles_diff=1
        fi
    fi
}

check_file_diff "$HOME/.bashrc" "${DOTFILES_DIR}/.bashrc" "~/.bashrc"
check_file_diff "$HOME/.profile" "${DOTFILES_DIR}/.profile" "~/.profile"
check_file_diff "$HOME/.gitconfig" "${DOTFILES_DIR}/.gitconfig" "~/.gitconfig"
check_dir_diff "$HOME/.config/openbox" "${DOTFILES_DIR}/.config/openbox" "~/.config/openbox"
check_dir_diff "$HOME/.config/lxqt" "${DOTFILES_DIR}/.config/lxqt" "~/.config/lxqt"
check_file_diff "$HOME/.config/Code/User/settings.json" "${DOTFILES_DIR}/.config/Code/User/settings.json" "~/.config/Code/User/settings.json"
check_file_diff "$HOME/.config/Code/User/keybindings.json" "${DOTFILES_DIR}/.config/Code/User/keybindings.json" "~/.config/Code/User/keybindings.json"
check_dir_diff "$HOME/.config/Code/User/snippets" "${DOTFILES_DIR}/.config/Code/User/snippets" "~/.config/Code/User/snippets"
check_dir_diff "$HOME/.local/bin" "${DOTFILES_DIR}/.local/bin" "~/.local/bin"

if [ $has_dotfiles_diff -eq 0 ]; then
    echo -e "${GREEN}Dotfiles полностью синхронизированы со снимком.${NC}\n"
fi

# 2. Сравнение системных конфигураций /etc
echo -e "${YELLOW}--- [2/4] Системные конфигурации (/etc) ---${NC}"
has_etc_diff=0

if [ -f "${SYSCONFIG_DIR}/meta/permissions.manifest" ]; then
    while IFS='|' read -r filepath user group mode || [ -n "$filepath" ]; do
        [ -z "$filepath" ] && continue
        rel="${filepath#/etc/}"
        snap_file="${SYSCONFIG_DIR}/etc/${rel}"
        
        if [ -f "$snap_file" ]; then
            if [ ! -f "$filepath" ]; then
                echo -e "${RED}[ОТСУТСТВУЕТ В /etc]${NC} $filepath"
                has_etc_diff=1
            elif ! cmp -s "$snap_file" "$filepath"; then
                echo -e "${YELLOW}[ИЗМЕНЕН /etc]${NC} $filepath"
                diff -u --color=always "$snap_file" "$filepath" | head -n 15 || true
                echo ""
                has_etc_diff=1
            fi
        fi
    done < "${SYSCONFIG_DIR}/meta/permissions.manifest"
fi

if [ $has_etc_diff -eq 0 ]; then
    echo -e "${GREEN}Конфигурации /etc полностью совпадают со снимком.${NC}\n"
fi

# 3. Сравнение расширений VS Code
echo -e "${YELLOW}--- [3/4] Расширения VS Code ---${NC}"
if [ -f "${MANIFESTS_DIR}/vscode-extensions.list" ] && command -v code >/dev/null 2>&1; then
    current_ext=$(mktemp)
    code --list-extensions 2>/dev/null | sort > "$current_ext" || true
    
    new_ext=$(comm -13 "${MANIFESTS_DIR}/vscode-extensions.list" "$current_ext")
    missing_ext=$(comm -23 "${MANIFESTS_DIR}/vscode-extensions.list" "$current_ext")
    
    if [ -n "$new_ext" ]; then
        echo -e "${BLUE}Новые установленные расширения VS Code:${NC}"
        echo "$new_ext" | sed 's/^/  + /'
        echo ""
    fi
    if [ -n "$missing_ext" ]; then
        echo -e "${RED}Удаленные расширения VS Code:${NC}"
        echo "$missing_ext" | sed 's/^/  - /'
        echo ""
    fi
    if [ -z "$new_ext" ] && [ -z "$missing_ext" ]; then
        echo -e "${GREEN}Список расширений VS Code полностью соответствует снимку.${NC}\n"
    fi
    rm -f "$current_ext"
fi

# 4. Сравнение APT пакетов
echo -e "${YELLOW}--- [4/4] Новые / удаленные пакеты APT ---${NC}"
if [ -f "${MANIFESTS_DIR}/apt-manual.list" ] && command -v apt-mark >/dev/null 2>&1; then
    current_apt=$(mktemp)
    apt-mark showmanual | sort > "$current_apt"
    
    new_pkgs=$(comm -13 "${MANIFESTS_DIR}/apt-manual.list" "$current_apt")
    missing_pkgs=$(comm -23 "${MANIFESTS_DIR}/apt-manual.list" "$current_apt")
    
    if [ -n "$new_pkgs" ]; then
        echo -e "${BLUE}Новые установленные пакеты (нет в снимке):${NC}"
        echo "$new_pkgs" | sed 's/^/  + /'
        echo ""
    fi
    if [ -n "$missing_pkgs" ]; then
        echo -e "${RED}Удаленные пакеты (были в снимке):${NC}"
        echo "$missing_pkgs" | sed 's/^/  - /'
        echo ""
    fi
    if [ -z "$new_pkgs" ] && [ -z "$missing_pkgs" ]; then
        echo -e "${GREEN}Список APT пакетов полностью соответствует снимку.${NC}"
    fi
    rm -f "$current_apt"
fi

echo -e "\n${BLUE}=== Анализ завершен ===${NC}"
