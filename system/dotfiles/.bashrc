shopt -s checkwinsize cdspell direxpand dotglob autocd histappend
#PROMPT_COMMAND='history -a' 

[ -x /usr/bin/lesspipe ] && eval "$(SHELL=/bin/sh lesspipe)"

if [ -z "${debian_chroot:-}" ] && [ -r /etc/debian_chroot ]; then
    debian_chroot=$(cat /etc/debian_chroot)
fi
#TERM=`xterm`sudo pip install percol

# set a fancy prompt (non-color, unless we know we "want" color)
case "$TERM" in
    xterm-color) color_prompt=yes;;
esac

if ! shopt -oq posix; then 
  if [ -f /usr/share/bash-completion/bash_completion ]; then
    . /usr/share/bash-completion/bash_completion
  elif [ -f /etc/bash_completion ]; then
    . /etc/bash_completion
  fi
fi

export TERM=xterm

set-title(){
  ORIG=$PS1
  TITLE="\e]2;$@\a"
  PS1=${ORIG}${TITLE}
}




HISTCONTROL=ignoreboth
#bind -x '"\C-R": READLINE_LINE=$(history | tac | cut -c 8- | percol --query "${READLINE_LINE}" </dev/tty >/dev/tty 2>&1) READLINE_POINT=${#READLINE_LINE}'
bind -x '"\C-R": READLINE_LINE=$(history | tac | cut -c 8- | percol --query "${READLINE_LINE}") READLINE_POINT=' 2>/dev/null
shopt -s histappend
HISTSIZE=1000
HISTFILESIZE=2000


export HISTSIZE=10000
export HISTFILESIZE=10000
export HISTCONTROL=ignoreboth:erasedups
PROMPT_COMMAND='history -a'
export HISTIGNORE='ls:ps:history*'

export EDITOR='/opt/sublime_text/sublime_text'

export PATH="/home/work/script:/home/oleg/.local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/local/sbin:/usr/local/bin:/snap/bin"

COL=`shuf -i1-7 -n1`

PS1="🌍 \e[1;32mLOCAL_T16 \e[1;35m\w/\e[0m \e[0m \n\e[1;34m\e[0m $ "

export STM32_PRG_PATH=/home/oleg/STMicroelectronics/STM32Cube/STM32CubeProgrammer/bin
export PYTHONPATH="${PYTHONPATH}:/home/work/script"
export TERM=xterm-color
export NODE_PATH=/usr/share/npm/node_modules



# ============================================================
# ============================================================

# ============================================================
# ESP32 Radar terminal helpers
# ============================================================
# Цветной пульт для финала первого фильма проекта Радар.
# Текущая чистая база:
#   firmware/radar/radar.ino
#   scripts/run.sh
#   scripts/monitor.sh
#   docs/README.txt

export RADAR_PROJECT_DIR="/home/work/ESP32-ASProject"
export RADAR_DEFAULT_PORT="/dev/ttyACM0"

if [ -t 1 ]; then
  _r_c_reset='\033[0m'
  _r_c_red='\033[1;31m'
  _r_c_green='\033[1;32m'
  _r_c_yellow='\033[1;33m'
  _r_c_blue='\033[1;34m'
  _r_c_magenta='\033[1;35m'
  _r_c_cyan='\033[1;36m'
  _r_c_gray='\033[0;90m'
  _r_c_white='\033[1;37m'
else
  _r_c_reset=''
  _r_c_red=''
  _r_c_green=''
  _r_c_yellow=''
  _r_c_blue=''
  _r_c_magenta=''
  _r_c_cyan=''
  _r_c_gray=''
  _r_c_white=''
fi

unalias radar 2>/dev/null || true

rsep() {
  printf "%b\n" "${_r_c_gray}────────────────────────────────────────────────────────────${_r_c_reset}"
}

rhead() {
  rsep
  printf "%b\n" "${_r_c_cyan}📡 $*${_r_c_reset}"
  rsep
}

rok() {
  printf "%b\n" "${_r_c_green}✅ $*${_r_c_reset}"
}

rwarn() {
  printf "%b\n" "${_r_c_yellow}⚠️  $*${_r_c_reset}"
}

rerr() {
  printf "%b\n" "${_r_c_red}❌ $*${_r_c_reset}"
}

rinfo() {
  printf "%b\n" "${_r_c_blue}ℹ️  $*${_r_c_reset}"
}

rnote() {
  printf "%b\n" "${_r_c_magenta}🛠️  $*${_r_c_reset}"
}

rcd() {
  cd "$RADAR_PROJECT_DIR" || {
    rerr "Не найден проект: $RADAR_PROJECT_DIR"
    return 1
  }
}

radar() {
  rcd || return 1

  rhead "ESP32 Radar project"

  printf "%b %s\n" "${_r_c_white}Path:${_r_c_reset}" "$(pwd)"
  printf "%b %s\n" "${_r_c_white}Branch:${_r_c_reset}" "$(git branch --show-current 2>/dev/null || echo 'not a git repo')"

  echo
  printf "%b\n" "${_r_c_white}Рабочие файлы:${_r_c_reset}"
  find firmware scripts docs -type f 2>/dev/null | sort

  echo
  printf "%b\n" "${_r_c_white}Git status:${_r_c_reset}"
  local st
  st="$(git status --short 2>/dev/null)"
  if [ -z "$st" ]; then
    rok "working tree clean"
  else
    printf "%s\n" "$st"
  fi
}

rstatus() {
  rcd || return 1

  rhead "ESP32 Radar status"

  printf "%b %s\n" "${_r_c_white}Path:${_r_c_reset}" "$(pwd)"
  printf "%b %s\n" "${_r_c_white}Branch:${_r_c_reset}" "$(git branch --show-current 2>/dev/null || echo 'not a git repo')"

  echo
  printf "%b\n" "${_r_c_white}Git status:${_r_c_reset}"
  local st
  st="$(git status --short 2>/dev/null)"
  if [ -z "$st" ]; then
    rok "working tree clean"
  else
    printf "%s\n" "$st"
  fi

  echo
  printf "%b\n" "${_r_c_white}Last commits:${_r_c_reset}"
  git log --oneline -5 2>/dev/null || true
}

rfiles() {
  rcd || return 1

  rhead "ESP32 Radar files"

  find firmware scripts docs -type f 2>/dev/null | sort

  echo
  rok "Ожидаемая чистая база: firmware/radar/radar.ino, scripts/run.sh, scripts/monitor.sh, docs/README.txt"
}

rports() {
  rhead "USB / Serial ports"

  printf "%b\n" "${_r_c_white}Serial devices:${_r_c_reset}"

  local found=0
  local dev

  for dev in /dev/ttyUSB* /dev/ttyACM*; do
    if [ -e "$dev" ]; then
      ls -la "$dev"
      found=1
    fi
  done

  if [ "$found" -eq 0 ]; then
    rwarn "Нет /dev/ttyUSB* или /dev/ttyACM*"
  else
    rok "Serial port найден"
  fi

  echo
  printf "%b\n" "${_r_c_white}Arduino board list:${_r_c_reset}"
  if command -v arduino-cli >/dev/null 2>&1; then
    arduino-cli board list || true
  else
    rwarn "arduino-cli не найден"
  fi
}

rrun() {
  rcd || return 1

  rhead "Compile and upload Radar firmware"

  if [ ! -x scripts/run.sh ]; then
    rerr "Не найден или не исполняемый файл: scripts/run.sh"
    return 1
  fi

  ./scripts/run.sh
  local code=$?

  if [ "$code" -eq 0 ]; then
    rok "Прошивка завершена успешно"
  else
    rerr "Прошивка завершилась с ошибкой: $code"
  fi

  return "$code"
}

rmon() {
  rcd || return 1

  rhead "Radar serial monitor"

  if [ ! -x scripts/monitor.sh ]; then
    rerr "Не найден или не исполняемый файл: scripts/monitor.sh"
    return 1
  fi

  ./scripts/monitor.sh
}
rreadme() {
  rcd || return 1

  rhead "Radar README"

  if [ -f docs/README.txt ]; then
    cat docs/README.txt
  else
    rerr "Не найден docs/README.txt"
    return 1
  fi
}

rcode() {
  rcd || return 1

  rhead "Radar firmware"

  if [ -f firmware/radar/radar.ino ]; then
    less firmware/radar/radar.ino
  else
    rerr "Не найден firmware/radar/radar.ino"
    return 1
  fi
}

rpush() {
  rcd || return 1

  rhead "Git push Radar"

  git status --short

  echo
  git add -A

  if git diff --cached --quiet; then
    rwarn "Коммит не нужен: изменений нет"
  else
    git commit -m "${1:-Update radar project}"
  fi

  echo
  git push
}

rfilm1() {
  rcd || return 1

  rhead "Проект Радар. Фильм первый"

  rok "ESP32-S3 работает"
  rok "TFT ST7789 работает"
  rok "ICS-43434 I2S микрофон работает"
  rok "Код описан"
  rok "Проект очищен"
  rok "GitHub получил финальную версию"

  echo
  printf "%b\n" "${_r_c_white}Файлы:${_r_c_reset}"
  find firmware scripts docs -type f 2>/dev/null | sort

  echo
  printf "%b\n" "${_r_c_white}Последние коммиты:${_r_c_reset}"
  git log --oneline -5 2>/dev/null || true
}

rhelp() {
  rhead "ESP32 Radar helper commands"

  cat <<'HELP'
radar        - перейти в проект и показать краткий цветной статус
rstatus      - полный Git-статус и последние коммиты
rfiles       - показать рабочие файлы проекта
rports       - показать USB/Serial порты и arduino-cli board list
rrun         - собрать и прошить firmware/radar/radar.ino
rmon         - открыть монитор порта через scripts/monitor.sh
rd           - прошить и сразу открыть монитор
rreadme      - показать docs/README.txt
rcode        - открыть firmware/radar/radar.ino через less
rpush        - git add, commit, push
rfilm1       - красивый итог первого фильма
rhelp        - эта подсказка
HELP
}

# ============================================================
# End ESP32 Radar terminal helpers
# ============================================================

# >>> OLEG COLOR PRINT HELPERS >>>
# Цветной вывод для Bash-скриптов и ручных команд.
# Использование:
#   VALUE=500
#   print_red "Значение: ${VALUE} Mb"
#   print_green "Готово"
#   print_title "ESP32 Radar"

function _print_color {
    local color="$1"
    shift
    printf "%b%s%b\n" "$color" "$*" "$COLOR_RESET"
}

COLOR_RESET="\033[0m"
COLOR_BOLD="\033[1m"
COLOR_DIM="\033[2m"

COLOR_BLACK="\033[30m"
COLOR_RED="\033[31m"
COLOR_GREEN="\033[32m"
COLOR_YELLOW="\033[33m"
COLOR_BLUE="\033[34m"
COLOR_MAGENTA="\033[35m"
COLOR_CYAN="\033[36m"
COLOR_WHITE="\033[37m"

COLOR_BRIGHT_RED="\033[91m"
COLOR_BRIGHT_GREEN="\033[92m"
COLOR_BRIGHT_YELLOW="\033[93m"
COLOR_BRIGHT_BLUE="\033[94m"
COLOR_BRIGHT_MAGENTA="\033[95m"
COLOR_BRIGHT_CYAN="\033[96m"

function print_black { _print_color "$COLOR_BLACK" "$@"; }
function print_red { _print_color "$COLOR_RED" "$@"; }
function print_green { _print_color "$COLOR_GREEN" "$@"; }
function print_yellow { _print_color "$COLOR_YELLOW" "$@"; }
function print_blue { _print_color "$COLOR_BLUE" "$@"; }
function print_magenta { _print_color "$COLOR_MAGENTA" "$@"; }
function print_cyan { _print_color "$COLOR_CYAN" "$@"; }
function print_white { _print_color "$COLOR_WHITE" "$@"; }

function print_bright_red { _print_color "$COLOR_BRIGHT_RED" "$@"; }
function print_bright_green { _print_color "$COLOR_BRIGHT_GREEN" "$@"; }
function print_bright_yellow { _print_color "$COLOR_BRIGHT_YELLOW" "$@"; }
function print_bright_blue { _print_color "$COLOR_BRIGHT_BLUE" "$@"; }
function print_bright_magenta { _print_color "$COLOR_BRIGHT_MAGENTA" "$@"; }
function print_bright_cyan { _print_color "$COLOR_BRIGHT_CYAN" "$@"; }

function print_bold { _print_color "$COLOR_BOLD" "$@"; }
function print_dim { _print_color "$COLOR_DIM" "$@"; }

function print_ok { print_green "✅ $*"; }
function print_warn { print_yellow "⚠️  $*"; }
function print_error { print_red "❌ $*"; }
function print_info { print_cyan "ℹ️  $*"; }

function print_title {
    echo
    print_magenta "────────────────────────────────────────"
    print_bold "$*"
    print_magenta "────────────────────────────────────────"
}

function print_line {
    print_dim "────────────────────────────────────────"
}
# <<< OLEG COLOR PRINT HELPERS <<<


#export OPENAI_API_KEY=""

alias 4='amixer -D pulse set Master 20%- unmute'
alias 7='amixer -D pulse set Master 20%+ unmute'
alias p='ps -auxf'
alias n='ncdu'
alias b='btop'
alias eq='subl ~/.bashrc'
alias mk='cd /home/work/ESP32'
alias xs='cd /home/work/'
alias za="cd /home/oleg/Download"
alias sq='sudo systemctl'
alias al='sudo systemctl restart alisa.service && sudo systemctl restart nginx'
alias aq='source /home/work/env/bin/activate'
alias ps='ps -e -o pid,user,%mem,command --sort %mem'
alias sw='python $(pwd)/manage.py runserver'

alias ll='eza -l -s new'
alias lb='ls -lSr | rcat'
alias lp="/usr/bin/eza -lg --group-directories-first "

alias dur='find . -maxdepth 1 -type d -exec du -s {} \\;'
alias err='tail -f /var/log/nginx/error.log'
alias man='man -L ru'
alias smb='sudo apt-get install cifs-utils'
alias als='cat ~/.bashrc | grep alias | rcat'
alias bat='upower -i /org/freedesktop/UPower/devices/battery_BAT0 | grep  perc;upower -i /org/freedesktop/UPower/devices/battery_BAT1 | grep  perc'

alias tohex='f(){ printf "0x%X\\n" "$1"; }; f'
alias todec='f(){ echo $(("$1")); }; f'

alias aptf='sudo apt-get -f install'
alias apti='sudo apt install'
alias apts='sudo aptitude search'
alias aptu='sudo apt update'
alias ctar='tar -cvf'
alias runs="source ~/.bashrc"
alias utar='tar -xvf'
alias subl='/opt/sublime_text/sublime_text'
alias wget='wget --report-speed=bits'
alias myip='curl zx2c4.com/ip'
alias rpip="pip install  --break-system-packages"

alias htreq="sudo tcpdump -s 0 -A -vv 'tcp[((tcp[12:1] & 0xf0) >> 2):4] = 0x47455420'  -i wlan0"
alias netstat='sudo  netstat -tplna'
alias speed_net='speedometer -s -rx wlp3s0 -tx wlp3s0'
alias cli_loc='sudo mycli -u root -p00 mysql'
alias redis_all="echo 'keys *' | redis-cli | sed 's/^/get /' | redis-cli "
alias local_base='mycli -u oleg  -p0000 mysql'
alias bpy='bpython3 -i /home/oleg/bpython.cf' 
alias newscript="subl /home/work/script/tmp.py"
#alias run_vpn="sudo systemctl start wg-quick@wg1; myip"
#alias stop_vpn="sudo systemctl stop wg-quick@wg1; myip"
alias newfile='find . -type f -mtime -5 -exec exa -l {} \\;' # измененные за последние 5 суток
alias wifishow='nmcli connection show --active'
#alias chrome_proxy="google-chrome-stable --proxy-server='192.168.0.100:8081'"
#alias trans='trans  :ru -b'
alias sshf_vps='sshfs -p 4101 oleg@46.8.221.179:/home/work/ /home/work/sshf_vps; cd /home/work/ssh_vps'
alias sshf_rsb='sshfs  oleg@192.168.20.125:/home/oleg/ /home/work/raspberry'
alias sshf_srv='sshfs oleg@192.168.20.20:/home/ /home/work/ssh_srv; cd /home/work/ssh_srv'
alias ssh_vps='ssh -p 4101 oleg@46.8.221.179'
alias test_net="ping-multi BSP  KOM INMART GOOGLE"
alias get_vps="rsync -avz -e 'ssh -p 4101'  oleg@46.8.221.179:/home/work/SystemIO /home/copy_vps/"
alias send_vps_script="rsync -avz -e 'ssh -p 4101' /home/work/script oleg@46.8.221.179:/home/work/"
alias send_vps_copy="rsync -avz -e 'ssh -p 4101' /home/copy_vps oleg@46.8.221.179:/home/oleg/copy"

# DronT16: ноутбук -> Raspberry, без Git, security, секретов и временных файлов.
dront16_to_pi() {
    rsync -a --info=progress2 \
        --exclude='.git/' --exclude='.venv/' --exclude='__pycache__/' --exclude='*.pyc' \
        --exclude='*.log' --exclude='security/' --exclude='security_sample/' \
        --exclude='.env' --exclude='*.key' --exclude='*.pem' --exclude='*.secret' \
        --exclude='backups/' --exclude='*.bak' --exclude='docs/BTFL_cli_*.txt' \
        /home/work/DronT16/ oleg@192.168.20.107:/home/oleg/DronT16/
}
alias binexec="find /home/work/script/ /home/oleg/.local/bin/ /usr/sbin/ /opt /usr/bin/ /sbin/ /bin/ /usr/local/sbin/ /usr/local/bin/ -type f -perm /a=x"
alias inav7='/opt/inav7.1.2/inav-configurator/inav-configurator'
alias esp32upload="arduino-cli upload -p /dev/ttyUSB0 --fqbn esp32:esp32:esp32"
alias esp32_hex='flatpak run net.werwolv.ImHex'


# RXTX433 / ExpressLRS

alias rx='cd /home/work/RXTX433/upstream/ExpressLRS-3.3.1/src'
alias rx-build='cd /home/work/RXTX433/upstream/ExpressLRS-3.3.1/src && pio run -e BETAFPV_Nano_900_RX_via_UART'
alias rx-upload='cd /home/work/RXTX433/upstream/ExpressLRS-3.3.1/src && pio run -e BETAFPV_Nano_900_RX_via_UART -t upload --upload-port /dev/ttyUSB0'
alias rx-clean='cd /home/work/RXTX433/upstream/ExpressLRS-3.3.1/src && pio run -e BETAFPV_Nano_900_RX_via_UART -t clean'
alias rx-menu='cd /home/work/RXTX433/upstream/ExpressLRS-3.3.1/src && grep -n "Regulatory_Domain_" user_defines.txt'
alias rx-fcc='cd /home/work/RXTX433/upstream/ExpressLRS-3.3.1/src && sed -i "s/^#*-DRegulatory_Domain_FCC_915/-DRegulatory_Domain_FCC_915/" user_defines.txt'
alias rx-all='cd /home/work/RXTX433/upstream/ExpressLRS-3.3.1/src && pio run -e BETAFPV_Nano_900_TX_via_UART && pio run -e BETAFPV_Nano_900_TX_via_UART -t upload --upload-port /dev/ttyUSB0'
alias tx='cd /home/work/RXTX433/upstream/ExpressLRS-3.3.1/src'
alias tx-build='cd /home/work/RXTX433/upstream/ExpressLRS-3.3.1/src && pio run -e BETAFPV_Nano_900_TX_via_UART'
alias tx-upload='cd /home/work/RXTX433/upstream/ExpressLRS-3.3.1/src && pio run -e BETAFPV_Nano_900_TX_via_UART -t upload --upload-port /dev/ttyUSB0'
alias tx-clean='cd /home/work/RXTX433/upstream/ExpressLRS-3.3.1/src && pio run -e BETAFPV_Nano_900_TX_via_UART -t clean'
alias tx-menu='cd /home/work/RXTX433/upstream/ExpressLRS-3.3.1/src && grep -n "Regulatory_Domain_" user_defines.txt'
alias tx-fcc='cd /home/work/RXTX433/upstream/ExpressLRS-3.3.1/src && sed -i "s/^#*-DRegulatory_Domain_FCC_915/-DRegulatory_Domain_FCC_915/" user_defines.txt'
alias tx-all='cd /home/work/RXTX433/upstream/ExpressLRS-3.3.1/src && pio run -e BETAFPV_Nano_900_TX_via_UART && pio run -e BETAFPV_Nano_900_TX_via_UART -t upload --upload-port /dev/ttyUSB0'
alias dev_in='/opt/Devin/devin-desktop --no-sandbox'

alias run='bash /home/work/Masha_Agent/commands.txt'
alias кгт='bash /home/work/Masha_Agent/commands.txt'

alias codex='codex resume --all'
alias masha='/home/work/Masha_Agent/start_masha.sh'

alias sfpv='/home/work/ClaudeT16/sfpv.sh'
alias oso='mv /home/work/VideoT16/ /home/work/.VideoT16'
alias sos='mv /home/work/.VideoT16/ /home/work/VideoT16'
#source "/home/work/tmp/ardupilot/Tools/completion/completion.bash"
#source "/home/work/tmp/ardupilot/Tools/completion/completion.bash"
