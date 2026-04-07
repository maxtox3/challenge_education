# AGENTS.md

## Команды

```bash
./gradlew wasmJsRun        # Dev server :8080
./gradlew check            # Tests + lint + detekt
./gradlew ktlintFormat     # Auto-fix style
./gradlew :composeApp:test --tests "Pattern*"  # Run specific tests
```

## Критичные правила

- **Detekt**: БЕЗ `@Suppress`. Исключения: тесты, `AppColors.kt` (обосновано)
- **No comments**: Без inline-комментариев. Допустим только KDoc для public API и `@file:OptIn`
- **Принципы**: SOLID, KISS, DRY, YAGNI — строго

## Архитектура

**MVI**: `State` (immutable data class) + `Intent` (sealed class) + `SideEffect` (одноразовые события)

**Module isolation**:
- `feature/*` → зависят только от `core/*`
- `feature/*` → НЕ зависят друг от друга
- `core/*` → общие модели, сеть, UI components
- Текущее исключение: `feature:chat` использует `feature:settings` для `ApiSettings`

**Current implementation status**:
- ✅ SSE streaming infrastructure in `core/network`
- ✅ Streaming интегрирован в ChatStoreFactory (для ZAI provider)
- ✅ localStorage persistence реализован в `core/storage` и используется в chat/settings

```
composeApp/     # Entry point
feature/        # chat, settings
core/           # model, network, ui (shared)
```

## Тестирование

| Тип      | Файлы                                     | Назначение                                           |
|----------|-------------------------------------------|------------------------------------------------------|
| Baseline | `baseline/*BaselineTest.kt`               | Characterization тесты — фиксируют текущее поведение |
| Unit     | `*ViewModelTest.kt`, `*RepositoryTest.kt` | Логика + state transitions                           |
| UI       | `ui/*UiRealTest.kt`                       | Compose interactions                                 |

## Ключевые файлы (изучи перед изменениями)

1. `feature/chat/.../ChatViewModel.kt` — MVI паттерн
2. `feature/chat/.../ChatRepository.kt` — Repository + network layer
3. `composeApp/.../App.kt` — Compose UI composition
4. `core/network/.../ChatClient.kt` — HTTP client

## Modules Documentation

Подробная документация для каждого модуля:

### Feature Modules

- **[feature/chat/AGENTS.md](./feature/chat/AGENTS.md)** — Chat module (MVI, streaming infrastructure, UI components)
- **[feature/settings/AGENTS.md](./feature/settings/AGENTS.md)** — Settings module (MVI, API settings, ModelSelector)

### Core Modules

- **[core/model/AGENTS.md](./core/model/AGENTS.md)** — Model module (data classes, enums, API models)
- **[core/network/AGENTS.md](./core/network/AGENTS.md)** — Network module (HTTP client, SSE streaming, error handling)
- **[core/storage/AGENTS.md](./core/storage/AGENTS.md)** — Storage module (localStorage persistence, chat history, settings)

## Workflow

1. `./gradlew check` → все проверки пройдены?
2. `./gradlew ktlintFormat` → если style issues
3. Commit

## Контекст заданий

Задания выполняются как доработка текущего проекта с записью видео процесса.
Каждое задание = новый функционал в проекте + демо на видео (демо записывает пользователь самостоятельно).

### Модели для сравнения (День 5)
| Уровень  | ID         | Название    |
|----------|------------|-------------|
| Слабая   | glm4.5-air | GLM-4.5 Air |
| Средняя  | glm4.7     | GLM-4.7     |
| Сильная  | glm5       | GLM-5       |
