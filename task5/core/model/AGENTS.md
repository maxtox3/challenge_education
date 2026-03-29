# AGENTS.md — Core Model Module

## Назначение

Core model module — централизованный модуль для всех data classes и enums. Shared across all modules, содержит API contract модели и UI models. No business logic — только data + serialization.

## Команды

```bash
./gradlew :core:model:check              # Tests + lint + detekt для модуля
./gradlew :core:model:ktlintFormat       # Auto-fix style
./gradlew :core:model:compileKotlinWasmJs # Проверка компиляции
```

## Архитектура

**Module Type**: Core infrastructure module (data layer)

**Pattern**: Plain data models (no business logic)

**Module Dependencies**:
```
core:model
└── kotlinx-serialization-json (only dependency)
```

**Used by**: ALL modules
- `feature:chat` — ChatMessage, StreamChunk, MetricRecord, ModelType
- `feature:metrics` — MetricRecord, ConstraintsInfo
- `feature:reasoning` — ReasoningMode, ReasoningResult, ReasoningComparison
- `feature:settings` — ModelType, ResponseFormat
- `core:network` — All API models (ZAiRequest, ZAiResponse, etc.)
- `composeApp` — tests

## Ключевые файлы (178 строк)

| Файл | Строк | Назначение |
|------|-------|------------|
| `ChatMessage.kt` | 14 | Модель сообщения в чате (user/assistant/system) |
| `MetricRecord.kt` | 30 | Модель метрик запроса + ConstraintsInfo для display |
| `ModelType.kt` | 14 | Enum доступных AI моделей (4 models) |
| `ReasoningMode.kt` | 27 | Enum режимов рассуждения + ReasoningResult + ReasoningComparison |
| `StreamChunk.kt` | 21 | Sealed class для streaming events (Content, Reasoning, Done) |
| `ZAiRequest.kt` | 72 | API models (request, response, streaming, error) с @Serializable |

## Model Categories

### 1. Chat Models

#### ChatMessage
```kotlin
data class ChatMessage(
    val role: String,                    // "user", "assistant", "system"
    val content: String,                 // Текст сообщения
    val timestamp: Long = 0L,            // Timestamp (milliseconds)
    val mode: String = "free",           // "free" или "constrained"
    val tokensUsed: Int? = null,         // Использованные токены
    val maxTokens: Int? = null,          // Лимит токенов (null = unlimited)
    val finishReason: String? = null,    // Причина завершения ("stop", "length", etc.)
    val isReasoningContent: Boolean = false, // Reasoning content flag (thinking mode)
    val isStreaming: Boolean = false,    // Streaming in progress flag
    val model: String? = null,           // ID модели (из ModelType)
)
```

**Usage**:
- `feature:chat` — messages list, display, state management
- `core:network` — return type from ChatClient

**Fields**:
- `role` — роль отправителя ("user", "assistant", "system")
- `content` — текст сообщения
- `timestamp` — время создания (0L = unknown)
- `mode` — режим генерации ("free" = без ограничений, "constrained" = с constraints)
- `tokensUsed` — использованные токены (null = unknown)
- `maxTokens` — лимит токенов (null = unlimited)
- `finishReason` — причина завершения генерации
- `isReasoningContent` — флаг reasoning/thinking content
- `isStreaming` — флаг streaming in progress
- `model` — ID модели (из ModelType.id)

### 2. Metrics Models

#### MetricRecord
```kotlin
data class MetricRecord(
    val id: Int,                         // Уникальный ID
    val prompt: String,                  // Исходный prompt
    val response: String,                // Ответ модели
    val mode: String,                    // Режим генерации
    val responseLength: Int,             // Длина ответа в символах
    val tokensUsed: Int?,                // Использованные токены
    val maxTokens: Int?,                 // Лимит токенов
    val finishReason: String?,           // Причина завершения
    val responseTimeMs: Long,            // Время ответа в ms
    val constraints: ConstraintsInfo,    // Constraints настройки
)
```

**Usage**:
- `feature:chat` — metrics tracking (ChatState.metrics)
- `feature:metrics` — display metrics in MetricsDialog

#### ConstraintsInfo
```kotlin
data class ConstraintsInfo(
    val maxTokens: Int?,                 // Лимит токенов
    val stopSequences: List<String>,     // Stop sequences
    val responseFormat: String,          // Формат ответа ("text", "json")
    val temperature: Double,             // Temperature (0.0 - 2.0)
) {
    fun toDisplayString(): String
}
```

**Methods**:
- `toDisplayString()` — форматирует constraints для UI display
  - `maxTokens=100` → "maxTokens=100"
  - `temperature=0.7` → "temp=0.7"
  - Empty constraints → "Free"

**Usage**:
- `feature:metrics` — display constraints in table headers
- `feature:chat` — create from ApiSettings

### 3. Model Selection

#### ModelType
```kotlin
enum class ModelType(val id: String, val displayName: String, val level: String) {
    LIGHT("glm-4-32b-0414-128k", "GLM-4 32b", "Супер легкая, слабая и дешевая"),
    AIR("glm-4.5-air", "GLM-4.5 Air", "Слабая"),
    MIDDLE("glm-4.7", "GLM-4.7", "Средняя"),
    PRO("glm-5", "GLM-5", "Сильная");

    companion object {
        fun fromId(id: String): ModelType = entries.find { it.id == id } ?: PRO
        val allIds: List<String> get() = entries.map { it.id }
    }
}
```

**Properties**:
- `id: String` — ID модели для API calls
- `displayName: String` — Отображаемое название
- `level: String` — Уровень мощности/стоимости

**Companion Object Methods**:
- `fromId(id: String)` — найти модель по ID (default: PRO)
- `allIds: List<String>` — список всех ID

**Usage**:
- `feature:settings` — ModelSelector dropdown
- `feature:chat` — ChatRepository, ApiSettings.model
- `core:network` — API calls

**Available Models**:
| Enum | ID | Display Name | Level |
|------|----|--------------| ----- |
| LIGHT | glm-4-32b-0414-128k | GLM-4 32b | Супер легкая, слабая и дешевая |
| AIR | glm-4.5-air | GLM-4.5 Air | Слабая |
| MIDDLE | glm-4.7 | GLM-4.7 | Средняя |
| PRO | glm-5 | GLM-5 | Сильная |

### 4. Reasoning Models

#### ReasoningMode
```kotlin
enum class ReasoningMode(val displayName: String, val description: String) {
    DIRECT("Прямой ответ", "Без дополнительных инструкций"),
    STEP_BY_STEP("Пошагово", "Инструкция: решай пошагово"),
    META_PROMPT("Мета-промпт", "Сначала составляет промпт, затем использует его"),
    EXPERT_PANEL("Эксперты", "Группа экспертов: аналитик, инженер, критик"),
}
```

**Properties**:
- `displayName: String` — Отображаемое название
- `description: String` — Описание подхода

**Available Modes**:
| Enum | Display Name | Description |
|------|--------------|-------------|
| DIRECT | Прямой ответ | Без дополнительных инструкций |
| STEP_BY_STEP | Пошагово | Инструкция: решай пошагово |
| META_PROMPT | Мета-промпт | Сначала составляет промпт, затем использует его |
| EXPERT_PANEL | Эксперты | Группа экспертов: аналитик, инженер, критик |

**Usage**:
- `feature:reasoning` — tabs in ReasoningDialog
- `feature:chat` — ChatRepository.runReasoningComparison()

#### ReasoningResult
```kotlin
data class ReasoningResult(
    val mode: ReasoningMode,             // Режим рассуждения
    val systemPrompt: String,            // System prompt для этого режима
    val actualPrompt: String,            // Фактический prompt (для meta-prompt)
    val response: String = "",           // Ответ модели
    val responseTimeMs: Long = 0,        // Время ответа в ms
    val tokensUsed: Int? = null,         // Использованные токены
    val isLoading: Boolean = false,      // Loading in progress flag
    val error: String? = null,           // Error message (null = success)
)
```

**Usage**:
- `feature:reasoning` — display single mode result
- `feature:chat` — ChatState.reasoningComparison.results

#### ReasoningComparison
```kotlin
data class ReasoningComparison(
    val task: String,                    // Задача для сравнения
    val results: Map<ReasoningMode, ReasoningResult>,
) {
    val isComplete: Boolean
        get() = results.values.all { !it.isLoading && it.error == null && it.response.isNotEmpty() }

    val hasAnyResult: Boolean
        get() = results.values.any { it.response.isNotEmpty() }
}
```

**Computed Properties**:
- `isComplete` — все 4 режима завершились успешно (no loading, no error, has response)
- `hasAnyResult` — хотя бы один режим имеет результат

**Usage**:
- `feature:reasoning` — display comparison table (only when isComplete)
- `feature:chat` — ChatState.reasoningComparison

### 5. Streaming Models

#### StreamChunk
```kotlin
sealed class StreamChunk {
    /**
     * Regular content chunk from the assistant.
     */
    data class Content(val text: String) : StreamChunk()

    /**
     * Reasoning/thinking content chunk (for models with thinking mode).
     */
    data class Reasoning(val text: String) : StreamChunk()

    /**
     * Signals the end of the stream.
     */
    data object Done : StreamChunk()
}
```

**Types**:
- `Content(text: String)` — regular content chunk
- `Reasoning(text: String)` — reasoning/thinking content (GLM-5 thinking mode)
- `Done` — end of stream signal

**Usage**:
- `core:network` — ChatClient.sendMessageStreaming() return type
- `feature:chat` — MessageHandler processes StreamChunk events

### 6. API Models (with @Serializable)

#### Request Models

##### ZAiRequest
```kotlin
@Serializable
data class ZAiRequest(
    val model: String = "glm-5",
    val messages: List<Message>,
    val stream: Boolean = false,
    val temperature: Double = 1.0,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    val stop: List<String>? = null,
    @SerialName("response_format")
    val responseFormat: ResponseFormat? = null,
)
```

**Fields** (snake_case → camelCase mapping):
- `model` — ID модели (из ModelType.id)
- `messages` — список сообщений (Message objects)
- `stream` — streaming mode flag
- `temperature` — temperature для генерации
- `maxTokens` — лимит токенов (mapped from `max_tokens`)
- `stop` — stop sequences
- `responseFormat` — формат ответа (mapped from `response_format`)

##### Message
```kotlin
@Serializable
data class Message(
    val role: String,
    val content: String,
    @SerialName("reasoning_content")
    val reasoningContent: String? = null,
)
```

##### ResponseFormat
```kotlin
@Serializable
data class ResponseFormat(val type: String)
```

**Usage**: `"text"` или `"json_object"`

#### Response Models

##### ZAiResponse
```kotlin
@Serializable
data class ZAiResponse(
    val id: String? = null,
    val choices: List<Choice>,
    val usage: Usage? = null
)
```

##### Choice
```kotlin
@Serializable
data class Choice(
    val message: Message,
    @SerialName("finish_reason")
    val finishReason: String? = null,
)
```

##### Usage
```kotlin
@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int,
)
```

#### Streaming Models

##### ZAiStreamChunk
```kotlin
@Serializable
data class ZAiStreamChunk(
    val id: String? = null,
    val choices: List<StreamChoice>,
)
```

##### StreamChoice
```kotlin
@Serializable
data class StreamChoice(
    val delta: Delta,
    @SerialName("finish_reason")
    val finishReason: String? = null,
)
```

##### Delta
```kotlin
@Serializable
data class Delta(
    val content: String? = null,
    @SerialName("reasoning_content")
    val reasoningContent: String? = null,
)
```

#### Error Models

##### ZAiErrorResponse
```kotlin
@Serializable
data class ZAiErrorResponse(val error: ZAiError)
```

##### ZAiError
```kotlin
@Serializable
data class ZAiError(
    val code: String,
    val message: String? = null
)
```

## Serialization

### Annotations Used

**@Serializable** — для JSON serialization/deserialization:
```kotlin
@Serializable
data class ZAiRequest(...)
```

**@SerialName("snake_case")** — для маппинга snake_case API полей:
```kotlin
@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int,
)
```

### JSON Mapping

| Kotlin (camelCase) | JSON (snake_case) |
|--------------------|-------------------|
| `maxTokens` | `max_tokens` |
| `responseFormat` | `response_format` |
| `promptTokens` | `prompt_tokens` |
| `completionTokens` | `completion_tokens` |
| `totalTokens` | `total_tokens` |
| `finishReason` | `finish_reason` |
| `reasoningContent` | `reasoning_content` |

### Serialization Configuration

In `core:network`:
```kotlin
private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}
```

## Computed Properties

### ReasoningComparison

```kotlin
val isComplete: Boolean
    get() = results.values.all { !it.isLoading && it.error == null && it.response.isNotEmpty() }

val hasAnyResult: Boolean
    get() = results.values.any { it.response.isNotEmpty() }
```

**Usage**:
- `isComplete` — show ComparisonTable only when all results ready
- `hasAnyResult` — show tabs/content only when at least one result exists

### ConstraintsInfo

```kotlin
fun toDisplayString(): String {
    val parts = mutableListOf<String>()
    if (maxTokens != null) parts.add("maxTokens=$maxTokens")
    if (stopSequences.isNotEmpty()) parts.add("stop=${stopSequences.joinToString(",")}")
    if (responseFormat != "text") parts.add("format=$responseFormat")
    if (temperature != 1.0) parts.add("temp=$temperature")
    return if (parts.isEmpty()) "Free" else parts.joinToString(", ")
}
```

**Examples**:
- Empty constraints → `"Free"`
- `maxTokens=1000` → `"maxTokens=1000"`
- `maxTokens=1000, temperature=0.7` → `"maxTokens=1000, temp=0.7"`
- `maxTokens=500, stop=["\n"], temperature=0.8` → `"maxTokens=500, stop=\n, temp=0.8"`

## Usage in Other Modules

### feature:chat
```kotlin
import model.ChatMessage
import model.StreamChunk
import model.MetricRecord
import model.ModelType
import model.ReasoningMode
import model.ReasoningComparison

// ChatState
data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val metrics: List<MetricRecord> = emptyList(),
    val settings: ApiSettings = ApiSettings(),
    val reasoningComparison: ReasoningComparison = ReasoningComparison(...),
)

// ChatRepository
suspend fun runReasoningComparison(
    task: String,
    settings: ApiSettings,
    onProgress: (ReasoningComparison) -> Unit,
): ReasoningComparison
```

### feature:metrics
```kotlin
import model.MetricRecord
import model.ConstraintsInfo

@Composable
fun MetricsDialog(metrics: List<MetricRecord>, onDismiss: () -> Unit) {
    val groupedByPrompt = metrics.groupBy { it.prompt }
    // Display constraints.info.toDisplayString() in table headers
}
```

### feature:reasoning
```kotlin
import model.ReasoningMode
import model.ReasoningResult
import model.ReasoningComparison

@Composable
fun ReasoningDialog(
    comparison: ReasoningComparison,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRunComparison: (task: String) -> Unit,
) {
    // Use ReasoningMode.entries for tabs
    // Show ComparisonTable when comparison.isComplete
}
```

### feature:settings
```kotlin
import model.ModelType
import model.ResponseFormat

@Composable
fun ModelSelector(selectedModelId: String, onModelSelected: (String) -> Unit) {
    // Use ModelType.entries for dropdown items
    // Display: "${model.displayName} (${model.level})"
}

data class ApiSettings(
    val model: String = ModelType.PRO.id,
    // ...
) {
    fun toResponseConstraints(): ResponseConstraints {
        // ...
        val format = when (responseFormat) {
            "json" -> ResponseFormat("json_object")
            else -> null
        }
        // ...
    }
}
```

### core:network
```kotlin
import model.ChatMessage
import model.StreamChunk
import model.ZAiRequest
import model.ZAiResponse
import model.ZAiStreamChunk
import model.ZAiErrorResponse
import model.Message

class ChatClientImpl : ChatClient {
    override suspend fun sendMessage(...): Result<ChatMessage> {
        val request = ZAiRequest(...)
        val response: ZAiResponse = json.decodeFromString(responseBody)
        // Convert to ChatMessage
    }
    
    override fun sendMessageStreaming(...): Flow<StreamChunk> {
        // Parse ZAiStreamChunk, emit StreamChunk.Content/Reasoning/Done
    }
}
```

## Критичные правила

- **Immutable data classes**: Все fields — `val`, с default values
- **@Serializable для API models**: Только для models, которые сериализуются в JSON
- **@SerialName для API fields**: snake_case → camelCase маппинг
- **No business logic**: Только data + computed properties
- **No dependencies**: Только kotlinx-serialization-json
- **Detekt**: БЕЗ `@Suppress` (исключение: build.gradle.kts)
- **No comments**: Только KDoc для public API (как в StreamChunk)
- **Default values**: Все fields должны иметь default values
- **Nullability**: Использовать `?` для optional fields

## Workflow при изменениях

1. Изменил model → `./gradlew :core:model:check`
2. Обновил usage во всех модулях (если изменился API)
3. Запустил full build → `./gradlew check`
4. Проверил tests → `./gradlew :composeApp:test`
5. Commit

## Частые задачи

### Добавить новое поле в ChatMessage

1. Добавить поле в ChatMessage data class с default value:
```kotlin
data class ChatMessage(
    // existing fields
    val newField: String? = null,
)
```

2. Обновить usage в ChatRepository (если нужно)
3. Обновить tests

### Добавить новый ModelType

1. Добавить enum value в ModelType:
```kotlin
enum class ModelType(...) {
    // existing models
    NEW_MODEL("glm-new", "GLM New", "Новая модель");
}
```

2. UI автоматически обновится (ModelSelector использует `ModelType.entries`)
3. Обновить tests

### Добавить новый ReasoningMode

1. Добавить enum value в ReasoningMode:
```kotlin
enum class ReasoningMode(...) {
    // existing modes
    NEW_MODE("Новый режим", "Описание нового режима"),
}
```

2. Обновить system prompts в ChatRepository:
```kotlin
private fun getSystemPrompt(mode: ReasoningMode): String = when (mode) {
    // existing modes
    ReasoningMode.NEW_MODE -> "New mode system prompt"
}
```

3. UI автоматически обновится (ScrollableTabRow использует `ReasoningMode.entries`)
4. Обновить tests

### Добавить новое API field

1. Добавить поле в ZAiRequest/ZAiResponse:
```kotlin
@Serializable
data class ZAiRequest(
    // existing fields
    @SerialName("new_field")
    val newField: String? = null,
)
```

2. Обновить ChatClientImpl usage:
```kotlin
val request = ZAiRequest(
    // existing fields
    newField = constraints.newField,
)
```

3. Обновить tests

### Добавить новый StreamChunk type

1. Добавить type в StreamChunk sealed class:
```kotlin
sealed class StreamChunk {
    // existing types
    data class NewType(val data: String) : StreamChunk()
}
```

2. Обновить parseSSEEvent() в ChatClientImpl:
```kotlin
private fun parseSSEEvent(data: String?): StreamChunk? {
    // ...
    when {
        // existing cases
        condition -> StreamChunk.NewType(data)
    }
}
```

3. Обновить MessageHandler в feature:chat
4. Обновить tests

## Особенности модуля

- **Zero dependencies**: Только kotlinx-serialization-json (minimal footprint)
- **Shared models**: Используются всеми модулями (single source of truth)
- **API contract**: Models для API коммуникации (ZAiRequest, ZAiResponse, etc.)
- **UI models**: Models для UI display (MetricRecord, ReasoningComparison)
- **Sealed classes**: StreamChunk для type-safe streaming events
- **Enums**: ModelType, ReasoningMode для type-safe selection
- **Computed properties**: isComplete, hasAnyResult, toDisplayString
- **Serialization annotations**: @Serializable, @SerialName для JSON mapping
- **Immutable**: Все data classes immutable с default values
- **WASM support**: Оптимизирован для Kotlin/Wasm target
- **No business logic**: Только data models + computed properties
- **KDoc**: Документация только для public API (как в StreamChunk)
- **Consistent naming**: camelCase в Kotlin, snake_case в JSON
