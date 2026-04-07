# AGENTS.md — composeApp Module

## Назначение

composeApp — entry point для WASM приложения. Собирает зависимости, инициализирует storage, storeFactory и root component. Содержит decompose-компоненты и Compose UI composition.

## Команды

```bash
./gradlew :composeApp:check              # Tests + lint + detekt для модуля
./gradlew :composeApp:ktlintFormat       # Auto-fix style
./gradlew :composeApp:compileKotlinWasmJs # Проверка компиляции
```

## Архитектура

**Module Type**: App entry + composition

**Pattern**: Decompose + MVIKotlin stores

**Module Dependencies**:
```
composeApp
├── feature:chat
├── feature:settings
├── core:storage
├── core:network
└── core:model
```

## Ключевые файлы

| Файл | Назначение |
|------|------------|
| `src/wasmJsMain/kotlin/main.kt` | Entry point, DI wiring, ComposeViewport |
| `src/wasmJsMain/kotlin/root/DefaultRootComponent.kt` | Root component, slot navigation |
| `src/wasmJsMain/kotlin/root/RootContent.kt` | UI composition и показ SettingsDialog |
| `src/wasmJsMain/kotlin/chat/DefaultChatComponent.kt` | Chat component, store binding |
| `src/wasmJsMain/kotlin/settings/DefaultSettingsComponent.kt` | Settings component, store binding |

## Data Flow

```
main.kt → DefaultRootComponent → RootContent
                      ├── ChatContent (feature:chat)
                      └── SettingsDialog (feature:settings)
```

## Integration Notes

- `LocalStorageService` создается в `main.kt` и передается в `DefaultRootComponent`
- `RootContent` синхронизирует настройки: при изменении `SettingsState` отправляет `ChatIntent.UpdateSettings`
- Settings отображаются через decompose slot

## Критичные правила

- **Detekt**: БЕЗ `@Suppress` (исключения: только тесты)
- **No comments**: Без inline-комментариев. KDoc только для public API
- **Composition**: Все UI-корни инициализируются в composeApp

## Workflow

1. Изменил UI composition → `./gradlew :composeApp:check`
2. Style issues → `./gradlew :composeApp:ktlintFormat`
3. Проверил проект → `./gradlew check`
4. Commit
