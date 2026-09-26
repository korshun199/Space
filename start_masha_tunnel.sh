#!/usr/bin/env bash
# start_masha_tunnel.sh — запуск агента Машеньки на ноутбуке и реверс-туннеля на VPS (lora.systemio.ru)

pkill -f "adk web" 2>/dev/null
pkill -f "ssh -N -R 8000:localhost:8000" 2>/dev/null

export GOOGLE_API_KEY=$(grep GOOGLE_API_KEY /home/work/Space/my_agent/.env | cut -d'=' -f2- | tr -d '\"')

# Запуск агента Google ADK на ноутбуке в режиме единого агента
nohup /home/oleg/.local/bin/adk web --host 0.0.0.0 --port 8000 --allow_origins "*" --logo-text "Машенька" --logo-image-url "adk_favicon.svg" /home/work/Space/my_agent > /tmp/masha_adk.log 2>&1 &

sleep 2

# Запуск постоянного реверс-туннеля на VPS (systemio.ru)
nohup ssh -N -R 8000:localhost:8000 -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o ExitOnForwardFailure=yes 46.8.221.179 > /tmp/masha_tunnel.log 2>&1 &

echo "Машенька запущена на ноутбуке и доступна через реверс: http://lora.systemio.ru"
