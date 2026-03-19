# AGENTS.md

## Project Overview

Z.ai Chat — Kotlin Multiplatform приложение с Compose for Web (WASM) для чата с Z.AI API.

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
├── ChatClient.kt       # HTTP клиент (Ktor)
├── main.kt             # Точка входа
└── model/
    ├── ChatMessage.kt  # Модель сообщения для UI
    └── ZAiRequest.kt   # Модели для API (serialization)
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

## Lint Commands

```bash
./gradlew ktlintCheck      # Проверка стиля кода
./gradlew ktlintFormat     # Автоисправление стиля
./gradlew detekt           # Статический анализ
./gradlew check            # Все проверки (tests + ktlint + detekt)
```

Pre-commit hook автоматически запускает проверки перед коммитом.
