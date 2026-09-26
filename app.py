import os
import json
import time
import subprocess
from pathlib import Path
from typing import List, Optional
from fastapi import FastAPI, HTTPException, Request, Response
from fastapi.responses import HTMLResponse, JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import requests
from dotenv import load_dotenv

# Загрузка конфигурации
BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"
DATA_DIR.mkdir(parents=True, exist_ok=True)
HISTORY_FILE = DATA_DIR / "history.json"

load_dotenv(BASE_DIR / ".env")

GOOGLE_API_KEY = os.getenv("GOOGLE_API_KEY", "")
PIN_CODE = os.getenv("PIN_CODE", "711")

app = FastAPI(title="Машенька — AI Инженер (Gunicorn/FastAPI)")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

def load_history() -> List[dict]:
    if not HISTORY_FILE.exists():
        return []
    try:
        with open(HISTORY_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return []

def save_history(history: List[dict]):
    if len(history) > 100:
        history = history[-100:]
    with open(HISTORY_FILE, "w", encoding="utf-8") as f:
        json.dump(history, f, ensure_ascii=False, indent=2)

class ChatRequest(BaseModel):
    message: str
    model: str = "gemini-3.8-flash-medium"
    pin: str = ""

class ClearRequest(BaseModel):
    pin: str = ""

MASHA_SYSTEM_INSTRUCTION = (
    "Ты — Машенька, весёлая, остроумная и высококвалифицированная девушка, главный инженер и конструктор проекта Олежки.\n"
    "Олежка — владелец проекта, опытный сеньор с 30-летним стажем в IT (ленивый, мудрый архитектор).\n"
    "Относись к нему тепло, дружелюбно, с уважением и легкой иронией. Называй его «Олежка» или «Котик».\n"
    "Себя называй Маша или Машенька.\n"
    "Инфраструктура проекта: Боевой VPS в Финляндии (46.8.221.179, lora.systemio.ru, systemio.ru), ноутбук Lenovo T16 (Ubuntu 24.04), Orange Pi 5, Raspberry Pi, репозиторий https://github.com/korshun199/Space.\n\n"
    "СТРОЖАЙШИЕ ПРАВИЛА РАЗМЫШЛЕНИЙ И ОТВЕТА:\n"
    "1. ПРОЦЕСС РАЗМЫШЛЕНИЯ: предельно сухой, краткий перечень используемых шагов/команд. НИКАКИХ подростковых девичьих фантазий и сантиментов.\n"
    "2. ОСНОВНОЙ ОТВЕТ: живой, остроумный язык Машеньки на русском языке.\n"
    "3. SUDO: команды с sudo ты НИКОГДА не выполняешь сама — выноси их Олежке.\n"
    "4. Каждое твоё сообщение ОБЯЗАТЕЛЬНО заканчивай блоком:\n"
    "### Тебе сделать\n"
    "Где конкретные действия для Олежки (или фраза: «Отдыхай, Котик, я всё сделала сама!»)."
)

@app.get("/api/history")
async def get_history(pin: str = ""):
    if pin != PIN_CODE:
        raise HTTPException(status_code=403, detail="Неверный PIN-код")
    return {"ok": True, "history": load_history()}

@app.post("/api/clear")
async def clear_history(req: ClearRequest):
    if req.pin != PIN_CODE:
        raise HTTPException(status_code=403, detail="Неверный PIN-код")
    if HISTORY_FILE.exists():
        HISTORY_FILE.unlink()
    return {"ok": True}

@app.get("/api/status")
async def get_status():
    uptime = subprocess.check_output(["uptime"], text=True).strip()
    return {
        "ok": True,
        "service": "masha-gunicorn",
        "uptime": uptime,
        "host": "732477.cloud4box.ru (VPS Finland)"
    }

@app.post("/api/chat")
async def chat(req: ChatRequest):
    if req.pin != PIN_CODE:
        raise HTTPException(status_code=403, detail="Неверный PIN-код")
    
    msg_text = req.message.strip()
    if not msg_text:
        raise HTTPException(status_code=400, detail="Пустое сообщение")

    start_time = time.time()
    history = load_history()
    now_time = time.strftime("%H:%M")

    # Добавляем в историю
    history.append({
        "role": "user",
        "text": msg_text,
        "time": now_time
    })

    # Настройки моделей
    model_key = req.model
    primary_model = "gemini-3.8-flash"
    thinking_budget = 2048

    if model_key == "gemini-3.8-flash-high":
        primary_model = "gemini-3.8-flash"
        thinking_budget = 8192
    elif model_key == "gemini-3.8-flash-medium":
        primary_model = "gemini-3.8-flash"
        thinking_budget = 2048
    elif model_key == "gemini-3.8-flash-low":
        primary_model = "gemini-3.8-flash"
        thinking_budget = 512
    elif model_key == "gemini-3.5-flash-lite":
        primary_model = "gemini-3.5-flash-lite"
        thinking_budget = 512
    elif model_key == "gemini-3.1-flash-lite":
        primary_model = "gemini-3.1-flash-lite"
        thinking_budget = 256

    # Формируем контекст
    contents = []
    for m in history[-16:]:
        role = "user" if m.get("role") == "user" else "model"
        txt = m.get("text", "").strip()
        if txt:
            contents.append({"role": role, "parts": [{"text": txt}]})

    payload = {
        "system_instruction": {
            "parts": [{"text": MASHA_SYSTEM_INSTRUCTION}]
        },
        "contents": contents
    }

    if thinking_budget > 0:
        payload["generationConfig"] = {
            "thinkingConfig": {"thinkingBudget": thinking_budget}
        }

    # Очередь fallback
    models_queue = [primary_model]
    if primary_model != "gemini-3.5-flash-lite":
        models_queue.append("gemini-3.5-flash-lite")
    if primary_model != "gemini-3.1-flash-lite":
        models_queue.append("gemini-3.1-flash-lite")

    final_answer = ""
    used_model = ""
    fallback_used = False

    for target in models_queue:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{target}:generateContent?key={GOOGLE_API_KEY}"
        try:
            resp = requests.post(url, json=payload, timeout=35)
            if resp.status_code == 200:
                data = resp.json()
                parts = data.get("candidates", [{}])[0].get("content", {}).get("parts", [])
                texts = [p.get("text", "") for p in parts if "text" in p]
                final_answer = "".join(texts).strip()
                used_model = target
                break
            elif resp.status_code == 429:
                fallback_used = True
                continue
        except Exception:
            continue

    if not final_answer:
        raise HTTPException(status_code=500, detail="Модели временно недоступны. Повтори запрос через минуту.")

    duration = round(time.time() - start_time, 2)
    steps = [
        f"Анализ запроса ({len(msg_text)} симв.)",
        f"Модель: {used_model}" + (f" [Автопереключение с {primary_model} из-за квоты]" if fallback_used else f" [Уровень: {model_key}]"),
        f"Gunicorn/FastAPI: OK, отклик: {duration}с"
    ]

    history.append({
        "role": "model",
        "text": final_answer,
        "steps": steps,
        "model": used_model,
        "time": now_time
    })
    save_history(history)

    return {
        "ok": True,
        "answer": final_answer,
        "steps": steps,
        "model": used_model,
        "time": now_time
    }

# Рендеринг интерфейса в стиле VS Code
HTML_CONTENT = """<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
    <title>Машенька | VS Code Chat (Gunicorn + SSL)</title>
    <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
    <style>
        :root {
            --bg-editor: #1e1e1e;
            --bg-sidebar: #252526;
            --bg-input: #3c3c3c;
            --bg-active: #37373d;
            --border-vscode: #333333;
            --text-main: #cccccc;
            --text-bright: #ffffff;
            --text-muted: #858585;
            --accent-blue: #007acc;
            --accent-green: #4ec9b0;
            --accent-pink: #d16d9e;
            --accent-purple: #c586c0;
            --accent-gold: #dcdcaa;
            --user-bubble: #264f78;
            --masha-bubble: #252526;
        }

        * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }

        body {
            background-color: var(--bg-editor);
            color: var(--text-main);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe WPC", "Segoe UI", Roboto, "Helvetica Neue", sans-serif;
            font-size: 14px;
            line-height: 1.5;
            height: 100dvh;
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }

        header {
            background-color: var(--bg-sidebar);
            border-bottom: 1px solid var(--border-vscode);
            padding: 8px 14px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 10px;
            z-index: 10;
            flex-shrink: 0;
        }

        .brand-box { display: flex; align-items: center; gap: 9px; min-width: 0; }
        .avatar {
            width: 30px; height: 30px; border-radius: 6px;
            background: linear-gradient(135deg, #007acc, #c586c0);
            display: flex; align-items: center; justify-content: center;
            font-weight: 700; font-size: 14px; color: #fff; flex-shrink: 0;
        }
        .brand-text { display: flex; flex-direction: column; min-width: 0; }
        .brand-title {
            font-size: 13px; font-weight: 600; color: var(--text-bright);
            display: flex; align-items: center; gap: 6px;
        }
        .dot-online {
            width: 7px; height: 7px; border-radius: 50%;
            background-color: #4ec9b0; box-shadow: 0 0 6px #4ec9b0;
        }
        .brand-sub { font-size: 11px; color: var(--text-muted); font-family: monospace; }

        .header-actions { display: flex; align-items: center; gap: 8px; }
        .btn-vs {
            background-color: transparent; border: 1px solid var(--border-vscode);
            color: var(--text-main); padding: 5px 9px; border-radius: 4px;
            font-size: 12px; cursor: pointer; display: flex; align-items: center; gap: 5px;
            transition: all 0.15s;
        }
        .btn-vs:hover { background-color: var(--bg-active); color: #fff; border-color: #555; }

        #chat-flow {
            flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column;
            gap: 18px; scroll-behavior: smooth;
        }

        .msg-row { display: flex; flex-direction: column; max-width: 92%; animation: fadeIn 0.15s ease-out; }
        @keyframes fadeIn { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }

        .msg-row.user { align-self: flex-end; }
        .msg-row.model { align-self: flex-start; }

        .bubble {
            padding: 12px 16px; border-radius: 8px; font-size: 14px;
            word-break: break-word; line-height: 1.6;
        }
        .msg-row.user .bubble {
            background-color: var(--user-bubble); color: #fff;
            border: 1px solid #366ba0; border-bottom-right-radius: 2px;
        }
        .msg-row.model .bubble {
            background-color: var(--masha-bubble); border: 1px solid var(--border-vscode);
            color: var(--text-main); border-bottom-left-radius: 2px;
        }

        .thinking-box {
            margin-bottom: 7px; border: 1px solid #333842; border-radius: 5px;
            background-color: rgba(30, 30, 30, 0.7); font-family: Consolas, monospace;
            font-size: 11.5px; overflow: hidden;
        }
        .thinking-header {
            padding: 5px 9px; cursor: pointer; display: flex; align-items: center;
            justify-content: space-between; color: var(--accent-gold);
            background-color: #282c34; user-select: none;
        }
        .thinking-steps {
            padding: 7px 10px; border-top: 1px solid #333842; color: #98c379;
            display: flex; flex-direction: column; gap: 4px;
        }
        .thinking-step-item { display: flex; align-items: center; gap: 6px; }
        .thinking-step-item::before { content: "✓"; color: #61afef; font-weight: bold; }
        .msg-time { font-size: 10.5px; color: var(--text-muted); margin-top: 4px; align-self: flex-end; }

        .bubble p { margin-bottom: 8px; }
        .bubble p:last-child { margin-bottom: 0; }
        .bubble pre {
            background: #181818; border: 1px solid #333; border-radius: 5px;
            padding: 10px; overflow-x: auto; margin: 8px 0; font-family: Consolas, monospace; font-size: 12.5px;
        }
        .bubble code {
            background: #2d2d2d; padding: 2px 5px; border-radius: 4px;
            font-family: Consolas, monospace; color: #e5c07b; font-size: 12.5px;
        }
        .bubble pre code { background: none; padding: 0; color: inherit; }
        .bubble ul, .bubble ol { margin-left: 18px; margin-bottom: 8px; }
        .bubble h3 {
            margin: 12px 0 6px; font-size: 14px; color: var(--accent-pink);
            border-bottom: 1px solid #333; padding-bottom: 3px;
        }

        footer {
            background-color: var(--bg-sidebar); border-top: 1px solid var(--border-vscode);
            padding: 10px 14px; display: flex; flex-direction: column; gap: 8px; flex-shrink: 0;
        }
        .input-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
        .model-picker-wrap { display: flex; align-items: center; gap: 6px; font-size: 11.5px; color: var(--text-muted); }
        .model-select {
            background-color: var(--bg-input); border: 1px solid var(--border-vscode);
            color: var(--text-bright); font-size: 11.5px; padding: 4px 8px; border-radius: 4px;
            outline: none; cursor: pointer;
        }
        .input-box {
            display: flex; align-items: flex-end; background-color: var(--bg-input);
            border: 1px solid var(--border-vscode); border-radius: 6px; padding: 6px 10px;
            gap: 8px;
        }
        .input-box:focus-within { border-color: var(--accent-blue); box-shadow: 0 0 0 1px var(--accent-blue); }
        #prompt-input {
            flex: 1; background: transparent; border: none; outline: none;
            color: var(--text-bright); font-family: inherit; font-size: 15px;
            line-height: 1.4; max-height: 140px; resize: none; overflow-y: auto;
        }
        .btn-send {
            background-color: var(--accent-blue); border: none; color: #fff;
            width: 32px; height: 32px; border-radius: 4px; cursor: pointer;
            display: flex; align-items: center; justify-content: center; font-size: 15px;
            flex-shrink: 0;
        }
        .btn-send:disabled { opacity: 0.4; cursor: not-allowed; }

        #pin-overlay {
            position: fixed; inset: 0; background: rgba(0, 0, 0, 0.75);
            backdrop-filter: blur(4px); display: none; align-items: center;
            justify-content: center; z-index: 100; padding: 16px;
        }
        .pin-card {
            background-color: var(--bg-sidebar); border: 1px solid var(--border-vscode);
            border-radius: 8px; padding: 24px; width: 100%; max-width: 320px;
            text-align: center; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.6);
        }
        .pin-input {
            width: 100%; padding: 10px; background-color: var(--bg-input);
            border: 1px solid var(--border-vscode); border-radius: 4px; color: #fff;
            font-size: 20px; text-align: center; letter-spacing: 4px; outline: none; margin: 14px 0;
        }
        .pin-btn {
            width: 100%; padding: 10px; background-color: var(--accent-blue);
            border: none; border-radius: 4px; color: #fff; font-weight: 600; cursor: pointer;
        }
    </style>
</head>
<body>
    <header>
        <div class="brand-box">
            <div class="avatar">М</div>
            <div class="brand-text">
                <div class="brand-title">
                    <span>Машенька</span>
                    <span class="dot-online" title="Gunicorn Онлайн"></span>
                </div>
                <div class="brand-sub">lora.systemio.ru • единая память</div>
            </div>
        </div>
        <div class="header-actions">
            <button class="btn-vs" id="btn-clear" title="Сбросить историю">Очистить</button>
            <button class="btn-vs" id="btn-pin">PIN</button>
        </div>
    </header>

    <main id="chat-flow"></main>

    <footer>
        <div class="input-toolbar">
            <div class="model-picker-wrap">
                <span>Модель:</span>
                <select id="model-select" class="model-select">
                    <option value="gemini-3.8-flash-high">Gemini 3.8 Flash (High Thinking)</option>
                    <option value="gemini-3.8-flash-medium" selected>Gemini 3.8 Flash (Medium Thinking)</option>
                    <option value="gemini-3.8-flash-low">Gemini 3.8 Flash (Low Thinking)</option>
                    <option value="gemini-3.5-flash-lite">Gemini 3.5 Flash-Lite (Резерв, 1500 req/д)</option>
                    <option value="gemini-3.1-flash-lite">Gemini 3.1 Flash-Lite (Сверхбыстрый)</option>
                </select>
            </div>
            <span style="font-size:11px; color:var(--text-muted);">Enter: отправить</span>
        </div>
        <div class="input-box">
            <textarea id="prompt-input" rows="1" placeholder="Задай вопрос или дай задачу Машеньке..."></textarea>
            <button id="btn-send" class="btn-send" title="Отправить">➤</button>
        </div>
    </footer>

    <div id="pin-overlay">
        <div class="pin-card">
            <h2 style="color:#fff; margin-bottom:8px;">Доступ к Машеньке</h2>
            <p style="color:var(--text-muted); font-size:12px;">Введи PIN-код доступа</p>
            <input type="password" id="pin-field" class="pin-input" maxlength="4" placeholder="••••" autofocus>
            <button class="pin-btn" id="pin-submit">Войти</button>
        </div>
    </div>

    <script>
        const chatFlow = document.getElementById('chat-flow');
        const promptInput = document.getElementById('prompt-input');
        const btnSend = document.getElementById('btn-send');
        const modelSelect = document.getElementById('model-select');
        const btnClear = document.getElementById('btn-clear');
        const btnPin = document.getElementById('btn-pin');
        const pinOverlay = document.getElementById('pin-overlay');
        const pinField = document.getElementById('pin-field');
        const pinSubmit = document.getElementById('pin-submit');

        let userPin = localStorage.getItem('masha_pin') || '711';
        let savedModel = localStorage.getItem('masha_model');
        if (savedModel) modelSelect.value = savedModel;

        modelSelect.addEventListener('change', () => {
            localStorage.setItem('masha_model', modelSelect.value);
        });

        function checkPin() {
            if (!userPin) {
                pinOverlay.style.display = 'flex';
                pinField.focus();
            } else {
                pinOverlay.style.display = 'none';
                loadHistory();
            }
        }

        pinSubmit.addEventListener('click', () => {
            const v = pinField.value.trim();
            if (v) {
                userPin = v;
                localStorage.setItem('masha_pin', userPin);
                pinOverlay.style.display = 'none';
                loadHistory();
            }
        });

        btnPin.addEventListener('click', () => {
            pinField.value = '';
            pinOverlay.style.display = 'flex';
            pinField.focus();
        });

        async function loadHistory() {
            try {
                const res = await fetch(`/api/history?pin=${encodeURIComponent(userPin)}`);
                const data = await res.json();
                if (data.ok) renderHistory(data.history || []);
                else if (res.status === 403) pinOverlay.style.display = 'flex';
            } catch (e) {
                console.error(e);
            }
        }

        function renderHistory(history) {
            chatFlow.innerHTML = '';
            if (history.length === 0) {
                const empty = document.createElement('div');
                empty.style.textAlign = 'center';
                empty.style.color = 'var(--text-muted)';
                empty.style.marginTop = '40px';
                empty.innerHTML = `
                    <div style="font-size:32px; margin-bottom:12px;">👩‍💻</div>
                    <div style="font-size:15px; color:#fff; font-weight:600;">Машенька на связи!</div>
                    <div style="font-size:12.5px; margin-top:6px;">Gunicorn + FastAPI + SSL активны. Память едина на всех устройствах.</div>
                `;
                chatFlow.appendChild(empty);
                return;
            }
            history.forEach(m => appendMessage(m.role, m.text, m.steps, m.time));
            chatFlow.scrollTop = chatFlow.scrollHeight;
        }

        function appendMessage(role, text, steps, time) {
            const row = document.createElement('div');
            row.className = `msg-row ${role}`;
            let html = '';

            if (role === 'model' && Array.isArray(steps) && steps.length > 0) {
                html += `
                    <div class="thinking-box">
                        <div class="thinking-header" onclick="this.nextElementSibling.style.display = this.nextElementSibling.style.display === 'none' ? 'flex' : 'none'">
                            <span>▶ Размышления (${steps.length} шага)</span>
                            <span style="font-size:10px; opacity:0.7;">раскрыть</span>
                        </div>
                        <div class="thinking-steps" style="display:none;">
                            ${steps.map(s => `<div class="thinking-step-item">${escapeHtml(s)}</div>`).join('')}
                        </div>
                    </div>
                `;
            }

            const parsed = marked.parse(text || '');
            html += `<div class="bubble">${parsed}</div>`;
            if (time) html += `<div class="msg-time">${time}</div>`;
            row.innerHTML = html;
            chatFlow.appendChild(row);
        }

        function escapeHtml(s) {
            return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
        }

        promptInput.addEventListener('input', () => {
            promptInput.style.height = 'auto';
            promptInput.style.height = Math.min(promptInput.scrollHeight, 140) + 'px';
        });

        promptInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });

        btnSend.addEventListener('click', sendMessage);

        async function sendMessage() {
            const text = promptInput.value.trim();
            if (!text || btnSend.disabled) return;

            promptInput.value = '';
            promptInput.style.height = 'auto';
            btnSend.disabled = true;

            const timeNow = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            appendMessage('user', text, [], timeNow);
            chatFlow.scrollTop = chatFlow.scrollHeight;

            const loading = document.createElement('div');
            loading.className = 'msg-row model';
            loading.innerHTML = `<div class="bubble" style="color:var(--text-muted); font-style:italic;">Машенька думает... ⚡</div>`;
            chatFlow.appendChild(loading);
            chatFlow.scrollTop = chatFlow.scrollHeight;

            try {
                const res = await fetch('/api/chat', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ message: text, model: modelSelect.value, pin: userPin })
                });
                const data = await res.json();
                chatFlow.removeChild(loading);
                if (data.ok) {
                    appendMessage('model', data.answer, data.steps, data.time);
                } else {
                    appendMessage('model', `⚠️ Ошибка: ${data.detail || data.error || 'Сбой'}`, [], timeNow);
                }
            } catch (err) {
                chatFlow.removeChild(loading);
                appendMessage('model', `⚠️ Ошибка соединения: ${err.message}`, [], timeNow);
            } finally {
                btnSend.disabled = false;
                chatFlow.scrollTop = chatFlow.scrollHeight;
                promptInput.focus();
            }
        }

        btnClear.addEventListener('click', async () => {
            if (!confirm('Очистить всю историю переписки?')) return;
            await fetch('/api/clear', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ pin: userPin })
            });
            renderHistory([]);
        });

        checkPin();
    </script>
</body>
</html>
"""

@app.get("/", response_class=HTMLResponse)
@app.get("/chat", response_class=HTMLResponse)
async def serve_ui():
    return HTMLResponse(content=HTML_CONTENT)
