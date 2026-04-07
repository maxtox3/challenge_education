# AGENTS.md — Chat Module

## Назначение

Chat feature — главный модуль приложения. Реализует UI и бизнес-логику чата с AI моделями через MVI архитектуру с использованием MVIKotlin framework.

## Команды

```bash
./gradlew :feature:chat:check              # Tests + lint + detekt для модуля
./gradlew :feature:chat:ktlintFormat       # Auto-fix style
./gradlew :feature:chat:compileKotlinWasmJs # Проверка компиляции
```

## Архитектура

**MVI Pattern (MVIKotlin Framework)**:
- `ChatState` — immutable data class (единственный источник истины)
- `ChatIntent` — sealed class пользовательских действий
- `ChatLabel` — одноразовые события (аналог SideEffect в MVIKotlin)
- `ChatStore` — Store interface (MVIKotlin)
- `ChatStoreFactory` — Factory для создания Store с Executor и Reducer

**Data Flow**:
```
UI Event → Intent → Executor → Repository → Msg (internal) → Reducer → State Update → UI Render
                                    ↓
                               Label (one-time events)
```

**Module Dependencies**:
```
feature:chat
├── core:model        (ChatMessage, StreamChunk)
├── core:network      (ChatClient)
├── core:storage      (StorageService)
└── feature:settings  (ApiSettings)
```

**Note**: Metrics и Reasoning features запланированы, но ещё не реализованы.

## Ключевые файлы

### Business Logic
| Файл | Назначение |
|------|------------|
| `store/ChatStoreFactory.kt` | MVIKotlin Store factory, executor, reducer |
| `store/ChatStore.kt` | Store interface definition |
| `store/ChatStoreProvider.kt` | Store provider для DI |
| `ChatRepository.kt` | API calls, streaming infrastructure |
| `MessageHandler.kt` | Message validation, formatting utilities |
| `ChatState.kt` | Immutable state definition |
| `ChatIntent.kt` | Sealed class of user actions |
| `ChatLabel.kt` | One-time events (MVIKotlin Labels) |
| `ChatSideEffect.kt` | Legacy side effects (will be replaced with Labels) |

### UI Components
| Файл | Назначение |
|------|------------|
| `ui/components/MessageBubble.kt` | Message rendering (user/AI) |
| `ui/components/ChatInput.kt` | Input field + send button |
| `ui/components/MessageBubbleTags.kt` | Test tags for MessageBubble |
| `ui/components/ChatInputTags.kt` | Test tags for ChatInput |
| `ui/icons/*` | Icon composables (Settings, Close, Delete) |

**Note**: Main composable (App.kt) находится в composeApp модуле, не в feature:chat.

## MVI Components

### ChatState
```kotlin
data class ChatState(
    val inputText: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showSettings: Boolean = false,
    val settings: ApiSettings = ApiSettings(),
)
```

### ChatIntent
- `UpdateInputText(text: String)` — обновить текст ввода
- `SendMessage` — отправить сообщение
- `ClearChat` — очистить историю
- `UpdateSettings(settings: ApiSettings)` — обновить настройки API
- `ToggleSettings(show: Boolean)` — показать/скрыть настройки
- `SetError(message: String?)` — установить ошибку
- `ClearError` — очистить ошибку
- `SetLoading(loading: Boolean)` — установить состояние загрузки

### ChatLabel (MVIKotlin Labels)
- `ScrollToBottom` — прокрутить к последнему сообщению
- `ShowToast(message: String)` — показать toast уведомление
- `HideKeyboard` — скрыть клавиатуру

## Repository Layer

### ChatRepository Interface
```kotlin
interface ChatRepository {
    // Non-streaming API (CURRENTLY USED)
    suspend fun sendMessage(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings
    ): SendMessageResult
    
    // Streaming API (USED for ZAI provider)
    fun sendMessageStreaming(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings
    ): Flow<StreamChunk>
}
```

### SendMessageResult
- `Success(response: ChatMessage)` — успешный ответ
- `Error(message: String)` — ошибка

**Implementation Status**:
- ✅ `sendMessage()` — используется в ChatStoreFactory
- ✅ `sendMessageStreaming()` — используется в ChatStoreFactory для ZAI provider

## Implementation Details

### ChatStoreFactory Architecture

**Executor** (CoroutineExecutorFactory):
- Обрабатывает Intents
- Вызывает Repository методы
- Диспатчит внутренние Msg для Reducer
- Публикует Labels для одноразовых событий

**Internal Messages (Msg)**:
- `InputTextChanged` — обновление текста ввода
- `MessagesUpdated` — обновление списка сообщений
- `LoadingChanged` — изменение состояния загрузки
- `ErrorChanged` — изменение ошибки
- `SettingsToggled` — переключение настроек
- `SettingsUpdated` — обновление настроек

**Reducer** (MessageReducer):
- Immutable state updates
- Pattern matching on Msg types

### Current Flow (Non-Streaming, OpenRouter)
1. User sends message → `SendMessage` intent
2. Executor creates user message, adds to state
3. Executor calls `repository.sendMessage()` (non-streaming)
4. On success: adds assistant message to state
5. On error: sets error message, publishes ShowToast label

### Current Flow (Streaming, ZAI)
1. User sends message → `SendMessage` intent
2. Executor creates user message, adds to state
3. Executor calls `repository.sendMessageStreaming()`
4. Streaming chunks update `streamingContent/streamingReasoning`
5. On Done: final assistant message added to state + history persisted

## Error Handling

- Network errors → `errorMessage` в state + `ShowToast` label
- Loading states → `isLoading` в state
- Exception handling → CancellationException propagated, others converted to error state

## Критичные правила

- **Detekt**: БЕЗ `@Suppress` (исключения: только тесты)
- **No comments**: Без inline-комментариев. KDoc только для public API
- **KDoc**: Документация только для public API
- **State updates**: Только через Reducer (pattern matching на Msg)
- **Intents**: Обрабатываются в Executor (coroutineExecutorFactory)
- **Labels**: Одноразовые события, не state

## Workflow

1. Изменил код → `./gradlew :feature:chat:check`
2. Style issues → `./gradlew :feature:chat:ktlintFormat`
3. Проверил зависимые модули → `./gradlew check`
4. Commit

## Частые задачи

### Добавить новый Intent
1. Добавить case в `ChatIntent.kt`
2. Добавить handler в `ChatStoreFactory.executorFactory` (onIntent)
3. Добавить Msg в sealed interface `Msg` (если нужно новое поле)
4. Добавить case в `MessageReducer` (если нужно)
5. Вызвать из UI через Store

### Добавить новый UI компонент
1. Создать файл в `ui/components/`
2. Добавить test tags в отдельный файл `*Tags.kt`
3. Использовать в главном composable (composeApp модуль)

### Изменить API interaction
1. Изменить `ChatRepository.kt`
2. Обновить `ChatStoreFactory.kt` executor
3. Добавить обработку в reducer (если нужно)
4. Обновить tests (когда появятся)

## Feature Status

### Streaming Integration
**Status**: Implemented for ZAI provider

**What exists**:
- `ChatClient.sendMessageStreaming()` в core/network
- `ChatRepository.sendMessageStreaming()` используется в ChatStoreFactory для ZAI
- `StreamChunk` sealed class в core/model

**What needs to be done**:
- Add cancel streaming support
- Улучшить отображение streaming reasoning (если нужно)

### LocalStorage Persistence
**Status**: Implemented

**What exists**:
- Chat history сохраняется через `StorageService`
- Settings persistence реализована в settings module

### Metrics Tracking
**Status**: Not implemented

**What needs to be done**:
- Add `metrics` field to ChatState
- Add `showMetrics` state flag
- Track API call metrics (latency, tokens, etc.)
- Create MetricsDialog component
- Add metrics display UI

### Reasoning Comparison
**Status**: Not implemented

**What needs to be done**:
- Add `reasoningComparison`, `showReasoning`, `isReasoningLoading` fields to ChatState
- Implement multi-model comparison logic
- Create ReasoningDialog component
- Add comparison UI

## Особенности модуля

- **Framework**: MVIKotlin для MVI архитектуры
- **Non-streaming**: Для OpenRouter используется `sendMessage()`
- **Streaming**: Для ZAI используется `sendMessageStreaming()`
- **Persistence**: Chat history сохраняется между сессиями
- **Markdown**: Рендеринг markdown в MessageBubble
- **Settings**: Настройки API (model, temperature, etc.)

## Migration Notes

При добавлении новых фич:
1. Добавить поля в `ChatState`
2. Добавить Msg типы в `ChatStoreFactory.Msg`
3. Обновить reducer для новых Msg
4. Добавить intent handlers в executor
5. Добавить UI компоненты
6. Обновить тесты
