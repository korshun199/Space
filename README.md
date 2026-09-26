# Space

Проект рабочей среды, инфраструктуры и персонального ассистента «Машенька» (FastAPI + Gunicorn + Gemini API).

## Структура
- `app.py` — веб-сервис ассистента Машеньки (FastAPI, единая история диалогов, выбор моделей и параметров размышлений).
- `requirements.txt` — зависимости Python.
- `install_masha_gunicorn_ssl.sh` — скрипт автоматической настройки Gunicorn, Systemd, Nginx и Let's Encrypt SSL.
- `masha.php` — интерфейс интеграции с сайтом.
- `my_agent/` — локальные сценарии и агенты.
