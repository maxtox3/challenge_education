# AGENTS.md — Chat Module

## Назначение

Chat feature — главный модуль приложения. Реализует UI и бизнес-логику чата с AI моделями через MVI архитектуру.

## Команды

```bash
./gradlew :feature:chat:check              # Tests + lint + detekt для модуля
./gradlew :feature:chat:ktlintFormat       # Auto-fix style
./gradlew :feature:chat:compileKotlinWasmJs # Проверка компиляции
```

## Архитектура

**MVI Pattern**:
- `ChatState` — immutable data class (единственный источник истины)
- `ChatIntent` — sealed class пользовательских действий
- `ChatSideEffect` — одноразовые события (навигация, уведомления)

**Data Flow**:
```
UI Event → Intent → ViewModel → UseCases → Repository → State Update → UI Render
```

**Module Dependencies**:
```
feature:chat
├── core:model        (ChatMessage, MetricRecord, ReasoningComparison)
├── core:network      (ChatClient)
├── feature:settings  (ApiSettings)
├── feature:metrics   (MetricsDialog)
└── feature:reasoning (ReasoningDialog)
```

## Ключевые файлы

### Business Logic
| Файл | Назначение |
|------|------------|
| `ChatViewModel.kt` | MVI ViewModel, state management, intent processing |
| `ChatRepository.kt` | API calls, streaming, data layer |
| `ChatUseCases.kt` | Business logic: sendMessage, reasoning comparison |
| `ChatIntents.kt` | Intent handlers (extracted from ViewModel) |
| `MessageHandler.kt` | Stream processing, message parsing |
| `ChatState.kt` | Immutable state definition |
| `ChatIntent.kt` | Sealed class of user actions |
| `ChatSideEffect.kt` | One-time events |

### UI Components
| Файл | Назначение |
|------|------------|
| `ui/App.kt` | Main composable, MVI wiring |
| `ui/components/MessageBubble.kt` | Message rendering (user/AI) |
| `ui/components/ChatInput.kt` | Input field + send button |
| `ui/components/MessageBubbleTags.kt` | Test tags for MessageBubble |
| `ui/components/ChatInputTags.kt` | Test tags for ChatInput |
| `ui/icons/*` | Icon composables (Settings, Brain, Chart, etc.) |

## MVI Components

### ChatState
```kotlin
data class ChatState(
    val inputText: String,
    val messages: List<ChatMessage>,
    val isLoading: Boolean,
    val errorMessage: String?,
    val showSettings: Boolean,
    val showMetrics: Boolean,
    val showReasoning: Boolean,
    val metrics: List<MetricRecord>,
    val settings: ApiSettings,
    val reasoningComparison: ReasoningComparison,
    val isReasoningLoading: Boolean,
)
```

### ChatIntent (примеры)
- `SendMessage` — отправить сообщение
- `UpdateInputText` — обновить текст ввода
- `ToggleSettings` — показать/скрыть настройки
- `ToggleMetrics` — показать/скрыть метрики
- `RunReasoningComparison` — запустить сравнение моделей
- `ClearChat` — очистить историю

### ChatSideEffect
- `ShowError(message: String)` — показать ошибку
- `ScrollToBottom` — прокрутить к последнему сообщению

## Repository Layer

### ChatRepository Interface
```kotlin
interface ChatRepository {
    suspend fun sendMessage(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings
    ): SendMessageResult
    
    suspend fun runReasoningComparison(
        task: String,
        settings: ApiSettings,
        onProgress: (ReasoningComparison) -> Unit
    ): ReasoningComparison
    
    fun sendMessageStreaming(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings
    ): Flow<StreamChunk>
}
```

### SendMessageResult
- `Success(response: ChatMessage, metric: MetricRecord)`
- `Error(message: String)`

## UI Composition

### App.kt Structure
```
App
├── TopBar (Settings, Metrics, Reasoning buttons)
├── MessageList (LazyColumn)
│   └── MessageBubble (for each message)
├── TypingIndicator (when isLoading)
├── ChatInput (TextField + Send button)
├── SettingsDialog (if showSettings)
├── MetricsDialog (if showMetrics)
└── ReasoningDialog (if showReasoning)
```

## Работа со Streaming

MessageHandler обрабатывает stream chunks:
1. `StreamChunk.Token` → добавляет к текущему сообщению
2. `StreamChunk.Complete` → финализирует сообщение
3. `StreamChunk.Error` → показывает ошибку

## Error Handling

- Network errors → `errorMessage` в state
- Loading states → `isLoading`, `isReasoningLoading`
- User feedback → side effects (snackbars, dialogs)

## Критичные правила

- **Detekt**: БЕЗ `@Suppress` (исключения: только тесты)
- **No comments**: Код self-documenting
- **KDoc**: документация приветствуется
- **State updates**: Только через `_uiState.update {}`
- **Intents**: Обрабатываются в `ChatIntents` классе
- **SideEffects**: Одноразовые события, не state

1. Изменил код → `./gradlew :feature:chat:check`
2. Style issues → `./gradlew :feature:chat:ktlintFormat`
3. Проверил зависимые модули → `./gradlew check`
4. Commit

## Частые задачи

### Добавить новый Intent
1. Добавить case в `ChatIntent.kt`
2. Добавить handler в `ChatIntents.kt`
3. Обновить `ChatState.kt` (если нужно новое поле)
4. Вызвать из UI: `viewModel.processIntent(NewIntent(...))`

### Добавить новый UI компонент
1. Создать файл в `ui/components/`
2. Добавить test tags в отдельный файл `*Tags.kt`
3. Использовать в `App.kt` или других компонентах

### Изменить API interaction
1. Изменить `ChatRepository.kt`
2. Обновить `ChatUseCases.kt` (если меняется логика)
3. Добавить обработку в `ChatViewModel.kt`
4. Обновить tests (когда появятся)

## Особенности модуля

- **Streaming**: Поддержка streaming responses через Flow
- **Markdown**: Рендеринг markdown в MessageBubble
- **Multi-model**: Сравнение ответов разных моделей (reasoning)
- **Metrics tracking**: Сбор метрик для каждого запроса
- **Settings persistence**: Настройки сохраняются в localStorage (WASM)
