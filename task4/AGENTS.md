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
- **No comments**: Код должен быть self-documenting. Только `@file:OptIn`
- **Принципы**: SOLID, KISS, DRY, YAGNI — строго

## Архитектура

**MVI**: `State` (immutable data class) + `Intent` (sealed class) + `SideEffect` (одноразовые события)

**Module isolation**:
- `feature/*` → зависят только от `core/*`
- `feature/*` → НЕ зависят друг от друга
- `core/*` → общие модели, сеть, UI components

```
composeApp/     # Entry point
feature/        # chat, settings, metrics, reasoning
core/           # model, network, ui (shared)
```

## Тестирование

| Тип | Файлы | Назначение |
|-----|-------|------------|
| Baseline | `baseline/*BaselineTest.kt` | Characterization тесты — фиксируют текущее поведение |
| Unit | `*ViewModelTest.kt`, `*RepositoryTest.kt` | Логика + state transitions |
| UI | `ui/*UiRealTest.kt` | Compose interactions |

## Ключевые файлы (изучи перед изменениями)

1. `feature/chat/.../ChatViewModel.kt` — MVI паттерн
2. `feature/chat/.../ChatRepository.kt` — Repository + network layer
3. `feature/chat/.../ui/App.kt` — Compose UI composition
4. `core/network/.../ChatClient.kt` — HTTP client

## Modules Documentation

Подробная документация для каждого модуля:

- **[feature/chat/AGENTS.md](./feature/chat/AGENTS.md)** — Chat module (MVI, streaming, UI components)

## Workflow

1. `./gradlew check` → все проверки пройдены?
2. `./gradlew ktlintFormat` → если style issues
3. Commit

## Контекст заданий

Задания выполняются как доработка текущего проекта с записью видео процесса.
Каждое задание = новый функционал в проекте + демо на видео.

### Модели для сравнения (День 5)
| Уровень | ID | Название |
|---------|-----|----------|
| Слабая | glm4.5-air | GLM-4.5 Air |
| Средняя | glm4.7 | GLM-4.7 |
| Сильная | glm5 | GLM-5 |
