# AgentStore Architecture (The Elm Architecture)

## Overview

AgentStore — центральная сущность приложения, реализующая **The Elm Architecture (TEA)** для управления состоянием AI-агента.

## Core Principles

| Принцип | Применение |
|---------|------------|
| **Immutable State** | `AgentState` — data class, `update()` возвращает новую копию |
| **Pure Updates** | `update(state, msg)` — чистая функция, без side effects |
| **Commands** | Side effects вынесены в `Cmd`, выполняются отдельно |
| **Unidirectional** | State → View → Msg → Update → State |
| **Single Source of Truth** | AgentStore — единственный источник состояния |

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    AgentStore (Core)                     │
│                                                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │  State (Единственный источник истины)              │ │
│  │  AgentState {                                       │ │
│  │    context: AgentContext,     // история + summary  │ │
│  │    config: AgentConfig,       // настройки          │ │
│  │    messages: List<Message>,   // текущий диалог     │ │
│  │    metrics: AgentMetrics,     // статистика         │ │
│  │    status: AgentStatus        // idle/loading/error │ │
│  │  }                                                 │ │
│  └────────────────────────────────────────────────────┘ │
│                                                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Update (Pure Function)                            │ │
│  │  (state, msg) → (newState, cmd?)                   │ │
│  │                                                     │ │
│  │  Msg:                                               │ │
│  │    - SendMessage(prompt)                           │ │
│  │    - HandleApiResponse(response)                   │ │
│  │    - HandleApiError(error)                         │ │
│  │    - LoadContextFromStorage                        │ │
│  │    - ContextLoaded(context)                        │ │
│  │    - CompressHistory                               │ │
│  │    - ClearContext                                  │ │
│  └────────────────────────────────────────────────────┘ │
│                                                          │
└─────────────────────────────────────────────────────────┘
           ↓                                    ↓
    ┌──────────────┐                   ┌────────────────┐
    │     View     │                   │     Effects    │
    │  (UI/Compose)│                   │  (Side Effects)│
    └──────────────┘                   └────────────────┘
                                                ↓
                          ┌────────────────────────────────┐
                          │  - LlmClient (API calls)       │
                          │  - ContextStorage (localStorage)│
                          │  - ContextCompressor (LLM)     │
                          │  - TokenEstimator              │
                          └────────────────────────────────┘
```

---

## Data Flow

```
User Input
    ↓
View (Compose)
    ↓
Intent → Msg mapping
    ↓
AgentStore.dispatch(Msg)
    ↓
update(state, msg) → (newState, cmd?)
    ↓
    ├─→ State update (pure, synchronous)
    │       ↓
    │   _stateFlow.value = newState
    │       ↓
    │   View re-renders
    │
    └─→ Cmd execution (side effect, async)
            ↓
        LlmClient / Storage / Compressor
            ↓
        Result wrapped as Msg
            ↓
        dispatch(resultMsg) → cycle continues
```

---

## Components

### 1. AgentState (Immutable State)

```kotlin
data class AgentState(
    val context: AgentContext = AgentContext(),
    val config: AgentConfig = AgentConfig(),
    val messages: List<ChatMessage> = emptyList(),
    val metrics: AgentMetrics = AgentMetrics(),
    val status: AgentStatus = AgentStatus.Idle
)

enum class AgentStatus {
    Idle, Loading, Error
}
```

**Responsibilities:**
- Хранит текущее состояние агента
- Immutable — каждое обновление создаёт новую копию
- Единственный источник истины для всего приложения

---

### 2. AgentMsg (Messages)

```kotlin
sealed class AgentMsg {
    // User actions
    data class SendMessage(val prompt: String) : AgentMsg()
    data object ClearContext : AgentMsg()
    
    // API responses
    data class HandleApiResponse(
        val response: String, 
        val tokens: Int
    ) : AgentMsg()
    data class HandleApiError(val error: String) : AgentMsg()
    data class HandleStreamChunk(val chunk: StreamChunk) : AgentMsg()
    
    // Context management
    data object LoadContextFromStorage : AgentMsg()
    data class ContextLoaded(val context: AgentContext) : AgentMsg()
    data class ContextSaved(val success: Boolean) : AgentMsg()
    
    // Compression
    data object CompressHistory : AgentMsg()
    data class HistoryCompressed(val summary: String) : AgentMsg()
    
    // Config
    data class UpdateConfig(val config: AgentConfig) : AgentMsg()
}
```

**Responsibilities:**
- Описывают все возможные события в системе
- Typed — компилятор проверяет все cases
- Serialisable — можно логировать, воспроизводить

---

### 3. AgentCmd (Commands / Side Effects)

```kotlin
sealed class AgentCmd {
    // LLM API
    data class CallApi(
        val prompt: String, 
        val context: AgentContext,
        val config: AgentConfig
    ) : AgentCmd()
    
    data class StreamApi(
        val prompt: String,
        val context: AgentContext,
        val config: AgentConfig
    ) : AgentCmd()
    
    // Persistence
    data class SaveContext(val context: AgentContext) : AgentCmd()
    data object LoadContext : AgentCmd()
    data object ClearStorage : AgentCmd()
    
    // Compression
    data class CompressWithLlm(
        val messages: List<ChatMessage>,
        val keepLastN: Int
    ) : AgentCmd()
}
```

**Responsibilities:**
- Описывают side effects (IO, network, etc.)
- Не выполняются в `update()` — только возвращаются
- Выполняются асинхронно в `AgentStore`

---

### 4. update() (Pure Function)

```kotlin
fun update(state: AgentState, msg: AgentMsg): Pair<AgentState, AgentCmd?> {
    return when (msg) {
        is AgentMsg.SendMessage -> {
            val userMessage = ChatMessage(role = "user", content = msg.prompt)
            val newMessages = state.messages + userMessage
            val newState = state.copy(
                messages = newMessages,
                status = AgentStatus.Loading
            )
            val cmd = AgentCmd.StreamApi(
                prompt = msg.prompt,
                context = state.context,
                config = state.config
            )
            newState to cmd
        }
        
        is AgentMsg.HandleStreamChunk -> {
            when (msg.chunk) {
                is StreamChunk.Content -> {
                    val lastMessage = state.messages.lastOrNull()
                    val updatedMessage = if (lastMessage?.role == "assistant" && lastMessage.isStreaming) {
                        lastMessage.copy(content = lastMessage.content + msg.chunk.text)
                    } else {
                        ChatMessage(
                            role = "assistant",
                            content = msg.chunk.text,
                            isStreaming = true
                        )
                    }
                    val newMessages = if (lastMessage?.role == "assistant" && lastMessage.isStreaming) {
                        state.messages.dropLast(1) + updatedMessage
                    } else {
                        state.messages + updatedMessage
                    }
                    state.copy(messages = newMessages) to null
                }
                is StreamChunk.Done -> {
                    val lastMessage = state.messages.lastOrNull()
                    val finalMessage = lastMessage?.copy(isStreaming = false)
                    val newMessages = if (lastMessage != null && finalMessage != null) {
                        state.messages.dropLast(1) + finalMessage
                    } else {
                        state.messages
                    }
                    val newContext = state.context.addExchange(
                        user = state.messages.lastOrNull { it.role == "user" } ?: return state to null,
                        assistant = finalMessage ?: return state to null
                    )
                    val newState = state.copy(
                        messages = newMessages,
                        context = newContext,
                        status = AgentStatus.Idle
                    )
                    val cmd = if (state.config.enablePersistence) {
                        AgentCmd.SaveContext(newContext)
                    } else null
                    newState to cmd
                }
                is StreamChunk.Reasoning -> state to null
            }
        }
        
        is AgentMsg.HandleApiError -> {
            state.copy(
                status = AgentStatus.Error(msg.error),
                errorMessage = msg.error
            ) to null
        }
        
        is AgentMsg.LoadContextFromStorage -> {
            state.copy(status = AgentStatus.Loading) to AgentCmd.LoadContext
        }
        
        is AgentMsg.ContextLoaded -> {
            state.copy(
                context = msg.context,
                status = AgentStatus.Idle
            ) to null
        }
        
        is AgentMsg.CompressHistory -> {
            if (state.messages.size > state.config.keepLastN * 2) {
                state.copy(status = AgentStatus.Loading) to AgentCmd.CompressWithLlm(
                    messages = state.messages,
                    keepLastN = state.config.keepLastN
                )
            } else {
                state to null
            }
        }
        
        is AgentMsg.HistoryCompressed -> {
            val recentMessages = state.messages.takeLast(state.config.keepLastN)
            val summaryMessage = ChatMessage(
                role = "system",
                content = "[Previous conversation summary]\n${msg.summary}"
            )
            val newContext = state.context.copy(
                messages = listOf(summaryMessage) + recentMessages,
                summary = msg.summary
            )
            state.copy(context = newContext) to null
        }
        
        is AgentMsg.ClearContext -> {
            state.copy(
                context = AgentContext(),
                messages = emptyList(),
                metrics = AgentMetrics()
            ) to AgentCmd.ClearStorage
        }
        
        is AgentMsg.UpdateConfig -> {
            state.copy(config = msg.config) to null
        }
        
        is AgentMsg.ContextSaved -> {
            state to null // No state change, just confirmation
        }
    }
}
```

**Responsibilities:**
- Чистая функция: `(state, msg) → (newState, cmd?)`
- Никаких side effects внутри
- Легко тестируется: `update(oldState, msg) == expectedState`

---

### 5. AgentStore (Runtime)

```kotlin
class AgentStore(
    private val llmClient: LlmClient,
    private val storage: ContextStorage,
    private val compressor: ContextCompressor
) {
    private var _state: AgentState = AgentState()
    private val _stateFlow = MutableStateFlow(_state)
    val stateFlow: StateFlow<AgentState> = _stateFlow.asStateFlow()
    
    val state: AgentState get() = _state
    
    fun dispatch(msg: AgentMsg) {
        val (newState, cmd) = update(_state, msg)
        _state = newState
        _stateFlow.value = _state
        
        cmd?.let { executeCmd(it) }
    }
    
    private fun executeCmd(cmd: AgentCmd) {
        CoroutineScope(Dispatchers.Default).launch {
            when (cmd) {
                is AgentCmd.CallApi -> {
                    val result = llmClient.call(
                        prompt = cmd.prompt,
                        context = cmd.context,
                        config = cmd.config
                    )
                    result.fold(
                        onSuccess = { response ->
                            dispatch(AgentMsg.HandleApiResponse(
                                response = response.content,
                                tokens = response.tokensUsed
                            ))
                        },
                        onFailure = { error ->
                            dispatch(AgentMsg.HandleApiError(error.message ?: "Unknown error"))
                        }
                    )
                }
                
                is AgentCmd.StreamApi -> {
                    llmClient.stream(
                        prompt = cmd.prompt,
                        context = cmd.context,
                        config = cmd.config
                    ).collect { chunk ->
                        dispatch(AgentMsg.HandleStreamChunk(chunk))
                    }
                }
                
                is AgentCmd.SaveContext -> {
                    val success = storage.save(cmd.context)
                    dispatch(AgentMsg.ContextSaved(success))
                }
                
                is AgentCmd.LoadContext -> {
                    val context = storage.load()
                    dispatch(AgentMsg.ContextLoaded(context))
                }
                
                is AgentCmd.ClearStorage -> {
                    storage.clear()
                }
                
                is AgentCmd.CompressWithLlm -> {
                    val toCompress = cmd.messages.dropLast(cmd.keepLastN)
                    val summary = compressor.compress(toCompress)
                    dispatch(AgentMsg.HistoryCompressed(summary))
                }
            }
        }
    }
    
    fun init() {
        dispatch(AgentMsg.LoadContextFromStorage)
    }
}
```

**Responsibilities:**
- Хранит текущее состояние
- Обрабатывает `dispatch(msg)`
- Выполняет `Cmd` асинхронно
- Эмитит изменения состояния через `StateFlow`

---

## Domain Models

### AgentContext

```kotlin
data class AgentContext(
    val messages: List<ChatMessage> = emptyList(),
    val summary: String? = null,
    val tokenCount: Int = 0
) {
    val messageCount: Int get() = messages.size
    val totalTokens: Int get() = tokenCount
    
    fun addExchange(user: ChatMessage, assistant: ChatMessage): AgentContext {
        return copy(
            messages = messages + user + assistant,
            tokenCount = tokenCount + estimateTokens(user.content) + estimateTokens(assistant.content)
        )
    }
    
    private fun estimateTokens(text: String): Int =
        text.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
}
```

### AgentConfig

```kotlin
data class AgentConfig(
    val model: String = "glm-5",
    val temperature: Float? = null,
    val maxTokens: Int? = null,
    
    // Persistence
    val enablePersistence: Boolean = false,
    
    // Compression
    val keepLastN: Int = 10,
    val compressThreshold: Int = 20,
    val enableCompression: Boolean = false
)
```

### AgentMetrics

```kotlin
data class AgentMetrics(
    val responseTimeMs: Long = 0,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val contextTokens: Int = 0,
    val compressedTokens: Int? = null,
    val totalRequests: Int = 0
) {
    val totalTokens: Int get() = inputTokens + outputTokens
    val tokensSaved: Int? get() = compressedTokens?.let { contextTokens - it }
    val averageTokensPerRequest: Float get() = 
        if (totalRequests > 0) totalTokens.toFloat() / totalRequests else 0f
    
    fun addResponse(outputTokens: Int, responseTimeMs: Long): AgentMetrics {
        return copy(
            outputTokens = outputTokens + outputTokens,
            totalRequests = totalRequests + 1,
            responseTimeMs = responseTimeMs
        )
    }
}
```

---

## Effect Interfaces (DIP)

### LlmClient

```kotlin
interface LlmClient {
    suspend fun call(
        prompt: String,
        context: AgentContext,
        config: AgentConfig
    ): Result<LlmResponse>
    
    fun stream(
        prompt: String,
        context: AgentContext,
        config: AgentConfig
    ): Flow<StreamChunk>
}

data class LlmResponse(
    val content: String,
    val tokensUsed: Int,
    val model: String
)
```

### ContextStorage

```kotlin
interface ContextStorage {
    fun load(): AgentContext
    fun save(context: AgentContext): Boolean
    fun clear()
}
```

### ContextCompressor

```kotlin
interface ContextCompressor {
    suspend fun compress(messages: List<ChatMessage>): String
}
```

### TokenEstimator

```kotlin
interface TokenEstimator {
    fun estimate(text: String): Int
    fun estimate(messages: List<ChatMessage>): Int
}
```

---

## Integration with Feature Modules

### ChatViewModel Refactor

**Before (MVI with ChatState):**
```kotlin
class ChatViewModel(
    private val repository: ChatRepository,
    private val useCases: ChatUseCases
) {
    private val _uiState = MutableStateFlow(ChatState())
    val uiState: StateFlow<ChatState> = _uiState.asStateFlow()
    
    fun processIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SendMessage -> sendMessage()
            // ...
        }
    }
}
```

**After (Delegates to AgentStore):**
```kotlin
class ChatViewModel(
    private val agentStore: AgentStore
) {
    val state: StateFlow<AgentState> = agentStore.stateFlow
    
    fun processIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SendMessage -> {
                agentStore.dispatch(AgentMsg.SendMessage(state.value.inputText))
            }
            is ChatIntent.ClearChat -> {
                agentStore.dispatch(AgentMsg.ClearContext)
            }
            is ChatIntent.UpdateSettings -> {
                agentStore.dispatch(AgentMsg.UpdateConfig(
                    config = state.value.config.copy(
                        model = intent.settings.model,
                        temperature = intent.settings.temperature,
                        maxTokens = intent.settings.maxTokens,
                        enablePersistence = intent.settings.enablePersistence
                    )
                ))
            }
            // ...
        }
    }
}
```

### Intent → Msg Mapping

```kotlin
fun ChatIntent.toMsg(state: AgentState): AgentMsg? = when (this) {
    is ChatIntent.SendMessage -> AgentMsg.SendMessage(state.inputText)
    is ChatIntent.ClearChat -> AgentMsg.ClearContext
    is ChatIntent.UpdateSettings -> AgentMsg.UpdateConfig(
        config = state.config.copy(/* ... */)
    )
    is ChatIntent.ToggleSettings -> null // UI-only, no state change
    is ChatIntent.ToggleMetrics -> null // UI-only
    else -> null
}
```

---

## File Structure

```
core/agent/
├── src/wasmJsMain/kotlin/agent/
│   │
│   ├── AgentStore.kt              (Runtime, dispatch, executeCmd)
│   ├── AgentState.kt              (Immutable state)
│   ├── AgentMsg.kt                (Messages)
│   ├── AgentCmd.kt                (Commands)
│   ├── AgentUpdate.kt             (Pure update function)
│   │
│   ├── domain/
│   │   ├── AgentContext.kt        (Context model)
│   │   ├── AgentConfig.kt         (Config model)
│   │   └── AgentMetrics.kt        (Metrics model)
│   │
│   └── data/
│       ├── LlmClient.kt           (interface)
│       ├── KtorLlmClient.kt       (impl - wraps ChatClient)
│       ├── ContextStorage.kt      (interface)
│       ├── LocalStorageContextStorage.kt (impl - localStorage)
│       ├── ContextCompressor.kt   (interface)
│       ├── LlmContextCompressor.kt (impl - uses LLM)
│       ├── TokenEstimator.kt      (interface)
│       └── WhitespaceTokenEstimator.kt (impl - simple split)
│
└── build.gradle.kts

feature/chat/
├── src/wasmJsMain/kotlin/chat/
│   ├── ChatViewModel.kt           (Delegates to AgentStore)
│   ├── ChatIntent.kt              (Maps to AgentMsg)
│   ├── ChatIntents.kt             (DELETE - logic in update())
│   ├── ChatState.kt               (DELETE - use AgentState)
│   ├── ChatRepository.kt          (DELETE - logic in AgentStore)
│   ├── ChatUseCases.kt            (DELETE - logic in update())
│   └── ui/
│       ├── App.kt                 (Observes AgentStore.stateFlow)
│       └── components/
│           ├── MessageBubble.kt
│           ├── ChatInput.kt
│           └── ...
```

---

## Testing Strategy

### Unit Tests

```kotlin
class AgentUpdateTest {
    @Test
    fun `SendMessage creates user message and returns CallApi cmd`() {
        val state = AgentState()
        val msg = AgentMsg.SendMessage("Hello")
        
        val (newState, cmd) = update(state, msg)
        
        assertEquals(1, newState.messages.size)
        assertEquals("user", newState.messages.first().role)
        assertEquals("Hello", newState.messages.first().content)
        assertEquals(AgentStatus.Loading, newState.status)
        assertTrue(cmd is AgentCmd.StreamApi)
    }
    
    @Test
    fun `HandleStreamChunk appends content to streaming message`() {
        val state = AgentState(
            messages = listOf(
                ChatMessage(role = "user", content = "Hi"),
                ChatMessage(role = "assistant", content = "Hello", isStreaming = true)
            )
        )
        val msg = AgentMsg.HandleStreamChunk(StreamChunk.Content(" world"))
        
        val (newState, cmd) = update(state, msg)
        
        assertEquals("Hello world", newState.messages.last().content)
        assertTrue(newState.messages.last().isStreaming)
        assertNull(cmd)
    }
}
```

### Integration Tests

```kotlin
class AgentStoreIntegrationTest {
    @Test
    fun `full message cycle`() = runTest {
        val mockLlmClient = MockLlmClient()
        val store = AgentStore(
            llmClient = mockLlmClient,
            storage = InMemoryContextStorage(),
            compressor = MockCompressor()
        )
        
        store.dispatch(AgentMsg.SendMessage("Hello"))
        
        eventually {
            assertTrue(store.state.messages.any { it.role == "assistant" })
            assertEquals(AgentStatus.Idle, store.state.status)
        }
    }
}
```

---

## Advantages Over Current Architecture

| Aspect | Current (MVI + Repository) | AgentStore (TEA) |
|--------|---------------------------|------------------|
| **State Source** | Multiple (ChatState, Agent, Repository) | Single (AgentState) |
| **Logic Location** | Scattered (ViewModel, UseCases, Repository) | Centralized (update function) |
| **Side Effects** | Mixed with logic | Explicit (Cmd) |
| **Testability** | Mock repository | Pure update function |
| **Predictability** | Hard to trace | Easy to trace (msg → state) |
| **Extensibility** | Add to multiple places | Add msg + update case |

---

## Implementation Plan

| Phase | Task | Files | Priority |
|-------|------|-------|----------|
| **1a** | Create AgentState, AgentMsg, AgentCmd | `AgentState.kt`, `AgentMsg.kt`, `AgentCmd.kt` | High |
| **1b** | Implement `update()` function | `AgentUpdate.kt` | High |
| **1c** | Create AgentStore | `AgentStore.kt` | High |
| **2a** | Create effect interfaces | `LlmClient.kt`, `ContextStorage.kt` | High |
| **2b** | Implement KtorLlmClient (wraps ChatClient) | `KtorLlmClient.kt` | High |
| **2c** | Implement LocalStorageContextStorage | `LocalStorageContextStorage.kt` | High |
| **3a** | Refactor ChatViewModel to use AgentStore | `ChatViewModel.kt` | High |
| **3b** | Update UI to observe AgentStore.stateFlow | `App.kt` | High |
| **4a** | Delete deprecated files | ChatRepository, ChatUseCases, ChatState | Medium |
| **5a** | Implement ContextCompressor | `LlmContextCompressor.kt` | Low |
| **5b** | Add compression logic to update | `AgentUpdate.kt` | Low |

---

## Migration Strategy

### Phase 1: Parallel Implementation
- Create AgentStore alongside existing code
- Keep ChatRepository working
- No breaking changes

### Phase 2: Feature Flag
- Add `useAgentStore: Boolean` flag
- ChatViewModel switches between old/new path
- Test both in parallel

### Phase 3: Cutover
- Remove feature flag
- Delete old code
- AgentStore is the only path

---

## References

- [The Elm Architecture](https://guide.elm-lang.org/architecture/)
- [Model-View-Update (MVU) Pattern](https://thomasbandt.com/model-view-update)
- [Cycle.js](https://cycle.js.org/) (similar architecture in JS)
- [Redux](https://redux.js.org/) (inspired by Elm)
