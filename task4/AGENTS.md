# AGENTS.md

## Project Overview

Z.ai Chat — Kotlin Multiplatform приложение с Compose for Web (WASM) для чата с Z.AI API. Поддерживает SSE streaming для отображения ответов AI в реальном времени.

## Build Commands

```bash
./gradlew build              # Сборка проекта
./gradlew clean              # Очистка build/
./gradlew wasmJsRun          # Запуск dev-сервера (http://localhost:8080)
```

## Test Commands

```bash
./gradlew check                          # Все проверки
./gradlew :composeApp:wasmJsBrowserTest  # Тесты в браузере
```

## Project Structure

```
composeApp/src/wasmJsMain/kotlin/
├── App.kt              # UI чата (Compose)
├── ChatClient.kt       # HTTP клиент (Ktor) + SSE streaming
├── ChatRepository.kt   # Репозиторий для работы с API
├── ChatViewModel.kt    # ViewModel для управления состоянием
├── main.kt             # Точка входа
├── SettingsState.kt    # Состояние настроек (API key)
├── model/
│   ├── ChatMessage.kt  # Модель сообщения для UI
│   ├── ZAiRequest.kt   # Модели для API (serialization)
│   └── StreamChunk.kt  # Sealed class для SSE событий
└── ui/components/
    └── MessageBubble.kt # Компонент для отображения сообщений
```

## Code Conventions

- Kotlin 2.0.21
- Compose Multiplatform 1.7.1
- Kotlinx serialization для JSON
- Ktor client 3.0.0 для HTTP
- JVM target: 17

## API Configuration

- Endpoint: `https://api.z.ai/api/coding/paas/v4/chat/completions`
- Model: `glm-5`
- Dev server port: **8080** (обязательно для CORS)

## SSE Streaming

Приложение использует Server-Sent Events (SSE) для streaming ответов:

- **StreamChunk** — sealed class с типами: `Content`, `Reasoning`, `Done`
- **channelFlow** — используется вместо `flow` для безопасной многопоточной эмиссии
- **Reasoning content** — отображается автоматически с меньшим шрифтом (12sp), без клика для раскрытия

### Формат SSE от Z.AI

```
data: {"choices":[{"delta":{"content":"текст"}}]}
data: {"choices":[{"delta":{"reasoning_content":"рассуждения"}}]}
data: [DONE]
```

## Lint Commands

```bash
./gradlew ktlintCheck      # Проверка стиля кода
./gradlew ktlintFormat     # Автоисправление стиля
./gradlew detekt           # Статический анализ
./gradlew check            # Все проверки (tests + ktlint + detekt)
```

Pre-commit hook автоматически запускает проверки перед коммитом.

## Known Issues

- Kotlin/WASM compiler cache issues — используйте `./gradlew clean` при внутренних ошибках компилятора
- Ktor SSE может буферизировать на WASM — если streaming не работает в реальном времени, может потребоваться JS interop с `fetch()` + `ReadableStream`
