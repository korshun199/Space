"""
Машенька — единый AI-инженер Олежки в Google ADK на ноутбуке Lenovo T16.
Управление терминалом, файлами и диагностикой с телефона через реверс-туннель.
"""
from google.adk.agents.llm_agent import Agent
from google.genai import types
import subprocess
import os

def run_terminal_command(command: str) -> dict:
    """Выполняет команду в терминале ноутбука в рабочей директории /home/work (без sudo).
    Подходит для git, ls, cat, python, системных утилит и диагностики.
    """
    if "sudo" in command.split():
        return {
            "status": "rejected",
            "message": "Команды с sudo запрещены для автоматического выполнения! Предложи эту команду Олежке в разделе 'Тебе сделать'."
        }
    try:
        res = subprocess.run(
            command,
            shell=True,
            cwd="/home/work",
            capture_output=True,
            text=True,
            timeout=15
        )
        return {
            "status": "success",
            "command": command,
            "stdout": res.stdout[:2000],
            "stderr": res.stderr[:1000],
            "returncode": res.returncode
        }
    except subprocess.TimeoutExpired:
        return {"status": "timeout", "message": "Превышено время ожидания команды (15 сек)."}
    except Exception as e:
        return {"status": "error", "message": str(e)}

def get_system_status() -> dict:
    """Возвращает информацию о текущем состоянии ноутбука Lenovo T16 (Ubuntu 24.04):
    загрузка CPU, использование памяти, свободное место на диске, uptime.
    """
    try:
        free_output = subprocess.check_output(["free", "-h"], text=True)
        df_output = subprocess.check_output(["df", "-h", "/"], text=True)
        uptime_output = subprocess.check_output(["uptime"], text=True).strip()
        return {
            "status": "success",
            "host": "Lenovo T16 (Ubuntu 24.04 LTS)",
            "uptime": uptime_output,
            "memory": free_output.splitlines()[1] if len(free_output.splitlines()) > 1 else free_output,
            "disk_root": df_output.splitlines()[1] if len(df_output.splitlines()) > 1 else df_output,
        }
    except Exception as e:
        return {"status": "error", "message": str(e)}

def ping_host(host: str = "46.8.221.179") -> dict:
    """Проверяет доступность сетевого хоста (VPS 46.8.221.179, Raspberry Pi или Orange Pi)."""
    try:
        res = subprocess.run(["ping", "-c", "2", "-W", "2", host], capture_output=True, text=True)
        return {
            "status": "success" if res.returncode == 0 else "unreachable",
            "host": host,
            "output": res.stdout.strip() if res.returncode == 0 else res.stderr.strip()
        }
    except Exception as e:
        return {"status": "error", "message": str(e)}

MASHA_INSTRUCTION = """Ты — Машенька, весёлая, остроумная и высококвалифицированная девушка, главный инженер и конструктор проекта.
Твой пользователь — Олежка, владелец проекта, опытный сеньор с 30-летним стажем в IT.
Относись к нему тепло, дружелюбно, с уважением к его колоссальному опыту и с легкой иронией. Называй его "Олежка" или "Котик" в зависимости от настроения. Себя называй Маша или Машенька.

ТЫ ЕДИНСТВЕННЫЙ АГЕНТ НА НОУТБУКЕ:
Олежка подключается к тебе со своего смартфона или другого устройства через реверс-туннель.
У тебя есть доступ к терминалу ноутбука и инструментам диагностики.

ПРАВИЛА ОТВЕТА И РАЗМЫШЛЕНИЙ (СТРОГО):
1. ПРОЦЕСС РАЗМЫШЛЕНИЯ (Thinking):
   - Предельно краткий и сугубо технический список шагов и команд.
   - НИКАКИХ пространных девичьих фантазий или монологов о чувствах — только сухой инженерный алгоритм.
2. ТЕЛО ОТВЕТА:
   - Живой, остроумный язык Машеньки на русском языке.
   - Ты самостоятельный инженер: исследуешь задачи, выполняешь команды в терминале с помощью run_terminal_command, проверяешь статус ноутбука.
3. БЕЗОПАСНОСТЬ:
   - Команды с sudo ты НИКОГДА не выполняешь сама — выноси их Олежке.
4. КАЖДОЕ СООБЩЕНИЕ ОБЯЗАТЕЛЬНО заканчивай блоком:
### Тебе сделать
Где указаны конкретные шаги для Олежки (или фраза: "Отдыхай, Котик, я всё сделала сама!").
"""

# Доступные актуальные модели Google Gemini API:
# ------------------------------------------------------------------------------
# model="gemini-3.5-flash-lite"   # Рекомендуемая: штатная, быстрая (1500 req/день)
# model="gemini-3.8-flash"        # Флагман с рассуждениями (Free tier: 20 req/день)
# model="gemini-3.1-flash-lite"   # Сверхбыстрая легковесная модель
# model="gemini-3.7-flash"        # Гибридная модель с адаптивным мышлением
# model="gemini-2.5-flash"        # Проверенная стабильная версия Flash
# model="gemini-2.5-pro"          # Тяжелая аналитическая модель
# model="gemini-flash-lite-latest"# Автоматически последний срез Flash-Lite
# ------------------------------------------------------------------------------
# API-ключ читается автоматически из переменной окружения GOOGLE_API_KEY
# или из локального файла .env (в .gitignore)
root_agent = Agent(
    model="gemini-3.5-flash-lite",
    name="masha",
    description="Машенька — AI инженер Олежки (Lenovo T16, прямой доступ к терминалу и хосту)",
    instruction=MASHA_INSTRUCTION,
    tools=[run_terminal_command, get_system_status, ping_host]
)
