#!/usr/bin/env bash
# ==============================================================================
# setup_adk_service.sh — Развертывание службы Google ADK Машеньки на VPS
# ==============================================================================
set -euo pipefail

echo "⚙️ Настройка системного сервиса adk-masha.service..."
cat << 'EOF' > /etc/systemd/system/adk-masha.service
[Unit]
Description=Google ADK Agent Masha Service
After=network.target

[Service]
User=oleg
Group=oleg
WorkingDirectory=/home/work/Space
EnvironmentFile=/home/work/Space/my_agent/.env
ExecStart=/home/work/Space/venv/bin/adk web --host 127.0.0.1 --port 8000 --allow_origins "*" /home/work/Space/my_agent
Restart=always
RestartSec=3

[Install]
WantedBy=multi-user.target
EOF

echo "🔄 Перезагрузка демонов systemd и запуск..."
systemctl daemon-reload
systemctl enable --now adk-masha.service
systemctl restart adk-masha.service

echo "🔍 Проверка статуса..."
systemctl status adk-masha.service --no-pager | head -n 12
echo "✅ Служба adk-masha успешно запущена!"
