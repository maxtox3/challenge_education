# AGENTS.md

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

## Lint Commands

```bash
./gradlew ktlintCheck      # Проверка стиля кода
./gradlew ktlintFormat     # Автоисправление стиля
./gradlew detekt           # Статический анализ
./gradlew check            # Все проверки (tests + ktlint + detekt)
```

## Code Conventions

- Kotlin 2.3.20
- Compose Multiplatform 1.10.2
- Kotlinx coroutines 1.10.2
- Kotlinx serialization 1.10.0
- Ktor client 3.4.1
- JVM target: 17

## Known Issues

- Kotlin/WASM compiler cache issues — используйте `./gradlew clean` при внутренних ошибках компилятора
- Ktor SSE может буферизировать на WASM — если streaming не работает в реальном времени, может потребоваться JS interop с `fetch()` + `ReadableStream`
