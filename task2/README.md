# Z.ai Chat

Kotlin Multiplatform приложение с Compose for Web (WASM) для чата с Z.AI API.

## Запуск

```bash
./gradlew wasmJsRun
```

Откроется на http://localhost:8080

## Структура

```
composeApp/src/wasmJsMain/kotlin/
├── App.kt           - UI чата
├── ChatClient.kt    - HTTP клиент для Z.AI API
├── main.kt          - Точка входа
└── model/
    ├── ChatMessage.kt  - Модель сообщения для UI
    └── ZAiRequest.kt   - Модели для API (serialization)
```

## Как работает

1. Пользователь вводит сообщение
2. ChatClient отправляет POST запрос на `https://api.z.ai/api/coding/paas/v4/chat/completions`
3. Используется модель `glm-5`
4. Ответ отображается в чате
