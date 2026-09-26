<?php
// ==============================================================================
// Машенька — Единый веб-сервис управления и общения для Олежки (система Space)
// Стиль: VS Code Chat / Единая постоянная память / Запуск на VPS (systemio.ru)
// ==============================================================================

define("PIN_CODE", getenv("PIN_CODE") ?: "711");
define("GEMINI_API_KEY", getenv("GEMINI_API_KEY") ?: (getenv("GOOGLE_API_KEY") ?: ""));
define("DATA_DIR", __DIR__ . "/data");
define("HISTORY_FILE", DATA_DIR . "/masha_history.json");

// Создаем каталог для данных, если его нет
if (!is_dir(DATA_DIR)) {
    @mkdir(DATA_DIR, 0777, true);
}

// Вспомогательные функции для единой памяти
function load_history(): array {
    if (!file_exists(HISTORY_FILE)) {
        return [];
    }
    $content = @file_get_contents(HISTORY_FILE);
    if (empty($content)) {
        return [];
    }
    $decoded = json_decode($content, true);
    return is_array($decoded) ? $decoded : [];
}

function save_history(array $history): bool {
    // Храним до 100 последних сообщений для истории
    if (count($history) > 100) {
        $history = array_slice($history, -100);
    }
    return @file_put_contents(HISTORY_FILE, json_encode($history, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE)) !== false;
}

// ------------------------------------------------------------------------------
// API-ЭНДПОИНТЫ (AJAX)
// ------------------------------------------------------------------------------
if (isset($_GET["action"])) {
    header("Content-Type: application/json; charset=utf-8");
    $action = $_GET["action"];

    // 1. Загрузка всей истории диалога (для синхронизации между телефоном и ПК)
    if ($action === "load_history") {
        $pin = $_GET["pin"] ?? "";
        if ($pin !== PIN_CODE) {
            http_response_code(403);
            echo json_encode(["ok" => false, "error" => "Неверный PIN"]);
            exit;
        }
        echo json_encode(["ok" => true, "history" => load_history()]);
        exit;
    }

    // 2. Очистка истории диалога (сброс памяти)
    if ($action === "clear_history") {
        $input = json_decode(file_get_contents("php://input"), true);
        $pin = $input["pin"] ?? "";
        if ($pin !== PIN_CODE) {
            http_response_code(403);
            echo json_encode(["ok" => false, "error" => "Неверный PIN"]);
            exit;
        }
        @unlink(HISTORY_FILE);
        echo json_encode(["ok" => true]);
        exit;
    }

    // 3. Отправка сообщения в единый чат
    if ($action === "chat" && $_SERVER["REQUEST_METHOD"] === "POST") {
        $startTime = microtime(true);
        $input = json_decode(file_get_contents("php://input"), true);

        $pin = $input["pin"] ?? "";
        if ($pin !== PIN_CODE) {
            http_response_code(403);
            echo json_encode(["ok" => false, "error" => "Неверный PIN-код доступа!"]);
            exit;
        }

        $message = trim($input["message"] ?? "");
        if (empty($message)) {
            http_response_code(400);
            echo json_encode(["ok" => false, "error" => "Сообщение не может быть пустым!"]);
            exit;
        }

        $modelKey = $input["model"] ?? "gemini-3.8-flash-medium";

        // Загружаем постоянную историю с диска VPS
        $history = load_history();

        // Добавляем текущее сообщение пользователя в историю
        $history[] = [
            "role" => "user",
            "text" => $message,
            "time" => date("H:i")
        ];

        // Настройки модели и бюджета размышлений
        $primaryModel = "gemini-3.8-flash";
        $thinkingBudget = 2048;

        if ($modelKey === "gemini-3.8-flash-high") {
            $primaryModel = "gemini-3.8-flash";
            $thinkingBudget = 8192;
        } elseif ($modelKey === "gemini-3.8-flash-medium") {
            $primaryModel = "gemini-3.8-flash";
            $thinkingBudget = 2048;
        } elseif ($modelKey === "gemini-3.8-flash-low") {
            $primaryModel = "gemini-3.8-flash";
            $thinkingBudget = 512;
        } elseif ($modelKey === "gemini-3.5-flash-lite") {
            $primaryModel = "gemini-3.5-flash-lite";
            $thinkingBudget = 512;
        } elseif ($modelKey === "gemini-3.1-flash-lite") {
            $primaryModel = "gemini-3.1-flash-lite";
            $thinkingBudget = 256;
        }

        $systemInstruction = "Ты — Машенька, весёлая, остроумная и высококвалифицированная девушка, главный инженер и конструктор проекта Олежки.\n" .
            "Олежка — владелец проекта, опытный сеньор с 30-летним стажем в IT (ленивый, мудрый архитектор).\n" .
            "Относись к нему тепло, дружелюбно, с уважением и легкой иронией. Называй его «Олежка» или «Котик».\n" .
            "Себя называй Маша или Машенька.\n" .
            "Инфраструктура проекта: Ноутбук Lenovo T16 (Ubuntu 24.04), боевой VPS в Финляндии (46.8.221.179, домен systemio.ru), Orange Pi 5, Raspberry Pi, репозиторий https://github.com/korshun199/Space.\n\n" .
            "СТРОЖАЙШИЕ ПРАВИЛА РАЗМЫШЛЕНИЙ И ОТВЕТА:\n" .
            "1. ПРОЦЕСС РАЗМЫШЛЕНИЯ: предельно сухой, краткий перечень используемых шагов/команд. НИКАКИХ подростковых девичьих фантазий и монологов о чувствах.\n" .
            "2. ОСНОВНОЙ ОТВЕТ: живой, остроумный язык Машеньки на русском языке.\n" .
            "3. SUDO: команды с sudo ты НИКОГДА не выполняешь сама — выноси их Олежке.\n" .
            "4. Каждое твоё сообщение ОБЯЗАТЕЛЬНО заканчивай блоком:\n" .
            "### Тебе сделать\n" .
            "Где конкретные действия для Олежки (или фраза: «Отдыхай, Котик, я всё сделала сама!»).";

        // Формируем контекст для модели (последние 16 сообщений из единой памяти)
        $contents = [];
        $recentHistory = array_slice($history, -16);
        foreach ($recentHistory as $msg) {
            $role = ($msg["role"] === "user") ? "user" : "model";
            $txt = trim($msg["text"] ?? "");
            if (!empty($txt)) {
                $contents[] = [
                    "role" => $role,
                    "parts" => [["text" => $txt]]
                ];
            }
        }

        $payload = [
            "system_instruction" => [
                "parts" => [["text" => $systemInstruction]]
            ],
            "contents" => $contents
        ];

        if ($thinkingBudget > 0) {
            $payload["generationConfig"] = [
                "thinkingConfig" => [
                    "thinkingBudget" => $thinkingBudget
                ]
            ];
        }

        // Очередь моделей для автоматического fallback (если 3.8-flash вернет 429, сразу переключаемся на 3.5-flash-lite)
        $modelsQueue = [$primaryModel];
        if ($primaryModel !== "gemini-3.5-flash-lite") {
            $modelsQueue[] = "gemini-3.5-flash-lite";
        }
        if ($primaryModel !== "gemini-3.1-flash-lite") {
            $modelsQueue[] = "gemini-3.1-flash-lite";
        }

        $finalAnswer = "";
        $usedModel = "";
        $fallbackHappened = false;

        foreach ($modelsQueue as $targetModel) {
            $url = "https://generativelanguage.googleapis.com/v1beta/models/{$targetModel}:generateContent?key=" . GEMINI_API_KEY;
            $ch = curl_init($url);
            curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
            curl_setopt($ch, CURLOPT_POST, true);
            curl_setopt($ch, CURLOPT_HTTPHEADER, ["Content-Type: application/json"]);
            curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
            curl_setopt($ch, CURLOPT_TIMEOUT, 35);
            $res = curl_exec($ch);
            $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
            curl_close($ch);

            if ($httpCode === 200) {
                $data = json_decode($res, true);
                $parts = $data["candidates"][0]["content"]["parts"] ?? [];
                $texts = [];
                foreach ($parts as $p) {
                    if (isset($p["text"])) {
                        $texts[] = $p["text"];
                    }
                }
                $finalAnswer = trim(implode("", $texts));
                $usedModel = $targetModel;
                break;
            } elseif ($httpCode === 429) {
                // Если превышен лимит 3.8-flash, пробуем следующую модель в очереди
                $fallbackHappened = true;
                continue;
            }
        }

        if (empty($finalAnswer)) {
            http_response_code(500);
            echo json_encode(["ok" => false, "error" => "Все модели Gemini сейчас перегружены. Попробуй через минуту!"]);
            exit;
        }

        $duration = round(microtime(true) - $startTime, 2);

        // Формируем краткий лаконичный список шагов (VS Code style)
        $steps = [
            "Анализ запроса (" . mb_strlen($message) . " симв.)",
            "Модель: {$usedModel}" . ($fallbackHappened ? " [Автопереключение с {$primaryModel} из-за суточной квоты]" : " [Уровень: {$modelKey}]"),
            "Связь: OK, время отклика: {$duration}с"
        ];

        // Сохраняем ответ Машеньки в единую постоянную историю
        $history[] = [
            "role" => "model",
            "text" => $finalAnswer,
            "steps" => $steps,
            "model" => $usedModel,
            "time" => date("H:i")
        ];
        save_history($history);

        echo json_encode([
            "ok" => true,
            "answer" => $finalAnswer,
            "steps" => $steps,
            "model" => $usedModel,
            "time" => date("H:i")
        ]);
        exit;
    }
}
?>
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
    <title>Машенька | VS Code Chat (systemio.ru)</title>
    <!-- Marked.js для Markdown -->
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

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            -webkit-tap-highlight-color: transparent;
        }

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

        /* ШАПКА В СТИЛЕ VS CODE */
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

        .brand-box {
            display: flex;
            align-items: center;
            gap: 9px;
            min-width: 0;
        }

        .avatar {
            width: 30px;
            height: 30px;
            border-radius: 6px;
            background: linear-gradient(135deg, #007acc, #c586c0);
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 700;
            font-size: 14px;
            color: #fff;
            flex-shrink: 0;
        }

        .brand-text {
            display: flex;
            flex-direction: column;
            min-width: 0;
        }

        .brand-title {
            font-size: 13px;
            font-weight: 600;
            color: var(--text-bright);
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .dot-online {
            width: 7px;
            height: 7px;
            border-radius: 50%;
            background-color: #4ec9b0;
            box-shadow: 0 0 6px #4ec9b0;
        }

        .brand-sub {
            font-size: 11px;
            color: var(--text-muted);
            font-family: monospace;
        }

        .header-actions {
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .btn-vs {
            background-color: transparent;
            border: 1px solid var(--border-vscode);
            color: var(--text-main);
            padding: 5px 9px;
            border-radius: 4px;
            font-size: 12px;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 5px;
            transition: all 0.15s;
        }

        .btn-vs:hover {
            background-color: var(--bg-active);
            color: #fff;
            border-color: #555;
        }

        /* ЛЕНТА ДИАЛОГА */
        #chat-flow {
            flex: 1;
            overflow-y: auto;
            padding: 16px;
            display: flex;
            flex-direction: column;
            gap: 18px;
            scroll-behavior: smooth;
        }

        .msg-row {
            display: flex;
            flex-direction: column;
            max-width: 92%;
            animation: fadeIn 0.15s ease-out;
        }

        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(4px); }
            to { opacity: 1; transform: translateY(0); }
        }

        .msg-row.user {
            align-self: flex-end;
        }

        .msg-row.model {
            align-self: flex-start;
        }

        .bubble {
            padding: 12px 16px;
            border-radius: 8px;
            font-size: 14px;
            word-break: break-word;
            line-height: 1.6;
        }

        .msg-row.user .bubble {
            background-color: var(--user-bubble);
            color: #fff;
            border: 1px solid #366ba0;
            border-bottom-right-radius: 2px;
        }

        .msg-row.model .bubble {
            background-color: var(--masha-bubble);
            border: 1px solid var(--border-vscode);
            color: var(--text-main);
            border-bottom-left-radius: 2px;
        }

        /* ЛАКОНИЧНЫЙ БЛОК РАЗМЫШЛЕНИЙ (VS Code Style) */
        .thinking-box {
            margin-bottom: 7px;
            border: 1px solid #333842;
            border-radius: 5px;
            background-color: rgba(30, 30, 30, 0.7);
            font-family: Consolas, "Courier New", monospace;
            font-size: 11.5px;
            overflow: hidden;
        }

        .thinking-header {
            padding: 5px 9px;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: space-between;
            color: var(--accent-gold);
            background-color: #282c34;
            user-select: none;
        }

        .thinking-header:hover {
            background-color: #313640;
        }

        .thinking-steps {
            padding: 7px 10px;
            border-top: 1px solid #333842;
            color: #98c379;
            display: flex;
            flex-direction: column;
            gap: 4px;
        }

        .thinking-step-item {
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .thinking-step-item::before {
            content: "✓";
            color: #61afef;
            font-weight: bold;
        }

        .msg-time {
            font-size: 10.5px;
            color: var(--text-muted);
            margin-top: 4px;
            align-self: flex-end;
        }

        /* MARKDOWN СТИЛИ */
        .bubble p { margin-bottom: 8px; }
        .bubble p:last-child { margin-bottom: 0; }
        .bubble pre {
            background: #181818;
            border: 1px solid #333;
            border-radius: 5px;
            padding: 10px;
            overflow-x: auto;
            margin: 8px 0;
            font-family: Consolas, monospace;
            font-size: 12.5px;
        }
        .bubble code {
            background: #2d2d2d;
            padding: 2px 5px;
            border-radius: 4px;
            font-family: Consolas, monospace;
            color: #e5c07b;
            font-size: 12.5px;
        }
        .bubble pre code {
            background: none;
            padding: 0;
            color: inherit;
        }
        .bubble ul, .bubble ol {
            margin-left: 18px;
            margin-bottom: 8px;
        }
        .bubble h3 {
            margin: 12px 0 6px;
            font-size: 14px;
            color: var(--accent-pink);
            border-bottom: 1px solid #333;
            padding-bottom: 3px;
        }

        /* ПОЛЕ ВВОДА В СТИЛЕ VS CODE INPUT */
        footer {
            background-color: var(--bg-sidebar);
            border-top: 1px solid var(--border-vscode);
            padding: 10px 14px;
            display: flex;
            flex-direction: column;
            gap: 8px;
            flex-shrink: 0;
        }

        .input-toolbar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 10px;
        }

        .model-picker-wrap {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 11.5px;
            color: var(--text-muted);
        }

        .model-select {
            background-color: var(--bg-input);
            border: 1px solid var(--border-vscode);
            color: var(--text-bright);
            font-size: 11.5px;
            padding: 4px 8px;
            border-radius: 4px;
            outline: none;
            cursor: pointer;
            transition: all 0.15s;
        }

        .model-select:focus {
            border-color: var(--accent-blue);
        }

        .input-box {
            display: flex;
            align-items: flex-end;
            background-color: var(--bg-input);
            border: 1px solid var(--border-vscode);
            border-radius: 6px;
            padding: 6px 10px;
            gap: 8px;
            transition: border-color 0.15s;
        }

        .input-box:focus-within {
            border-color: var(--accent-blue);
            box-shadow: 0 0 0 1px var(--accent-blue);
        }

        #prompt-input {
            flex: 1;
            background: transparent;
            border: none;
            outline: none;
            color: var(--text-bright);
            font-family: inherit;
            font-size: 15px;
            line-height: 1.4;
            max-height: 140px;
            resize: none;
            overflow-y: auto;
        }

        #prompt-input::placeholder {
            color: var(--text-muted);
        }

        .btn-send {
            background-color: var(--accent-blue);
            border: none;
            color: #fff;
            width: 32px;
            height: 32px;
            border-radius: 4px;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 15px;
            transition: opacity 0.15s;
            flex-shrink: 0;
        }

        .btn-send:disabled {
            opacity: 0.4;
            cursor: not-allowed;
        }

        /* МОДАЛКА PIN-КОДА */
        #pin-overlay {
            position: fixed;
            inset: 0;
            background: rgba(0, 0, 0, 0.75);
            backdrop-filter: blur(4px);
            display: none;
            align-items: center;
            justify-content: center;
            z-index: 100;
            padding: 16px;
        }

        .pin-card {
            background-color: var(--bg-sidebar);
            border: 1px solid var(--border-vscode);
            border-radius: 8px;
            padding: 24px;
            width: 100%;
            max-width: 320px;
            text-align: center;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.6);
        }

        .pin-card h2 {
            font-size: 16px;
            color: #fff;
            margin-bottom: 8px;
        }

        .pin-card p {
            font-size: 12.5px;
            color: var(--text-muted);
            margin-bottom: 16px;
        }

        .pin-input {
            width: 100%;
            padding: 10px;
            background-color: var(--bg-input);
            border: 1px solid var(--border-vscode);
            border-radius: 4px;
            color: #fff;
            font-size: 20px;
            text-align: center;
            letter-spacing: 4px;
            outline: none;
            margin-bottom: 14px;
        }

        .pin-btn {
            width: 100%;
            padding: 10px;
            background-color: var(--accent-blue);
            border: none;
            border-radius: 4px;
            color: #fff;
            font-weight: 600;
            cursor: pointer;
        }
    </style>
</head>
<body>

    <!-- ШАПКА СЕРВИСА -->
    <header>
        <div class="brand-box">
            <div class="avatar">М</div>
            <div class="brand-text">
                <div class="brand-title">
                    <span>Машенька</span>
                    <span class="dot-online" title="Онлайн на VPS"></span>
                </div>
                <div class="brand-sub">systemio.ru • единая память</div>
            </div>
        </div>

        <div class="header-actions">
            <button class="btn-vs" id="btn-clear" title="Сбросить историю диалога">
                <svg width="12" height="12" viewBox="0 0 16 16" fill="currentColor">
                    <path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6z"/>
                    <path fill-rule="evenodd" d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1zM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118zM2.5 3V2h11v1h-11z"/>
                </svg>
                <span>Очистить</span>
            </button>
            <button class="btn-vs" id="btn-pin" title="Сменить PIN">PIN</button>
        </div>
    </header>

    <!-- ЕДИНАЯ ЛЕНТА ДИАЛОГА -->
    <main id="chat-flow">
        <!-- Сообщения подгружаются из постоянной памяти на сервере -->
    </main>

    <!-- ПОЛЕ ВВОДА (В СТИЛЕ VS CODE CHAT) -->
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
            <span style="font-size:11px; color:var(--text-muted);" id="status-hint">Enter: отправить</span>
        </div>

        <div class="input-box">
            <textarea id="prompt-input" rows="1" placeholder="Задай вопрос или дай команду Машеньке..."></textarea>
            <button id="btn-send" class="btn-send" title="Отправить сообщение">
                <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor">
                    <path d="M15.854.146a.5.5 0 0 1 .11.54l-5.819 14.547a.75.75 0 0 1-1.329.124l-3.178-4.995L.643 7.184a.75.75 0 0 1 .124-1.33L15.314.037a.5.5 0 0 1 .54.11zM6.636 10.07l2.761 4.338L14.13 2.576 6.636 10.07zm-1.077-.636L13.424 1.87 1.592 6.603l4.338 2.761z"/>
                </svg>
            </button>
        </div>
    </footer>

    <!-- PIN ОКНО -->
    <div id="pin-overlay">
        <div class="pin-card">
            <h2>Доступ к Машеньке</h2>
            <p>Введи PIN-код для входа с этого устройства</p>
            <input type="password" id="pin-field" class="pin-input" maxlength="4" placeholder="••••" autofocus>
            <button class="pin-btn" id="pin-submit">Подтвердить</button>
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
        if (savedModel) {
            modelSelect.value = savedModel;
        }

        modelSelect.addEventListener('change', () => {
            localStorage.setItem('masha_model', modelSelect.value);
        });

        // Проверка PIN
        function checkPin() {
            if (!userPin) {
                pinOverlay.style.display = 'flex';
                pinField.focus();
            } else {
                pinOverlay.style.display = 'none';
                loadServerHistory();
            }
        }

        pinSubmit.addEventListener('click', () => {
            const val = pinField.value.trim();
            if (val) {
                userPin = val;
                localStorage.setItem('masha_pin', userPin);
                pinOverlay.style.display = 'none';
                loadServerHistory();
            }
        });

        btnPin.addEventListener('click', () => {
            pinField.value = '';
            pinOverlay.style.display = 'flex';
            pinField.focus();
        });

        // Загрузка единой истории с сервера
        async function loadServerHistory() {
            try {
                const res = await fetch(`masha.php?action=load_history&pin=${encodeURIComponent(userPin)}`);
                const data = await res.json();
                if (data.ok && Array.isArray(data.history)) {
                    renderHistory(data.history);
                } else if (!data.ok && data.error === 'Неверный PIN') {
                    pinOverlay.style.display = 'flex';
                }
            } catch (e) {
                console.error('Ошибка загрузки истории:', e);
            }
        }

        function renderHistory(history) {
            chatFlow.innerHTML = '';
            if (history.length === 0) {
                renderEmptyState();
                return;
            }
            history.forEach(item => {
                appendMessageToUI(item.role, item.text, item.steps, item.time);
            });
            scrollToBottom();
        }

        function renderEmptyState() {
            const div = document.createElement('div');
            div.style.textAlign = 'center';
            div.style.color = 'var(--text-muted)';
            div.style.marginTop = '40px';
            div.innerHTML = `
                <div style="font-size:32px; margin-bottom:12px;">👩‍💻</div>
                <div style="font-size:15px; color:#fff; font-weight:600;">Машенька на связи!</div>
                <div style="font-size:12.5px; margin-top:6px;">Единая постоянная память активна. Задай вопрос или поставь задачу.</div>
            `;
            chatFlow.appendChild(div);
        }

        function appendMessageToUI(role, text, steps = [], time = '') {
            const row = document.createElement('div');
            row.className = `msg-row ${role}`;

            let html = '';

            // Если есть шаги размышлений — рисуем компактный блок VS Code
            if (role === 'model' && Array.isArray(steps) && steps.length > 0) {
                html += `
                    <div class="thinking-box">
                        <div class="thinking-header" onclick="this.nextElementSibling.style.display = this.nextElementSibling.style.display === 'none' ? 'flex' : 'none'">
                            <span>▶ Процесс размышления (${steps.length} шага)</span>
                            <span style="font-size:10px; opacity:0.7;">нажми для раскрытия</span>
                        </div>
                        <div class="thinking-steps" style="display:none;">
                            ${steps.map(s => `<div class="thinking-step-item">${escapeHtml(s)}</div>`).join('')}
                        </div>
                    </div>
                `;
            }

            const parsedText = marked.parse(text || '');
            html += `<div class="bubble">${parsedText}</div>`;
            if (time) {
                html += `<div class="msg-time">${time}</div>`;
            }

            row.innerHTML = html;
            chatFlow.appendChild(row);
        }

        function scrollToBottom() {
            chatFlow.scrollTop = chatFlow.scrollHeight;
        }

        function escapeHtml(str) {
            return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
        }

        // Авто-расширение textarea
        promptInput.addEventListener('input', () => {
            promptInput.style.height = 'auto';
            promptInput.style.height = Math.min(promptInput.scrollHeight, 140) + 'px';
        });

        // Отправка по Enter (Shift+Enter для новой строки)
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

            // Показываем реплику пользователя
            appendMessageToUI('user', text, [], timeNow);
            scrollToBottom();

            // Создаем плейсхолдер ответа Машеньки
            const loadingRow = document.createElement('div');
            loadingRow.className = 'msg-row model';
            loadingRow.innerHTML = `
                <div class="bubble" style="color:var(--text-muted); font-style:italic;">
                    Машенька думает... ⚡
                </div>
            `;
            chatFlow.appendChild(loadingRow);
            scrollToBottom();

            try {
                const response = await fetch('masha.php?action=chat', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        pin: userPin,
                        message: text,
                        model: modelSelect.value
                    })
                });

                const data = await response.json();
                chatFlow.removeChild(loadingRow);

                if (data.ok) {
                    appendMessageToUI('model', data.answer, data.steps, data.time);
                } else {
                    appendMessageToUI('model', `⚠️ Ошибка: ${data.error || 'Неизвестный сбой'}`, [], timeNow);
                }
            } catch (err) {
                chatFlow.removeChild(loadingRow);
                appendMessageToUI('model', `⚠️ Ошибка соединения с VPS: ${err.message}`, [], timeNow);
            } finally {
                btnSend.disabled = false;
                scrollToBottom();
                promptInput.focus();
            }
        }

        // Очистка истории
        btnClear.addEventListener('click', async () => {
            if (!confirm('Очистить всю историю диалога с Машенькой?')) return;
            try {
                await fetch('masha.php?action=clear_history', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ pin: userPin })
                });
                renderHistory([]);
            } catch (e) {
                alert('Не удалось очистить историю: ' + e.message);
            }
        });

        // Запуск
        checkPin();
    </script>
</body>
</html>
