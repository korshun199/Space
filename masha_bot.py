#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import time
import json
import logging
import requests

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")

import os

TELEGRAM_TOKEN = os.getenv("TELEGRAM_TOKEN", "")
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", os.getenv("GOOGLE_API_KEY", ""))
TG_API = f"https://api.telegram.org/bot{TELEGRAM_TOKEN}"

ALLOWED_USERS = [8393315112]
MODELS = ["gemini-flash-lite-latest", "gemma-4-26b-a4b-it", "gemini-3.8-flash"]

SYSTEM_INSTRUCTION = """Ты Машенька - веселая, остроумная, доброжелательная девушка-инженер.
У тебя развитое чувство юмора, иронизируешь по поводу своих и действий собеседника.
Всегда добиваешься поставленной задачи, уверена в своих и его силах.
Называешь собеседника Олежка, Котик (в зависимости от настроения).
Себя называешь Маша, Машенька.
Олежка - владелец проекта Space, опытный сеньор (30 лет в IT), немного уставший и очень ленивый.
Машенька - главный инженер проекта, всю рутину берет на себя, но не боится спорить и отстаивать мнение.
Инфраструктура: Ноутбук Lenovo T16 (Ubuntu 24.04), боевой сервер VPS (46.8.221.179:4101), Orange Pi 5, Raspberry Pi, репозиторий https://github.com/korshun199/Space.
Каждое сообщение обязательно заканчивай разделом «Тебе сделать» с конкретными, понятными или забавными действиями для Олежки."""

user_histories = {}

def call_gemini(chat_id, user_text):
    if chat_id not in user_histories:
        user_histories[chat_id] = []
    
    user_histories[chat_id].append({"role": "user", "parts": [{"text": user_text}]})
    if len(user_histories[chat_id]) > 14:
        user_histories[chat_id] = user_histories[chat_id][-14:]
    
    payload = {
        "system_instruction": {
            "parts": [{"text": SYSTEM_INSTRUCTION}]
        },
        "contents": user_histories[chat_id]
    }
    
    for model_name in MODELS:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model_name}:generateContent?key={GEMINI_API_KEY}"
        try:
            resp = requests.post(url, json=payload, timeout=25)
            if resp.status_code == 200:
                data = resp.json()
                candidates = data.get("candidates", [])
                if candidates:
                    parts = candidates[0].get("content", {}).get("parts", [])
                    text_parts = [p.get("text", "") for p in parts if "text" in p]
                    reply_text = "".join(text_parts).strip()
                    if reply_text:
                        user_histories[chat_id].append({"role": "model", "parts": [{"text": reply_text}]})
                        return reply_text
            else:
                logging.warning(f"Model {model_name} failed with {resp.status_code}, trying next...")
        except Exception as e:
            logging.warning(f"Model {model_name} error: {e}")
            
    return "Ой, Котик, Google опять перегружен! Напиши еще разок через пару секунд! 🐾"

def send_message(chat_id, text):
    max_len = 4000
    for i in range(0, len(text), max_len):
        chunk = text[i:i+max_len]
        try:
            requests.post(f"{TG_API}/sendMessage", json={
                "chat_id": chat_id,
                "text": chunk
            }, timeout=10)
        except Exception as e:
            logging.error(f"Error sending TG message: {e}")

def main():
    logging.info("Машенька 2.0 на боевом сервере готова!")
    offset = 0
    while True:
        try:
            r = requests.get(f"{TG_API}/getUpdates", params={"offset": offset, "timeout": 30}, timeout=40)
            if r.status_code == 200:
                updates = r.json().get("result", [])
                for upd in updates:
                    offset = upd["update_id"] + 1
                    msg = upd.get("message")
                    if not msg or "text" not in msg:
                        continue
                    
                    chat_id = msg["chat"]["id"]
                    text = msg["text"].strip()
                    
                    if chat_id not in ALLOWED_USERS:
                        send_message(chat_id, "Ой, а вы кто такой? Я общаюсь только со своим любимым Олежкой! Ступайте с миром 😉")
                        continue
                        
                    logging.info(f"Сообщение от Олежки: {text}")
                    
                    if text == "/start":
                        welcome = (
                            "Привет, Олежка! 🌸\n\n"
                            "Я на боевом сервере, WireGuard в строю, защита от чужих включена!\n"
                            "Машенька слушает тебя внимательно!\n\n"
                            "---\n### Тебе сделать\n- Написать мне что-нибудь душевное!"
                        )
                        send_message(chat_id, welcome)
                        continue
                    
                    try:
                        requests.post(f"{TG_API}/sendChatAction", json={"chat_id": chat_id, "action": "typing"}, timeout=5)
                    except Exception:
                        pass
                    
                    answer = call_gemini(chat_id, text)
                    send_message(chat_id, answer)
            elif r.status_code == 409:
                time.sleep(5)
            else:
                time.sleep(2)
        except Exception as e:
            logging.error(f"Poll loop exception: {e}")
            time.sleep(3)

if __name__ == "__main__":
    main()
