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

👉 храните последние N сообщений "как есть"
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
4. **Task 4**: Context compression with LLM summarization

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
**Status:** ✅ COMPLETED (2026-03-29)

**Implementation:**
- ✅ `core/agent` module created with KMP/WASM support
- ✅ `Agent.kt` interface with `process()` and `processStreaming()` methods
- ✅ `SimpleAgent.kt` implementation wrapping `ChatClient`
- ✅ `AgentModels.kt` with data classes
- ✅ `AgentFactory.kt` for creating Agent instances
- ✅ Integration with `ChatUseCases.executeAgentRequest()`
- ✅ `ChatViewModel` creates Agent via `AgentFactory`

**Verification completed:**
- ✅ Compilation: SUCCESS
- ✅ Tests: 738/750 PASS (12 failures pre-existing, unrelated)
- ✅ Lint: PASS
- ✅ `./gradlew check`: SUCCESS

**Branch:** feature/agent-entity-2026-03-29
**Commit:** a17c26f

### Task 2: Persistence Layer
**Missing:**
- Context storage interface + implementation
- JSON-based persistence (localStorage for WASM)
- Load/save history methods
- Context restoration on startup

### Task 3: Token Counting
**Missing:**
- Token estimation utility (whitespace split)
- Statistics tracking in AgentMetrics
- UI display for metrics

### Task 4: Context Compression
**Missing:**
- Summarizer interface + LLM implementation
- Compression logic (keepLastN + summary)
- Compression triggers

---

## Architecture Design (Simplified)

### Design Principles Applied

| Principle | Application |
|-----------|-------------|
| **SOLID** | SRP: Each file has single responsibility. DIP: Interfaces for Storage, Summarizer |
| **KISS** | Flat package structure (no subdirectories). No unnecessary layers |
| **DRY** | TokenCounter utility reused. Single storage implementation |
| **YAGNI** | No ContextManager orchestrator. No premature abstractions |

### Module Structure

```
core/agent/src/wasmJsMain/kotlin/agent/
├── Agent.kt              (interface - keep, ~10 lines)
├── SimpleAgent.kt        (API calls + persistence + compression, ~150 lines)
├── AgentModels.kt        (all data classes, ~80 lines)
├── AgentStorage.kt       (interface + JsonContextStorage in one file, ~60 lines)
├── TokenCounter.kt       (utility object, ~30 lines)
└── Summarizer.kt         (interface + LlmSummarizer, ~60 lines)
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
│  │  │  │  │ TokenCounter │  │   AgentStorage      │  │  │  │ │
│  │  │  │  └──────────────┘  └─────────────────────┘  │  │  │ │
│  │  │  │         │                                     │  │  │ │
│  │  │  │         ▼                                     │  │  │ │
│  │  │  │  ┌──────────────┐                            │  │  │ │
│  │  │  │  │  Summarizer  │                            │  │  │ │
│  │  │  │  └─────────────┘                            │  │  │ │
│  │  │  └─────────────────────────────────────────────────┘  │ │
│  │  └──────────────────────────────────────────────────────┘ │
│  └──────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## Component Specifications

### 1. AgentStorage (Task 2)

```kotlin
package agent

import kotlinx.browser.localStorage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.ChatMessage

interface AgentStorage {
    fun load(): List<ChatMessage>
    fun save(messages: List<ChatMessage>)
    fun clear()
}

class JsonAgentStorage(
    private val key: String = "agent_history"
) : AgentStorage {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    override fun load(): List<ChatMessage> {
        val stored = localStorage.getItem(key) ?: return emptyList()
        return try {
            json.decodeFromString(stored)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override fun save(messages: List<ChatMessage>) {
        localStorage.setItem(key, json.encodeToString(messages))
    }
    
    override fun clear() {
        localStorage.removeItem(key)
    }
}
```

### 2. TokenCounter (Task 3)

```kotlin
package agent

import model.ChatMessage

object TokenCounter {
    fun count(text: String): Int =
        text.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
    
    fun estimate(messages: List<ChatMessage>): Int =
        messages.sumOf { count(it.content) }
}
```

### 3. Summarizer (Task 4)

```kotlin
package agent

import model.ChatMessage

interface Summarizer {
    suspend fun summarize(messages: List<ChatMessage>): String
}

class LlmSummarizer(
    private val chatClient: network.ChatClient,
    private val apiKey: String,
    private val model: String = "glm-5"
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
        
        return chatClient.sendMessage(
            apiKey = apiKey,
            model = model,
            messages = listOf(model.ChatMessage(role = "user", content = summaryPrompt)),
            constraints = null
        )
    }
}
```

### 4. Enhanced AgentModels

```kotlin
package agent

import model.ChatMessage

data class AgentContext(
    val messages: List<ChatMessage> = emptyList()
) {
    val messageCount: Int get() = messages.size
}

data class AgentConfig(
    val model: String = "glm-5",
    val temperature: Float? = null,
    val maxTokens: Int? = null,
    val systemPrompt: String? = null,
    val enablePersistence: Boolean = false,
    val keepLastN: Int? = null,
    val compressThreshold: Int? = null
)

data class AgentRequest(
    val prompt: String,
    val context: AgentContext? = null,
    val config: AgentConfig? = null
)

data class AgentMetrics(
    val responseTimeMs: Long = 0,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val totalTokens: Int = inputTokens + outputTokens
)

sealed class AgentResult {
    data class Success(
        val response: String,
        val metrics: AgentMetrics = AgentMetrics()
    ) : AgentResult()
    
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : AgentResult()
}
```

### 5. Enhanced SimpleAgent

```kotlin
package agent

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
    private val storage: AgentStorage? = null,
    private val summarizer: Summarizer? = null
) : Agent {
    
    override suspend fun process(
        request: AgentRequest,
        onChunk: (StreamChunk) -> Unit
    ): AgentResult {
        val startTime = getTimeMillis()
        val responseBuilder = StringBuilder()
        var error: Throwable? = null
        
        val contextMessages = request.context?.messages ?: storage?.load() ?: emptyList()
        val messages = buildMessages(request, contextMessages)
        val constraints = buildConstraints(request.config)
        val model = request.config?.model ?: "glm-5"
        
        try {
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
        
        val inputTokens = TokenCounter.estimate(messages)
        val outputTokens = TokenCounter.count(response)
        
        if (request.config?.enablePersistence == true && storage != null) {
            val updatedMessages = contextMessages + 
                ChatMessage(role = "user", content = request.prompt) +
                ChatMessage(role = "assistant", content = response)
            
            val messagesToSave = compressIfNeeded(updatedMessages, request.config)
            storage.save(messagesToSave)
        }
        
        return AgentResult.Success(
            response = response,
            metrics = AgentMetrics(
                responseTimeMs = responseTimeMs,
                inputTokens = inputTokens,
                outputTokens = outputTokens
            )
        )
    }
    
    override fun processStreaming(request: AgentRequest): Flow<StreamChunk> {
        val contextMessages = request.context?.messages ?: storage?.load() ?: emptyList()
        val messages = buildMessages(request, contextMessages)
        val constraints = buildConstraints(request.config)
        val model = request.config?.model ?: "glm-5"
        
        return chatClient.sendMessageStreaming(
            apiKey = apiKey,
            model = model,
            messages = messages,
            constraints = constraints
        ).catch { emit(StreamChunk.Done) }
    }
    
    fun loadContext(): AgentContext {
        val messages = storage?.load() ?: emptyList()
        return AgentContext(messages = messages)
    }
    
    fun clearContext() {
        storage?.clear()
    }
    
    private fun buildMessages(request: AgentRequest, contextMessages: List<ChatMessage>): List<ChatMessage> =
        contextMessages + ChatMessage(role = "user", content = request.prompt)
    
    private fun buildConstraints(config: AgentConfig?): ResponseConstraints =
        ResponseConstraints(
            temperature = config?.temperature?.toDouble(),
            maxTokens = config?.maxTokens
        )
    
    private suspend fun compressIfNeeded(
        messages: List<ChatMessage>,
        config: AgentConfig
    ): List<ChatMessage> {
        val threshold = config.compressThreshold ?: return messages
        val keepLastN = config.keepLastN ?: return messages
        
        if (messages.size < threshold) return messages
        
        val toCompress = messages.dropLast(keepLastN)
        if (toCompress.isEmpty()) return messages
        
        val summary = summarizer?.summarize(toCompress) ?: return messages
        
        val summaryMessage = ChatMessage(
            role = "system",
            content = "[Previous conversation summary]\n$summary"
        )
        
        return listOf(summaryMessage) + messages.takeLast(keepLastN)
    }
}
```

### 6. Enhanced AgentFactory

```kotlin
package agent

import network.ChatClient

object AgentFactory {
    
    fun create(
        chatClient: ChatClient,
        apiKey: String,
        enablePersistence: Boolean = false,
        summarizer: Summarizer? = null
    ): Agent {
        val storage = if (enablePersistence) JsonAgentStorage() else null
        
        return SimpleAgent(
            chatClient = chatClient,
            apiKey = apiKey,
            storage = storage,
            summarizer = summarizer
        )
    }
}
```

---

## Implementation Order (Refined)

| Phase | Task | Effort | Rationale |
|-------|------|--------|-----------|
| **1a** | Create `AgentStorage.kt` (interface + impl in one file) | 1h | Minimal, focused change |
| **1b** | Create `TokenCounter.kt` utility object | 0.5h | Single responsibility, no dependencies |
| **1c** | Wire storage into `SimpleAgent` (save after response) | 1h | Focused integration |
| **1d** | Load history on startup in `ChatViewModel` | 0.5h | Feature-level change |
| **2a** | Add token stats to `AgentMetrics` | 0.5h | Extend existing model |
| **2b** | Display token stats in `ChatState` + UI | 1h | Feature integration |
| **3a** | Create `Summarizer.kt` (interface + LlmSummarizer) | 1.5h | Compression support |
| **3b** | Add compression logic to `SimpleAgent.compressIfNeeded()` | 1h | No new components needed |
| **4** | UI controls for compression settings | 1h | Settings integration |

**Total: 8-10 hours** (vs 14-20 in original plan)

---

## Testing Strategy

### Unit Tests

- `AgentStorageTest` - save/load roundtrip, empty handling, corrupted JSON
- `TokenCounterTest` - counting accuracy, empty string, special chars, multi-line
- `SimpleAgentTest` - persistence integration, token tracking, compression

### Integration Tests

- `AgentPersistenceTest` - full save/load cycle, context restoration
- `AgentCompressionTest` - compression with real LLM, token savings

---

## Success Criteria

### Task 1: Simple Agent ✅
- ✅ Agent accepts user query
- ✅ Agent sends to LLM via API
- ✅ Agent returns response
- ✅ Agent is separate entity (interface + implementation)

### Task 2: Persistence
- [ ] History saves to localStorage
- [ ] History loads on startup
- [ ] Context restored correctly
- [ ] Dialog continues after restart

### Task 3: Token Counting
- [ ] Token count for current request
- [ ] Token count for full history
- [ ] Token count for response
- [ ] Statistics displayed in UI
- [ ] Growth tracked over conversation

### Task 4: Compression
- [ ] Last N messages kept as-is
- [ ] Older messages replaced with LLM summary
- [ ] Summary substituted in request
- [ ] Token savings demonstrated

---

## File Checklist

### New Files to Create

| File Path | Lines | Priority |
|-----------|-------|----------|
| `core/agent/src/wasmJsMain/kotlin/agent/AgentStorage.kt` | ~60 | High |
| `core/agent/src/wasmJsMain/kotlin/agent/TokenCounter.kt` | ~30 | High |
| `core/agent/src/wasmJsMain/kotlin/agent/Summarizer.kt` | ~60 | Medium |

### Files to Modify

| File Path | Changes | Priority |
|-----------|---------|----------|
| `core/agent/src/wasmJsMain/kotlin/agent/SimpleAgent.kt` | Add persistence, token tracking, compression | High |
| `core/agent/src/wasmJsMain/kotlin/agent/AgentFactory.kt` | Add new parameters | High |
| `core/agent/src/wasmJsMain/kotlin/agent/AgentModels.kt` | Enhance models | High |
| `feature/chat/src/wasmJsMain/kotlin/chat/ChatState.kt` | Add token stats | Medium |
| `feature/chat/src/wasmJsMain/kotlin/chat/ChatViewModel.kt` | Load context on init | Medium |

---

## Decisions Made

| Question | Decision | Rationale |
|----------|----------|-----------|
| **Token Estimation** | Simple whitespace split | WASM-compatible, sufficient for demo |
| **Compression Trigger** | Automatic (threshold-based) | Simpler UX, no manual intervention |
| **Summary Generation** | LLM summarization | User requested, better quality |
| **Storage Key** | Single key | Simpler implementation |
| **Context Limit** | Soft warning | Allow user to continue |

---

## Conclusion

This simplified plan reduces complexity while maintaining all required functionality:
- **3 new files** instead of 8
- **Flat package structure** instead of 4 subdirectories
- **8-10 hours** instead of 14-20
- **No ContextManager** - logic in SimpleAgent
- **No separate TokenStats** - merged into AgentMetrics

---

## Implementation Log

### Day 6 - Task 1: Simple Agent ✅ COMPLETED

**Date:** 2026-03-29
**Branch:** feature/agent-entity-2026-03-29
**Commit:** a17c26f

**What was implemented:**
- ✅ Created `core/agent` module with KMP/WASM support
- ✅ `Agent.kt` interface with `process()` and `processStreaming()` methods
- ✅ `AgentModels.kt` with `AgentRequest`, `AgentResult`, `AgentConfig`, `AgentMetrics`, `AgentContext`
- ✅ `SimpleAgent.kt` implementation wrapping `ChatClient`
- ✅ `AgentFactory.kt` for creating Agent instances
- ✅ Integration with `feature/chat`:
  - `ChatUseCases.executeAgentRequest()` uses Agent
  - `ChatViewModel` creates Agent via `AgentFactory`

**Architecture:**
```
feature/chat (ChatUseCases) → core/agent (SimpleAgent) → core/network (ChatClient) → API
```

**Files created:**
- core/agent/build.gradle.kts
- core/agent/src/wasmJsMain/kotlin/agent/Agent.kt
- core/agent/src/wasmJsMain/kotlin/agent/AgentModels.kt
- core/agent/src/wasmJsMain/kotlin/agent/SimpleAgent.kt
- core/agent/src/wasmJsMain/kotlin/agent/AgentFactory.kt

**Files modified:**
- settings.gradle.kts (added :core:agent)
- feature/chat/src/wasmJsMain/kotlin/chat/ChatUseCases.kt
- feature/chat/src/wasmJsMain/kotlin/chat/ChatViewModel.kt
- feature/chat/build.gradle.kts
- Test files updated for new signatures

**Verification:**
- Compilation: ✅ SUCCESS
- Tests: 738/750 PASS (12 failures pre-existing, unrelated to Agent)
- Lint: ✅ PASS
- `./gradlew check`: ✅ SUCCESS

**Ready for:**
- Task 2: Context Persistence (storage layer)
- Task 3: Token Counting
- Task 4: Context Compression

---
