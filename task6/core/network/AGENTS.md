# AGENTS.md — Core Network Module

## Назначение

Core network module — базовый модуль для API коммуникации с Z.AI API. Содержит HTTP client на базе Ktor, SSE streaming support и comprehensive error handling.

## Команды

```bash
./gradlew :core:network:check              # Tests + lint + detekt для модуля
./gradlew :core:network:ktlintFormat       # Auto-fix style
./gradlew :core:network:compileKotlinWasmJs # Проверка компиляции
```

## Архитектура

**Module Type**: Core infrastructure module (network layer)

**Pattern**: Repository pattern (interface + implementation)

**Module Dependencies**:
```
core:network
├── core:model   (ChatMessage, StreamChunk, ZAiRequest, ZAiResponse, ResponseFormat)
├── core         (ChatException hierarchy: ChatNetworkException, ChatApiException, etc.)
├── Ktor client  (core, content-negotiation, serialization, sse)
└── Kotlinx      (coroutines-core, serialization-json)
```

**Used by**:
- `feature:chat` → ChatRepository, ChatViewModel
- `composeApp` → tests

## Ключевые файлы (272 строки)

| Файл | Строк | Назначение |
|------|-------|------------|
| `ChatClient.kt` | 23 | Interface для chat API operations |
| `ChatClientImpl.kt` | 239 | Implementation с Ktor client, SSE, error handling |
| `ResponseConstraints.kt` | 10 | Data class для API constraints |

## ChatClient Interface

```kotlin
interface ChatClient {
    suspend fun sendMessage(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        constraints: ResponseConstraints = ResponseConstraints(),
        systemPrompt: String? = null,
    ): Result<ChatMessage>

    fun sendMessageStreaming(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        constraints: ResponseConstraints = ResponseConstraints(),
        systemPrompt: String? = null,
    ): Flow<StreamChunk>
}
```

**Methods**:
- `sendMessage()` — regular HTTP request, returns `Result<ChatMessage>`
- `sendMessageStreaming()` — SSE streaming request, returns `Flow<StreamChunk>`

**Current Implementation Status**:
- `sendMessage()` — ✅ USED by ChatRepository/ChatStoreFactory

- `sendMessageStreaming()` — ✅ USED by ChatStoreFactory (ZAI provider)

**Implementation Note**: Streaming is integrated for ZAI provider. OpenRouter requests use non-streaming `sendMessage()`.

**Parameters**:
- `apiKey: String` — API ключ для авторизации
- `model: String` — ID модели (из ModelType)
- `messages: List<ChatMessage>` — история сообщений
- `constraints: ResponseConstraints` — ограничения генерации (default: empty)
- `systemPrompt: String?` — system prompt (optional)

## ChatClientImpl Implementation

### Key Components

**HttpClient Configuration**:
```kotlin
private val client = HttpClient {
    install(ContentNegotiation) {
        json(json)
    }
    install(SSE) {
        bufferPolicy = SSEBufferPolicy.All
    }
}
```

**JSON Configuration**:
```kotlin
private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}
```

**Base URL**:
```kotlin
private val baseUrl = "https://api.z.ai/api/coding/paas/v4/chat/completions"
```

### Private Functions

#### buildAllMessages
```kotlin
private fun buildAllMessages(
    messages: List<ChatMessage>,
    systemPrompt: String?
): List<Message>
```
Конструирует messages list с system prompt в начале.

#### buildRequest
```kotlin
private fun buildRequest(
    model: String,
    messages: List<Message>,
    constraints: ResponseConstraints,
    stream: Boolean = false,
): ZAiRequest
```
Создает ZAiRequest с constraints и stream flag.

#### parseSSEEvent
```kotlin
private fun parseSSEEvent(data: String?): StreamChunk?
```
Парсит SSE events в StreamChunk:
- `null` или `"[DONE]"` → `StreamChunk.Done`
- `delta.reasoningContent` → `StreamChunk.Reasoning`
- `delta.content` → `StreamChunk.Content`
- `finishReason != null` → `StreamChunk.Done`

#### parseResponse
```kotlin
private fun parseResponse(
    responseBody: String,
    constraints: ResponseConstraints,
    model: String,
): Result<ChatMessage>
```
Парсит HTTP response в ChatMessage:
- Error response → `ChatClientException`
- Empty choices → `ChatClientException`
- Success → `ChatMessage` with content, tokens, finishReason

### sendMessage Implementation

```kotlin
override suspend fun sendMessage(
    apiKey: String,
    model: String,
    messages: List<ChatMessage>,
    constraints: ResponseConstraints,
    systemPrompt: String?,
): Result<ChatMessage> = try {
    val allMessages = buildAllMessages(messages, systemPrompt)
    val request = buildRequest(model, allMessages, constraints)
    
    val response: HttpResponse = client.post(baseUrl) {
        headers {
            append(HttpHeaders.Authorization, "Bearer $apiKey")
            append(HttpHeaders.AcceptLanguage, "en-US,en")
        }
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(request))
    }
    
    parseResponse(response.bodyAsText(), constraints, model)
} catch (e: ClientRequestException) {
    Result.failure(ChatNetworkException("Client error: ${e.message}", e))
} catch (e: ServerResponseException) {
    Result.failure(ChatNetworkException("Server error: ${e.message}", e))
} catch (e: SerializationException) {
    Result.failure(ChatSerializationException("Failed to parse response: ${e.message}", e))
} catch (e: ChatException) {
    Result.failure(e)
} catch (e: IllegalStateException) {
    Result.failure(ChatNetworkException("Connection error: ${e.message}", e))
} catch (e: IllegalArgumentException) {
    Result.failure(ChatNetworkException("Invalid request: ${e.message}", e))
}
```

### sendMessageStreaming Implementation

```kotlin
override fun sendMessageStreaming(
    apiKey: String,
    model: String,
    messages: List<ChatMessage>,
    constraints: ResponseConstraints,
    systemPrompt: String?,
): Flow<StreamChunk> = channelFlow {
    val allMessages = buildAllMessages(messages, systemPrompt)
    val request = buildRequest(model, allMessages, constraints, stream = true)
    
    client.sse(
        request = {
            url { takeFrom(baseUrl) }
            method = HttpMethod.Companion.Post
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header(HttpHeaders.AcceptLanguage, "en-US,en")
            setBody(json.encodeToString(request))
        }
    ) {
        incoming.collect { event ->
            parseSSEEvent(event.data)?.let { chunk -> send(chunk) }
        }
    }
}
```

## ResponseConstraints

```kotlin
data class ResponseConstraints(
    val maxTokens: Int? = null,
    val stop: List<String>? = null,
    val responseFormat: ResponseFormat? = null,
    val temperature: Double? = null,
)
```

**Fields**:
- `maxTokens: Int?` — максимальное количество токенов в ответе
- `stop: List<String>?` — stop sequences для прекращения генерации
- `responseFormat: ResponseFormat?` — формат ответа (text/json_object)
- `temperature: Double?` — temperature для генерации (0.0 - 2.0)

**Usage**:
```kotlin
val constraints = ResponseConstraints(
    maxTokens = 1000,
    temperature = 0.7,
    stop = listOf("\n", "END"),
    responseFormat = ResponseFormat("json_object")
)
```

## Streaming Support (SSE)

**Implementation Status**: ✅ Infrastructure READY, ✅ Integrated in ChatStoreFactory for ZAI

ChatStoreFactory uses `sendMessageStreaming()` for ZAI provider and keeps non-streaming flow for OpenRouter.

### StreamChunk Sealed Class

```kotlin
sealed class StreamChunk {
    data class Content(val text: String) : StreamChunk()
    data class Reasoning(val text: String) : StreamChunk()
    data object Done : StreamChunk()
}
```

**Types**:
- `Content(text: String)` — regular content chunk
- `Reasoning(text: String)` — reasoning/thinking content (for models with thinking mode)
- `Done` — end of stream signal

### SSE Event Parsing

**Event Flow**:
```
SSE Event → parseSSEEvent() → StreamChunk?
```

**Parsing Logic**:
1. `data == null` or `data == "[DONE]"` → `StreamChunk.Done`
2. Decode `ZAiStreamChunk` from JSON
3. Check `choice.delta.reasoningContent` → `StreamChunk.Reasoning`
4. Check `choice.delta.content` → `StreamChunk.Content`
5. Check `choice.finishReason` → `StreamChunk.Done`
6. SerializationException → null (skip chunk)

### Usage Example

```kotlin
client.sendMessageStreaming(
    apiKey = "your-api-key",
    model = "glm-5",
    messages = listOf(ChatMessage("user", "Hello")),
    constraints = ResponseConstraints(temperature = 0.7)
).collect { chunk ->
    when (chunk) {
        is StreamChunk.Content -> print(chunk.text)
        is StreamChunk.Reasoning -> print("[Thinking: ${chunk.text}]")
        is StreamChunk.Done -> println("\n[Done]")
    }
}
```

## Error Handling

### Exception Hierarchy

```
ChatException (base)
├── ChatNetworkException (connection, timeout, HTTP transport issues)
├── ChatApiException (HTTP error responses, API-specific errors)
│   └── ChatClientException (client-specific API errors)
├── ChatSerializationException (JSON decode/encode issues)
└── ChatStreamingException (SSE errors)
```

**Exception Types**:
- `ChatNetworkException` — network-level errors (connection, timeout, HTTP transport)
- `ChatApiException` — API-level errors (HTTP 4xx/5xx, API-specific errors)
- `ChatClientException` — client-specific API errors (invalid API key, rate limits, etc.)
- `ChatSerializationException` — JSON parsing/serialization errors
- `ChatStreamingException` — SSE streaming errors

### Exception Mapping

| Exception Type | Maps To | Message Format |
|----------------|---------|----------------|
| `ClientRequestException` | `ChatNetworkException` | "Client error: {message}" |
| `ServerResponseException` | `ChatNetworkException` | "Server error: {message}" |
| `SerializationException` | `ChatSerializationException` | "Failed to parse response: {message}" |
| `ChatException` | Propagate as is | Original message |
| `IllegalStateException` | `ChatNetworkException` | "Connection error: {message}" |
| `IllegalArgumentException` | `ChatNetworkException` | "Invalid request: {message}" |
| API error response | `ChatClientException` | "API Error {code}: {message}" |

### Error Response Handling

```kotlin
private fun parseResponse(...): Result<ChatMessage> {
    if (responseBody.contains("\"error\"")) {
        val errorResponse = json.decodeFromString<ZAiErrorResponse>(responseBody)
        val errorMsg = errorResponse.error.message ?: "Unknown error"
        return Result.failure(
            ChatClientException("API Error ${errorResponse.error.code}: $errorMsg")
        )
    }
    // ... success handling
}
```

## API Models (from core:model)

### Request Models

#### ZAiRequest
```kotlin
data class ZAiRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false,
    val maxTokens: Int? = null,
    val stop: List<String>? = null,
    val responseFormat: ResponseFormat? = null,
    val temperature: Double = 1.0,
)
```

#### Message
```kotlin
data class Message(
    val role: String,
    val content: String,
)
```

### Response Models

#### ZAiResponse
```kotlin
data class ZAiResponse(
    val id: String? = null,
    val choices: List<Choice>,
    val usage: Usage? = null,
)
```

#### Choice
```kotlin
data class Choice(
    val index: Int,
    val message: ResponseMessage,
    val finishReason: String?,
)
```

#### ResponseMessage
```kotlin
data class ResponseMessage(
    val role: String,
    val content: String,
    val reasoningContent: String? = null,
)
```

#### Usage
```kotlin
data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)
```

### Stream Models

#### ZAiStreamChunk
```kotlin
data class ZAiStreamChunk(
    val id: String? = null,
    val choices: List<StreamChoice>,
)
```

#### StreamChoice
```kotlin
data class StreamChoice(
    val index: Int,
    val delta: StreamDelta,
    val finishReason: String?,
)
```

#### StreamDelta
```kotlin
data class StreamDelta(
    val role: String? = null,
    val content: String? = null,
    val reasoningContent: String? = null,
)
```

### Error Models

#### ZAiErrorResponse
```kotlin
data class ZAiErrorResponse(
    val error: ZAiError,
)
```

#### ZAiError
```kotlin
data class ZAiError(
    val code: String,
    val message: String? = null,
)
```

## Logging

### Current Implementation

**Using println() for debugging:**
```kotlin
println("[ChatClient] Sending request to: $baseUrl")
println("[ChatClient] Request body: $requestBody")
println("[ChatClient] Response status: ${response.status}")
println("[ChatClient] Response body: $responseBody")
```

**Log Points**:
- Request: URL, body
- Response: status, body
- Streaming: connection established, data chunks, completion
- Errors: exception type, message
- API errors: error code, message

### Improving Logging

**TODO**: Replace println with proper logging library (e.g., Kermit):

```kotlin
// Add to dependencies
implementation("co.touchlab:kermit:2.0.0")

// Usage
private val logger = Logger.withTag("ChatClient")

logger.i { "Sending request to: $baseUrl" }
logger.d { "Request body: $requestBody" }
logger.e { "Error: ${e.message}" }
```

## HTTP Headers

### Required Headers

```kotlin
headers {
    append(HttpHeaders.Authorization, "Bearer $apiKey")
    append(HttpHeaders.AcceptLanguage, "en-US,en")
}
contentType(ContentType.Application.Json)
```

**Headers**:
- `Authorization: Bearer {apiKey}` — API authentication
- `Accept-Language: en-US,en` — language preference
- `Content-Type: application/json` — request body format

## Usage in Other Modules

### feature:chat (ChatViewModel.kt)

```kotlin
import network.ChatClientImpl

@Composable
fun rememberChatViewModel(...): ChatViewModel {
    val client = remember { ChatClientImpl() }
    // ...
}
```

### feature:chat (ChatRepository.kt)

**NOTE**: ChatRepository exposes both `sendMessage()` and `sendMessageStreaming()`. ChatStoreFactory chooses streaming for ZAI and non-streaming for OpenRouter.

```kotlin
import network.ChatClient

class ChatRepositoryImpl(
    private val client: ChatClient,
    private val scope: CoroutineScope
) : ChatRepository {
    
    override suspend fun sendMessage(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings,
    ): SendMessageResult {
        val constraints = settings.toResponseConstraints()
        
        return when (val result = client.sendMessage(
            apiKey = settings.apiKey,
            model = settings.model,
            messages = messages,
            constraints = constraints,
        )) {
            is Result.Success -> SendMessageResult.Success(
                response = result.value,
                metric = createMetric(result.value, startTime)
            )
            is Result.Failure -> SendMessageResult.Error(
                message = result.error.message ?: "Unknown error"
            )
        }
    }
}
```

**To Enable Streaming**: Replace `client.sendMessage()` with `client.sendMessageStreaming()` and collect `Flow<StreamChunk>` to update UI incrementally.

## Testing

### Test Files (in composeApp)

| File | Назначение |
|------|------------|
| `composeApp/.../ChatClientTest.kt` | Unit tests for ChatClient |
| `composeApp/.../baseline/NetworkBaselineTest.kt` | Characterization tests for network layer |
| `composeApp/.../ChatRepositoryTest.kt` | Uses FakeChatClient for testing |

### FakeChatClient Example

```kotlin
class FakeChatClient : ChatClient {
    var sendMessageResult: Result<ChatMessage> = Result.failure(Exception("Not implemented"))
    var streamingChunks: List<StreamChunk> = emptyList()
    
    override suspend fun sendMessage(...): Result<ChatMessage> {
        return sendMessageResult
    }
    
    override fun sendMessageStreaming(...): Flow<StreamChunk> {
        return streamingChunks.asFlow()
    }
}
```

**No tests in core/network itself** — all tests are in composeApp module.

## Критичные правила

- **Interface + Implementation**: ChatClient interface, ChatClientImpl implementation
- **Result type**: `sendMessage` возвращает `Result<ChatMessage>` (not throwing exceptions)
- **Flow for streaming**: `sendMessageStreaming` возвращает `Flow<StreamChunk>`
- **Error handling**: Все exceptions мапятся в ChatException hierarchy
- **Detekt**: БЕЗ `@Suppress` (исключение: build.gradle.kts)
- **No comments**: Код self-documenting
- **Logging**: println statements для debugging (можно заменить на proper logging)
- **Coroutines**: Используются coroutines и Flow для async operations

## Workflow при изменениях

1. Изменил code → `./gradlew :core:network:check`
2. Style issues → `./gradlew :core:network:ktlintFormat`
3. Проверил integration → `./gradlew :feature:chat:check`
4. Проверил tests → `./gradlew :composeApp:test --tests "*ChatClient*" --tests "*Network*"`
5. Commit

## Частые задачи

### Добавить новый API endpoint

1. Добавить метод в ChatClient interface:
```kotlin
suspend fun newEndpoint(
    apiKey: String,
    param: String,
): Result<NewResponse>
```

2. Реализовать в ChatClientImpl (copy-paste от sendMessage):
```kotlin
override suspend fun newEndpoint(...): Result<NewResponse> = try {
    val response = client.post(newUrl) {
        // headers, body
    }
    parseNewResponse(response.bodyAsText())
} catch (e: Exception) {
    // error handling
}
```

3. Добавить model classes в core:model (если нужно)
4. Написать tests в composeApp

### Изменить error handling

1. Добавить новый exception в `core/exception/ChatExceptions.kt`:
```kotlin
class ChatNewException(message: String) : ChatException(message)
```

2. Добавить mapping в ChatClientImpl catch block:
```kotlin
catch (e: NewExceptionType) {
    Result.failure(ChatNewException("New error: ${e.message}", e))
}
```

3. Написать tests для нового exception

### Изменить streaming behavior

1. Изменить `parseSSEEvent()` логику:
```kotlin
private fun parseSSEEvent(data: String?): StreamChunk? {
    // Add new parsing logic
}
```

2. Добавить новый StreamChunk type (если нужно) в core:model:
```kotlin
sealed class StreamChunk {
    // existing types
    data class NewType(val data: String) : StreamChunk()
}
```

3. Обновить MessageHandler в feature:chat
4. Написать tests

### Заменить println на proper logging

1. Добавить logging library в dependencies:
```kotlin
// build.gradle.kts
implementation("co.touchlab:kermit:2.0.0")
```

2. Создать logger instance:
```kotlin
private val logger = Logger.withTag("ChatClient")
```

3. Заменить все println на logger calls:
```kotlin
// Before
println("[ChatClient] Sending request to: $baseUrl")

// After
logger.i { "Sending request to: $baseUrl" }
```

4. Протестировать

### Добавить новый constraint

1. Добавить поле в ResponseConstraints:
```kotlin
data class ResponseConstraints(
    // existing fields
    val newConstraint: String? = null,
)
```

2. Обновить buildRequest():
```kotlin
ZAiRequest(
    // existing fields
    newConstraint = constraints.newConstraint,
)
```

3. Обновить ZAiRequest в core:model
4. Обновить ApiSettings.toResponseConstraints() в feature:settings
5. Написать tests

## Особенности модуля

- **Core infrastructure**: Базовый модуль для всех network operations
- **Ktor-based**: Использует Ktor client для HTTP и SSE (version 3.4.1)
- **Two modes**: Regular request (Result) и streaming (Flow)
- **Streaming integration**: Используется в ChatStoreFactory для ZAI provider
- **Comprehensive error handling**: Все exceptions мапятся в ChatException hierarchy
- **SSE streaming**: Поддержка Server-Sent Events для streaming responses
- **Reasoning content**: Поддержка reasoning/thinking content от моделей (GLM-5 thinking mode)
- **Flexible constraints**: ResponseConstraints для контроля генерации (maxTokens, temperature, stop sequences, responseFormat)
- **System prompts**: Поддержка system prompts через параметр (добавляется в начало messages)
- **Logging**: println statements для debugging (можно улучшить с proper logging library)
- **Coroutines & Flow**: Async operations с Kotlin coroutines и Flow
- **JSON serialization**: Kotlinx serialization с lenient parsing
- **WASM support**: Оптимизирован для Kotlin/Wasm target
