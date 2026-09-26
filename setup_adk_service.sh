#!/usr/bin/env bash
# ==============================================================================
# setup_adk_service.sh — Настройка службы Google ADK Машеньки на VPS
# ==============================================================================
set -euo pipefail

echo "🛑 1. Остановка старых служб на порту 8000..."
systemctl stop masha.service 2>/dev/null || true
systemctl disable masha.service 2>/dev/null || true

echo "⚙️ 2. Создание системного сервиса adk-masha.service..."
cat << 'EOF' > /etc/systemd/system/adk-masha.service
[Unit]
Description=Google ADK Agent Masha Service
After=network.target

[Service]
User=oleg
Group=oleg
WorkingDirectory=/home/work/Space
EnvironmentFile=/home/work/Space/my_agent/.env
ExecStart=/home/work/Space/venv/bin/adk web --host 127.0.0.1 --port 8000 --allow_origins "*" --logo-text "Машенька" /home/work/Space/my_agent
Restart=always
RestartSec=3

[Install]
WantedBy=multi-user.target
EOF

echo "🔄 3. Перезагрузка демонов и запуск службы..."
systemctl daemon-reload
systemctl enable --now adk-masha.service
systemctl restart adk-masha.service

echo "🔍 4. Проверка статуса службы..."
sleep 2
systemctl status adk-masha.service --no-pager | head -n 15
echo "✅ Служба ADK Машеньки успешно развёрнута на VPS!"
