#!/usr/bin/env bash
# ==============================================================================
# setup_auth_nginx.sh — Включение авторизации по PIN-коду (0711) в Nginx на VPS
# ==============================================================================
set -euo pipefail

echo "🛡️ Настройка Nginx с защитой по куке авторизации (0711)..."

AUTH_HASH="71c056a29a4be95c528336527b9ab5872c3ba14ebcd6a5b007dc48e0cbade72d"

cat << EOF > /etc/nginx/sites-available/lora.systemio.ru
# Карта авторизации: проверяет куку masha_auth
map \$cookie_masha_auth \$is_authorized {
    default 0;
    "${AUTH_HASH}" 1;
}

server {
    listen 80;
    server_name lora.systemio.ru;
    return 301 https://\$host\$request_uri;
}

server {
    listen 443 ssl http2;
    server_name lora.systemio.ru;

    ssl_certificate /etc/letsencrypt/live/lora.systemio.ru/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/lora.systemio.ru/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    # Статическая страница входа
    location = /login.html {
        root /home/work/Space/auth;
        try_files /login.html =404;
    }

    # Все остальные запросы проверяем на авторизацию
    location / {
        if (\$is_authorized = 0) {
            return 302 /login.html;
        }

        proxy_pass http://127.0.0.1:8000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_buffering off;
        proxy_read_timeout 86400s;
    }
}
EOF

ln -sf /etc/nginx/sites-available/lora.systemio.ru /etc/nginx/sites-enabled/lora.systemio.ru

echo "🔍 Проверка конфигурации Nginx..."
nginx -t

echo "🔄 Перезагрузка Nginx..."
systemctl reload nginx

echo "✅ Защита Space Shield активирована на lora.systemio.ru!"
