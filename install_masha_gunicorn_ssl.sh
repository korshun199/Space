#!/usr/bin/env bash
# ==============================================================================
# install_masha_gunicorn_ssl.sh
# Автоматическая настройка: Python-окружение, Gunicorn + Uvicorn, Systemd,
# Nginx виртуальный хост и бесплатный 90-дневный SSL (Certbot) для lora.systemio.ru
# ==============================================================================

set -euo pipefail

echo "=========================================================="
echo "🚀 1. Установка пакетов (Python, venv, Certbot Nginx)..."
echo "=========================================================="
apt-get update -y
apt-get install -y python3-pip python3-venv certbot python3-certbot-nginx

SERVER_DIR="/home/work/Space"
mkdir -p "$SERVER_DIR/data"
chown -R oleg:oleg "$SERVER_DIR"

echo "=========================================================="
echo "🐍 2. Настройка виртуального окружения и Gunicorn..."
echo "=========================================================="
if [ ! -f "$SERVER_DIR/venv/bin/gunicorn" ]; then
    sudo -u oleg python3 -m venv "$SERVER_DIR/venv"
    sudo -u oleg "$SERVER_DIR/venv/bin/pip" install --upgrade pip
    sudo -u oleg "$SERVER_DIR/venv/bin/pip" install -r "$SERVER_DIR/requirements.txt"
fi

echo "=========================================================="
echo "⚙️ 3. Настройка и запуск Systemd-сервиса masha.service..."
echo "=========================================================="
cat << 'EOF' > /etc/systemd/system/masha.service
[Unit]
Description=Masha AI Service (Gunicorn + Uvicorn)
After=network.target

[Service]
User=oleg
Group=oleg
WorkingDirectory=/home/work/Space
EnvironmentFile=/home/work/Space/.env
ExecStart=/home/work/Space/venv/bin/gunicorn -w 2 -k uvicorn.workers.UvicornWorker app:app --bind 127.0.0.1:8000
Restart=always
RestartSec=3

[Install]
WantedBy=multi-user.target
EOF

# Останавливаем старый lora-server если был включен
systemctl stop lora-server 2>/dev/null || true
systemctl disable lora-server 2>/dev/null || true

systemctl daemon-reload
systemctl enable --now masha.service
systemctl restart masha.service

echo "=========================================================="
echo "🌐 4. Настройка Nginx для lora.systemio.ru..."
echo "=========================================================="
cat << 'EOF' > /etc/nginx/sites-available/lora.systemio.ru
server {
    listen 80;
    server_name lora.systemio.ru;

    location / {
        proxy_pass http://127.0.0.1:8000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_buffering off;
        proxy_read_timeout 86400s;
    }
}
EOF

ln -sf /etc/nginx/sites-available/lora.systemio.ru /etc/nginx/sites-enabled/lora.systemio.ru
nginx -t
systemctl reload nginx

echo "=========================================================="
echo "🔒 5. Выпуск бесплатного 90-дневного SSL от Let's Encrypt..."
echo "=========================================================="
certbot --nginx -d lora.systemio.ru --redirect --non-interactive --agree-tos --register-unsafely-without-email || \
certbot --nginx -d lora.systemio.ru --redirect --non-interactive --agree-tos -m korshun199@gmail.com || true

nginx -t
systemctl reload nginx

echo "=========================================================="
echo "✅ ПРОВЕРКА РАБОТЫ СЕРВИСА..."
echo "=========================================================="
sleep 2
systemctl status masha.service --no-pager | head -n 12

echo ""
echo "🎉 ВСЁ ГОТОВО!"
echo "Открывай в телефоне или браузере:"
echo "👉 https://lora.systemio.ru"
echo "=========================================================="
