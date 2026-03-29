# Agent Entity Implementation Plan

цель: 
Реализуйте простого агента, который:
👉 принимает запрос пользователя
👉 отправляет его в LLM через API
👉 получает ответ
👉 выводит результат в вашем интерфейсе

(простой чат, CLI или web, запросы через HTTP-клиент)

Важно:
👉 агент должен быть отдельной сущностью, а не просто один вызов API
👉 логика запроса и ответа должна быть инкапсулирована в агенте

Результат:
Агент принимает запрос и корректно вызывает LLM через API  2) Добавьте агенту сохранение контекста:
👉 храните историю диалога (messages) в JSON или SQLite
👉 при перезапуске агента загружайте историю обратно
👉 продолжайте диалог так, как будто агент не выключался

Проверьте на практике:
👉 начните диалог
👉 перезапустите приложение
👉 продолжите диалог и убедитесь, что агент помнит прошлые сообщения

Результат:
Агент, который сохраняет и восстанавливает контекст между запусками  

Добавьте в код агента подсчёт токенов:
👉 для текущего запроса
👉 для всей истории диалога
👉 для ответа модели

Сравните:

👉 короткий диалог
👉 длинный диалог
👉 диалог, который превышает лимит модели

Покажите:

👉 как растёт стоимость/токены по мере диалога
👉 что ломается при переполнении

Результат:

Код, который считает токены и показывает, как они влияют на поведение агента

Реализуйте механизм управления контекстом:

👉 храните последние N сообщений “как есть”
👉 остальное заменяйте summary (например каждые 10 сообщений)
👉 храните summary отдельно и подставляйте его в запрос вместо полной истории

Сравните:

👉 качество ответов без сжатия
👉 качество ответов со сжатием
👉 расход токенов до/после

Результат:

Агент, который работает с компрессией истории и экономит токены

## Overview

This document outlines the plan to enhance the existing `core:agent` module to support:
1. **Task 1**: Simple agent (already implemented ✅)
2. **Task 2**: Context persistence between sessions
3. **Task 3**: Token counting and statistics
4. **Task 4**: Context compression with summarization

## Current State Analysis

### Existing Implementation (`core/agent`)

**Files:**
- `Agent.kt` - Interface with `process()` and `processStreaming()` methods
- `SimpleAgent.kt` - Basic implementation using `ChatClient` (Ktor)
- `AgentModels.kt` - Data models (`AgentContext`, `AgentConfig`, `AgentRequest`, `AgentResult`)
- `AgentFactory.kt` - Factory creating SimpleAgent instances

**Current Capabilities:**
- ✅ Request → API → Response flow
- ✅ Streaming support via `Flow<StreamChunk>`
- ✅ Separate entity with interface
- ✅ Basic error handling

**Current Gaps:**
- ❌ No history persistence
- ❌ No token counting/tracking
- ❌ No context compression
- ❌ No statistics display

## Gap Analysis

### Task 1: Simple Agent
**Status:** ✅ Already implemented

**Verification needed:**
- Test that `SimpleAgent.process()` works correctly
- Verify streaming in `ChatUseCases.executeAgentRequest()`
- Check integration with `ChatViewModel`

**Optional enhancements:**
- Add KDoc documentation
- Add unit tests

### Task 2: Persistence Layer
**Missing:**
- Context storage interface
- JSON-based persistence (localStorage for WASM)
- Load/save history methods
- Context restoration on startup

**Needed Components:**
1. `ContextStorage` interface
2. `JsonContextStorage` implementation
3. Integration with `SimpleAgent`
4. Startup loading logic in `ChatViewModel`

### Task 3: Token Counting
**Missing:**
- Token estimation utility
- Statistics tracking
- UI display for metrics

**Needed Components:**
1. `TokenCounter` utility object
2. `TokenStats` data class
3. Stats tracking in `SimpleAgent`
4. `TokenStatsPanel` UI component

### Task 4: Context Compression
**Missing:**
- Compression configuration
- Summary generation
- Context size management
- Compression orchestration

**Needed Components:**
1. `ContextCompressionConfig` data class
2. `Summarizer` interface + LLM implementation
3. `ContextManager` orchestrator
4. Compression triggers
5. UI controls

## Architecture Design

### Module Structure

```
core/agent/
├── src/wasmJsMain/kotlin/agent/
│   ├── Agent.kt                      (existing, 10 lines)
│   ├── SimpleAgent.kt                (existing, 91 lines → enhance to ~200)
│   ├── AgentFactory.kt               (existing, 7 lines)
│   ├── AgentModels.kt                (existing, 19 lines → enhance to ~50)
│   │
│   ├── storage/
│   │   ├── ContextStorage.kt         (new, ~20 lines)
│   │   └── JsonContextStorage.kt     (new, ~60 lines)
│   │
│   ├── stats/
│   │   ├── TokenStats.kt             (new, ~30 lines)
│   │   └── TokenCounter.kt           (new, ~40 lines)
│   │
│   ├── compression/
│   │   ├── ContextCompression.kt     (new, ~50 lines)
│   │   ├── Summarizer.kt             (new, ~80 lines)
│   │   └── ContextManager.kt         (new, ~100 lines)
│   │
│   └── context/
│       ├── AgentContext.kt           (enhance, ~25 lines)
│       ├── AgentConfig.kt            (enhance, ~20 lines)
│       └── AgentRequest.kt           (enhance, ~10 lines)
```

### Integration with Chat Feature

```
feature/chat/
├── ChatViewModel.kt                  (inject ContextManager, ~5 lines)
├── ChatState.kt                      (add compression state, ~10 lines)
├── ChatUseCases.kt                   (enhance agent usage, ~20 lines)
└── ui/components/
    └── TokenStatsPanel.kt            (new, ~100 lines)
```

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        ChatViewModel                         │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                    ChatUseCases                        │ │
│  │  ┌──────────────────────────────────────────────────┐  │ │
│  │  │                     Agent                         │  │ │
│  │  │  ┌─────────────────────────────────────────────┐  │  │ │
│  │  │  │              SimpleAgent                    │  │  │ │
│  │  │  │  ┌────────────────────────────────────────┐ │  │  │ │
│  │  │  │  │           ChatClient (Ktor)            │ │  │  │ │
│  │  │  │  └────────────────────────────────────────┘ │  │  │ │
│  │  │  │                                             │  │  │ │
│  │  │  │  ┌──────────────┐  ┌─────────────────────┐  │  │  │ │
│  │  │  │  │ContextManager│  │   TokenCounter      │  │  │  │ │
│  │  │  │  └──────────────┘  └─────────────────────┘  │  │  │ │
│  │  │  │         │                                     │  │  │ │
│  │  │  │         ├──────────────┐                      │  │  │ │
│  │  │  │         │              │                      │  │  │ │
│  │  │  │  ┌──────▼──────┐  ┌───▼────────┐             │  │  │ │
│  │  │  │  │ Summarizer  │  │  Storage   │             │  │  │ │
│  │  │  │  └─────────────┘  └────────────┘             │  │  │ │
│  │  │  └─────────────────────────────────────────────────┘  │ │
│  │  └──────────────────────────────────────────────────────┘ │
│  └──────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Component Specifications

### 1. Storage Layer (Task 2)

#### ContextStorage Interface
```kotlin
package agent.storage

import agent.context.AgentContext

interface ContextStorage {
    fun load(): AgentContext?
    fun save(context: AgentContext)
    fun clear()
}
```

#### JsonContextStorage Implementation
```kotlin
package agent.storage

import agent.context.AgentContext
import kotlinx.browser.localStorage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.ChatMessage

class JsonContextStorage(
    private val key: String = "agent_history"
) : ContextStorage {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    override fun load(): AgentContext? {
        val stored = localStorage.getItem(key) ?: return null
        
        return try {
            val messages = json.decodeFromString<List<ChatMessage>>(stored)
            AgentContext(messages = messages)
        } catch (e: Exception) {
            null
        }
    }
    
    override fun save(context: AgentContext) {
        val jsonStr = json.encodeToString(context.messages)
        localStorage.setItem(key, jsonStr)
    }
    
    override fun clear() {
        localStorage.removeItem(key)
    }
}
```

### 2. Token Statistics (Task 3)

#### TokenCounter Utility
```kotlin
package agent.stats

import model.ChatMessage

object TokenCounter {
    fun countTokens(text: String): Int {
        return text.split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .size
    }
    
    fun estimateTokens(messages: List<ChatMessage>): Int {
        return messages.sumOf { countTokens(it.content) }
    }
    
    fun calculateStats(
        messages: List<ChatMessage>,
        response: String,
        responseTimeMs: Long
    ): TokenStats {
        val promptTokens = estimateTokens(messages)
        val completionTokens = countTokens(response)
        
        return TokenStats(
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = promptTokens + completionTokens,
            responseTimeMs = responseTimeMs
        )
    }
}
```

#### TokenStats Data Class
```kotlin
package agent.stats

data class TokenStats(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val responseTimeMs: Long = 0L,
    val contextSize: Int = 0,
    val compressionRatio: Double? = null
) {
    val tokensPerSecond: Double
        get() = if (responseTimeMs > 0) {
            completionTokens.toDouble() / (responseTimeMs / 1000.0)
        } else 0.0
    
    val efficiency: Double
        get() = if (promptTokens > 0) {
            completionTokens.toDouble() / promptTokens
        } else 0.0
}
```

### 3. Context Compression (Task 4)

#### ContextCompressionConfig
```kotlin
package agent.compression

data class ContextCompressionConfig(
    val enabled: Boolean = false,
    val keepLastN: Int = 10,
    val compressThreshold: Int = 20,
    val maxContextSize: Int? = null,
    val generateSummary: Boolean = true
)
```

#### Summarizer Interface
```kotlin
package agent.compression

import agent.Agent
import agent.context.AgentContext
import agent.context.AgentRequest
import model.ChatMessage

interface Summarizer {
    suspend fun summarize(messages: List<ChatMessage>): String
}

class LlmSummarizer(
    private val agent: Agent
) : Summarizer {
    
    override suspend fun summarize(messages: List<ChatMessage>): String {
        if (messages.isEmpty()) return ""
        
        val conversationText = messages.joinToString("\n") { msg ->
            "${msg.role}: ${msg.content}"
        }
        
        val summaryPrompt = """
            Summarize the following conversation concisely, 
            preserving key information and context:
            
            $conversationText
            
            Summary:
        """.trimIndent()
        
        val request = AgentRequest(
            prompt = summaryPrompt,
            context = AgentContext(messages = emptyList()),
            config = null
        )
        
        return when (val result = agent.process(request)) {
            is agent.AgentResult.Success -> result.response
            else -> "Failed to generate summary"
        }
    }
}
```

#### ContextManager Orchestrator
```kotlin
package agent.compression

import agent.context.AgentContext
import agent.storage.ContextStorage
import agent.stats.TokenCounter
import model.ChatMessage

class ContextManager(
    private val storage: ContextStorage,
    private val summarizer: Summarizer?,
    private val config: ContextCompressionConfig
) {
    
    fun loadContext(): AgentContext {
        val context = storage.load() ?: AgentContext()
        
        return if (config.enabled && shouldCompress(context)) {
            compressContext(context)
        } else {
            context
        }
    }
    
    fun saveContext(context: AgentContext) {
        val contextToSave = if (config.enabled && shouldCompress(context)) {
            compressContext(context)
        } else {
            context
        }
        storage.save(contextToSave)
    }
    
    private fun shouldCompress(context: AgentContext): Boolean {
        return context.messages.size >= config.compressThreshold
    }
    
    private suspend fun compressContext(context: AgentContext): AgentContext {
        if (context.messages.isEmpty()) return context
        
        val keepLast = context.messages.takeLast(config.keepLastN)
        val toCompress = context.messages.dropLast(config.keepLastN)
        
        if (toCompress.isEmpty()) return context
        
        val summary = if (config.generateSummary && summarizer != null) {
            summarizer.summarize(toCompress)
        } else {
            "Previous context: ${toCompress.size} messages"
        }
        
        val summaryMessage = ChatMessage(
            role = "system",
            content = "[Previous conversation summary]\n$summary"
        )
        
        return context.copy(
            messages = listOf(summaryMessage) + keepLast
        )
    }
    
    fun getCompressionStats(context: AgentContext): CompressionStats {
        val originalTokens = TokenCounter.estimateTokens(context.messages)
        val originalSize = context.messages.size
        
        return CompressionStats(
            originalSize = originalSize,
            originalTokens = originalTokens,
            compressedSize = if (shouldCompress(context)) config.keepLastN + 1 else originalSize,
            compressionEnabled = config.enabled
        )
    }
}

data class CompressionStats(
    val originalSize: Int,
    val originalTokens: Int,
    val compressedSize: Int,
    val compressionEnabled: Boolean
)
```

### 4. Enhanced AgentModels

#### Enhanced AgentContext
```kotlin
package agent.context

import agent.compression.ContextCompressionConfig
import agent.stats.TokenStats
import model.ChatMessage

data class AgentContext(
    val messages: List<ChatMessage> = emptyList(),
    val tokenStats: TokenStats? = null,
    val compressionConfig: ContextCompressionConfig? = null
) {
    val totalTokens: Int
        get() = messages.sumOf { it.tokensUsed ?: 0 }
    
    val messageCount: Int
        get() = messages.size
}
```

#### Enhanced AgentConfig
```kotlin
package agent.context

data class AgentConfig(
    val model: String = "glm-5",
    val temperature: Float? = null,
    val maxTokens: Int? = null,
    val systemPrompt: String? = null,
    val enablePersistence: Boolean = false,
    val enableCompression: Boolean = false,
    val compressionConfig: ContextCompressionConfig? = null
)
```

### 5. Enhanced SimpleAgent

```kotlin
package agent

import agent.compression.ContextManager
import agent.context.AgentContext
import agent.context.AgentRequest
import agent.stats.TokenCounter
import agent.stats.TokenStats
import agent.storage.ContextStorage
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import model.ChatMessage
import model.StreamChunk
import network.ChatClient
import network.ResponseConstraints

class SimpleAgent(
    private val chatClient: ChatClient,
    private val apiKey: String,
    private val storage: ContextStorage? = null,
    private val contextManager: ContextManager? = null
) : Agent {
    
    override suspend fun process(
        request: AgentRequest,
        onChunk: (StreamChunk) -> Unit
    ): AgentResult {
        val startTime = getTimeMillis()
        val responseBuilder = StringBuilder()
        var error: Throwable? = null
        
        try {
            val messages = buildMessages(request)
            val constraints = buildConstraints(request.config)
            val model = request.config?.model ?: "glm-5"
            
            chatClient.sendMessageStreaming(
                apiKey = apiKey,
                model = model,
                messages = messages,
                constraints = constraints
            ).collect { chunk ->
                onChunk(chunk)
                when (chunk) {
                    is StreamChunk.Content -> responseBuilder.append(chunk.text)
                    is StreamChunk.Reasoning -> Unit
                    is StreamChunk.Done -> Unit
                }
            }
        } catch (e: Exception) {
            error = e
        }
        
        val responseTimeMs = getTimeMillis() - startTime
        val response = responseBuilder.toString()
        
        if (error != null) {
            return AgentResult.Error(
                message = error.message ?: "Unknown error",
                cause = error
            )
        }
        
        val stats = TokenCounter.calculateStats(
            messages = request.context?.messages ?: emptyList(),
            response = response,
            responseTimeMs = responseTimeMs
        )
        
        if (request.config?.enablePersistence == true && storage != null) {
            val updatedContext = AgentContext(
                messages = (request.context?.messages ?: emptyList()) + 
                    listOf(ChatMessage(role = "user", content = request.prompt)) +
                    listOf(ChatMessage(role = "assistant", content = response))
            )
            storage.save(updatedContext)
        }
        
        return AgentResult.Success(
            response = response,
            metrics = AgentMetrics(
                responseTimeMs = responseTimeMs,
                inputTokens = stats.promptTokens,
                outputTokens = stats.completionTokens
            )
        )
    }
    
    override fun processStreaming(request: AgentRequest): Flow<StreamChunk> {
        val messages = buildMessages(request)
        val constraints = buildConstraints(request.config)
        val model = request.config?.model ?: "glm-5"
        
        return chatClient.sendMessageStreaming(
            apiKey = apiKey,
            model = model,
            messages = messages,
            constraints = constraints
        ).catch { e ->
            emit(StreamChunk.Done)
        }
    }
    
    fun loadContext(): AgentContext? {
        return contextManager?.loadContext()
    }
    
    fun saveContext(context: AgentContext) {
        contextManager?.saveContext(context)
    }
    
    private fun buildMessages(request: AgentRequest): List<ChatMessage> {
        val contextMessages = request.context?.messages ?: emptyList()
        return contextMessages + ChatMessage(
            role = "user",
            content = request.prompt
        )
    }
    
    private fun buildConstraints(config: AgentConfig?): ResponseConstraints {
        return ResponseConstraints(
            temperature = config?.temperature?.toDouble(),
            maxTokens = config?.maxTokens
        )
    }
}
```

### 6. Enhanced AgentFactory

```kotlin
package agent

import agent.compression.ContextCompressionConfig
import agent.compression.ContextManager
import agent.compression.LlmSummarizer
import agent.storage.ContextStorage
import agent.storage.JsonContextStorage
import network.ChatClient

object AgentFactory {
    
    fun create(
        chatClient: ChatClient,
        apiKey: String,
        enablePersistence: Boolean = false,
        enableCompression: Boolean = false,
        compressionConfig: ContextCompressionConfig? = null
    ): Agent {
        val storage = if (enablePersistence) {
            JsonContextStorage()
        } else {
            null
        }
        
        val contextManager = if (enableCompression && storage != null && compressionConfig != null) {
            val summarizer = LlmSummarizer(SimpleAgent(chatClient, apiKey))
            ContextManager(storage, summarizer, compressionConfig)
        } else {
            null
        }
        
        return SimpleAgent(
            chatClient = chatClient,
            apiKey = apiKey,
            storage = storage,
            contextManager = contextManager
        )
    }
}
```

## Implementation Order

### Phase 1: Core Enhancements (Tasks 1-3)

**Step 1.1: Storage Layer**
1. Create `storage/ContextStorage.kt` (interface)
2. Create `storage/JsonContextStorage.kt` (implementation)
3. Test JSON serialization/deserialization
4. Test localStorage integration

**Step 1.2: Token Statistics**
1. Create `stats/TokenStats.kt` (data class)
2. Create `stats/TokenCounter.kt` (utility)
3. Add token tracking to `SimpleAgent`
4. Update `AgentMetrics` to include tokens
5. Test token counting accuracy

**Step 1.3: Enhanced Models**
1. Enhance `AgentContext` with token stats
2. Enhance `AgentConfig` with persistence flags
3. Update `AgentModels.kt`

**Step 1.4: Agent Integration**
1. Update `SimpleAgent` with storage support
2. Update `AgentFactory` with new parameters
3. Test persistence flow

### Phase 2: Compression (Task 4)

**Step 2.1: Compression Infrastructure**
1. Create `compression/ContextCompressionConfig.kt`
2. Create `compression/Summarizer.kt` (interface + implementation)
3. Create `compression/ContextManager.kt`
4. Test compression logic

**Step 2.2: Agent Enhancement**
1. Add `ContextManager` to `SimpleAgent`
2. Add compression methods
3. Test compression flow

**Step 2.3: Integration**
1. Update `AgentFactory` with compression support
2. Test end-to-end compression

### Phase 3: UI Integration

**Step 3.1: ChatState Updates**
1. Add compression state fields to `ChatState`
2. Add token stats fields
3. Add persistence state

**Step 3.2: ChatViewModel Updates**
1. Inject `ContextManager` into ViewModel
2. Add compression control methods
3. Add persistence control methods

**Step 3.3: UI Components**
1. Create `TokenStatsPanel.kt` component
2. Add compression toggle to settings
3. Add persistence toggle to settings
4. Integrate with `App.kt`

**Step 3.4: Testing**
1. Write unit tests for all new components
2. Write integration tests
3. Write UI tests

## Testing Strategy

### Unit Tests

**Storage Tests:**
- `JsonContextStorageTest`
  - Test save/load roundtrip
  - Test empty context handling
  - Test error handling for corrupted JSON

**Token Counter Tests:**
- `TokenCounterTest`
  - Test token counting accuracy
  - Test empty string handling
  - Test special characters
  - Test multi-line text

**Compression Tests:**
- `ContextManagerTest`
  - Test compression trigger logic
  - Test keepLastN logic
  - Test summary generation
  - Test compression stats

**Agent Tests:**
- `SimpleAgentTest`
  - Test persistence integration
  - Test token tracking
  - Test compression integration
  - Test error handling

### Integration Tests

- `AgentPersistenceTest`
  - Test full save/load cycle
  - Test context restoration on startup

- `AgentCompressionTest`
  - Test compression with real LLM
  - Test compression quality
  - Test token savings

### UI Tests

- `TokenStatsPanelTest`
  - Test display of token statistics
  - Test compression stats display

- `SettingsDialogTest`
  - Test compression toggle
  - Test persistence toggle

## Success Criteria

### Task 1: Simple Agent
- ✅ Agent accepts user query
- ✅ Agent sends to LLM via API
- ✅ Agent returns response
- ✅ Agent is separate entity (interface + implementation)

### Task 2: Persistence
- ✅ History saves to localStorage
- ✅ History loads on startup
- ✅ Context restored correctly
- ✅ Dialog continues after restart

### Task 3: Token Counting
- ✅ Token count for current request
- ✅ Token count for full history
- ✅ Token count for response
- ✅ Statistics displayed in UI
- ✅ Growth tracked over conversation
- ✅ Overflow behavior tested

### Task 4: Compression
- ✅ Last N messages kept as-is
- ✅ Older messages replaced with summary
- ✅ Summary stored separately
- ✅ Summary substituted in request
- ✅ Quality comparison (with/without compression)
- ✅ Token savings demonstrated

## File Checklist

### New Files to Create

| File Path | Lines | Priority |
|-----------|-------|----------|
| `core/agent/src/wasmJsMain/kotlin/agent/storage/ContextStorage.kt` | ~20 | High |
| `core/agent/src/wasmJsMain/kotlin/agent/storage/JsonContextStorage.kt` | ~60 | High |
| `core/agent/src/wasmJsMain/kotlin/agent/stats/TokenStats.kt` | ~30 | High |
| `core/agent/src/wasmJsMain/kotlin/agent/stats/TokenCounter.kt` | ~40 | High |
| `core/agent/src/wasmJsMain/kotlin/agent/compression/ContextCompression.kt` | ~20 | Medium |
| `core/agent/src/wasmJsMain/kotlin/agent/compression/Summarizer.kt` | ~80 | Medium |
| `core/agent/src/wasmJsMain/kotlin/agent/compression/ContextManager.kt` | ~100 | Medium |
| `feature/chat/src/wasmJsMain/kotlin/chat/ui/components/TokenStatsPanel.kt` | ~100 | Low |

### Files to Modify

| File Path | Changes | Priority |
|-----------|---------|----------|
| `core/agent/src/wasmJsMain/kotlin/agent/SimpleAgent.kt` | Add persistence, token tracking | High |
| `core/agent/src/wasmJsMain/kotlin/agent/AgentFactory.kt` | Add new parameters | High |
| `core/agent/src/wasmJsMain/kotlin/agent/AgentModels.kt` | Enhance models | High |
| `feature/chat/src/wasmJsMain/kotlin/chat/ChatState.kt` | Add compression state | Medium |
| `feature/chat/src/wasmJsMain/kotlin/chat/ChatViewModel.kt` | Inject ContextManager | Medium |
| `feature/chat/src/wasmJsMain/kotlin/chat/ChatUseCases.kt` | Add compression support | Medium |
| `feature/chat/src/wasmJsMain/kotlin/chat/ui/App.kt` | Add TokenStatsPanel | Low |

## Estimated Effort

| Phase | Tasks | Estimated Time |
|-------|-------|----------------|
| Phase 1 | Storage + Token Counting | 4-6 hours |
| Phase 2 | Compression Infrastructure | 4-6 hours |
| Phase 3 | UI Integration | 3-4 hours |
| Testing | Unit + Integration Tests | 3-4 hours |
| **Total** | | **14-20 hours** |

## Next Steps

1. **Review this plan** and approve architecture decisions
2. **Start with Phase 1** (Storage + Token Counting)
3. **Test incrementally** after each component
4. **Proceed to Phase 2** after Phase 1 is stable
5. **Complete Phase 3** for full UI integration
6. **Write tests** alongside implementation

## Questions to Resolve

1. **Token Estimation Method**: Use simple whitespace splitting or more sophisticated tokenizer?
   - Simple: `text.split(Regex("\\s+")).size`
   - Sophisticated: Implement BPE tokenizer
   
2. **Compression Trigger**: Automatic vs manual?
   - Automatic: Trigger when threshold reached
   - Manual: User clicks "Compress" button
   
3. **Summary Quality**: LLM-generated vs simple truncation?
   - LLM: Use agent itself to generate summary
   - Truncation: Keep first sentence of each message
   
4. **Storage Key Strategy**: Single key vs multiple keys?
   - Single: All history in one JSON
   - Multiple: Separate keys for different sessions

5. **Context Limit Handling**: Soft warning vs hard error?
   - Soft: Show warning, allow continue
   - Hard: Block request, force compression

## Conclusion

This plan provides a comprehensive roadmap for implementing the agent entity with all four tasks. The architecture is modular, allowing for incremental implementation and testing. The use of existing patterns (MVI, Repository) ensures consistency with the current codebase.
