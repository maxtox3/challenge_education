# Техническая Спецификация

**Kotlin/Compose Multiplatform Chat Application**

Version: 1.0.0  
Platform: Kotlin/Wasm (Web)  
Architecture: MVI (Model-View-Intent) with MVIKotlin Framework  
Last Updated: 2026-04-07

---

## 1. Обзор системы

### 1.1 Высокоуровневая архитектура

```
┌─────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                       │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              Compose UI (Compose Multiplatform)           │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌─────────────────┐  │  │
│  │  │ MessageBubble│  │  ChatInput   │  │ SettingsDialog  │  │  │
│  │  └──────────────┘  └──────────────┘  └─────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                       BUSINESS LOGIC LAYER                      │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │           MVI Architecture (MVIKotlin Framework)          │  │
│  │  ┌───────────────┐   ┌──────────────┐  ┌───────────────┐  │  │
│  │  │  ChatStore    │←→ │ChatRepository│←→│ SettingsStore │  │  │
│  │  │  (State/Intent│   │    (Data)    │  │  (Settings)   │  │  │
│  │  │   /Label)     │   └──────────────┘  └───────────────┘  │  │
│  │  └───────────────┘                                        │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                         DATA LAYER                              │
│  ┌──────────────────┐  ┌────────────────┐  ┌────────────────┐   │
│  │   Core Network   │  │  Core Model    │  │  Core Storage  │   │
│  │  ┌────────────┐  │  │ ┌────────────┐ │  │ ┌────────────┐ │   │
│  │  │ChatClient  │  │  │ │DataClasses │ │  │ │LocalStorage│ │   │
│  │  │(Ktor+SSE)  │  │  │ │Enums/Sealed│ │  │ │  Service   │ │   │
│  │  └────────────┘  │  │ └────────────┘ │  │ └────────────┘ │   │
│  └──────────────────┘  └────────────────┘  └────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                      EXTERNAL SERVICES                          │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                   Z.AI API (GLM Models)                   │  │
│  │  • REST API (HTTPS)                                       │  │
│  │  • SSE Streaming (Server-Sent Events)                     │  │
│  │  • Models: GLM-4 32b, GLM-4.5 Air, GLM-4.7, GLM-5         │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              Browser LocalStorage (Wasm)                  │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 Ключевые характеристики

- **Platform**: Kotlin/Wasm (WebAssembly) targeting modern browsers
- **UI Framework**: Jetbrains Compose Multiplatform 1.10.2
- **Architecture Pattern**: MVI (Model-View-Intent) with MVIKotlin
- **State Management**: Unidirectional data flow with immutable state
- **Networking**: Ktor Client 3.x with SSE support
- **Serialization**: Kotlinx Serialization (JSON)
- **Styling**: Material Design 3 with custom theme

---

## 2. Архитектура MVI

### 2.1 MVI Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        MVI CYCLE                                │
│                                                                 │
│  ┌──────────┐         ┌──────────────┐         ┌────────────┐   │
│  │   User   │────────→│     Intent   │────────→│  Executor  │   │
│  │  Action  │         │  (Sealed     │         │  (MVIKotlin│   │
│  │          │         │   Class)     │         │  Factory)  │   │
│  └──────────┘         └──────────────┘         └───────┬────┘   │
│        ↑                                               │        │
│        │                                               │        │
│        │         ┌──────────────────────────────────┐  │        │
│        │         │        EXECUTOR LOGIC            │  │        │
│        │         │  • Call Repository methods       │  │        │
│        │         │  • Dispatch internal Msg         │  │        │
│        │         │  • Publish Labels (one-time)     │  │        │
│        │         │  • Launch coroutines             │  │        │
│        │         └──────────────────────────────────┘  │        │
│        │                                               ↓        │
│        │         ┌──────────────┐         ┌──────────────┐      │
│        │         │     State    │←────────│   Reducer    │      │
│        │         │  (Immutable  │         │  (Pattern    │      │
│        │         │   Data Class)│         │   Matching)  │      │
│        │         └──────┬───────┘         └──────────────┘      │
│        │                │                                       │
│        │                │ State updates                         │
│        │                ↓                                       │
│        │         ┌──────────────┐                               │
│        └─────────│      UI      │                               │
│                  │  (Compose    │                               │
│                  │   Render)    │                               │
│                  └──────────────┘                               │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    SIDE EFFECTS (Labels)                  │  │
│  │  • ScrollToBottom (one-time scroll event)                 │  │
│  │  • ShowToast(message) (notification)                      │  │
│  │  • HideKeyboard (UI action)                               │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 MVI Components

#### ChatState (Immutable State)

```kotlin
data class ChatState(
    val inputText: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val streamingMessage: String? = null,      // Current streaming content
    val isStreaming: Boolean = false,           // Streaming in progress flag
    val errorMessage: String? = null,
    val showSettings: Boolean = false,
    val settings: ApiSettings = ApiSettings(),
)
```

**State Principles**:
- Single source of truth
- Immutable (all fields are `val`)
- Default values for all fields
- Updated only through Reducer

#### ChatIntent (User Actions)

```kotlin
sealed class ChatIntent {
    data class UpdateInputText(val text: String) : ChatIntent()
    data object SendMessage : ChatIntent()
    data object ClearChat : ChatIntent()
    data class UpdateSettings(val settings: ApiSettings) : ChatIntent()
    data class ToggleSettings(val show: Boolean) : ChatIntent()
    data class SetError(val message: String?) : ChatIntent()
    data object ClearError : ChatIntent()
    data class SetLoading(val loading: Boolean) : ChatIntent()
    data class UpdateMessages(val messages: List<ChatMessage>) : ChatIntent()
}
```

**Intent Principles**:
- Sealed class (exhaustive when expressions)
- Represent user actions only
- No business logic
- Processed by Executor

#### ChatLabel (One-Time Events)

```kotlin
sealed class ChatLabel {
    data object ScrollToBottom : ChatLabel()
    data class ShowToast(val message: String) : ChatLabel()
    data object HideKeyboard : ChatLabel()
}
```

**Label Principles**:
- One-time events (not stored in state)
- Published by Executor
- Consumed by UI (not affecting state)
- Examples: navigation, toasts, scroll events

### 2.3 Internal Messages (Msg)

```kotlin
private sealed interface Msg {
    data class InputTextChanged(val text: String) : Msg
    data class MessagesUpdated(val messages: List<ChatMessage>) : Msg
    data class LoadingChanged(val isLoading: Boolean) : Msg
    data class ErrorChanged(val error: String?) : Msg
    data class SettingsToggled(val show: Boolean) : Msg
    data class SettingsUpdated(val settings: ApiSettings) : Msg
    data class StreamingMessageUpdated(val content: String) : Msg
    data object StreamingStarted : Msg
    data object StreamingFinished : Msg
}
```

**Msg Principles**:
- Internal to Store (not exposed to UI)
- Dispatched by Executor
- Processed by Reducer
- Enable state updates

---

## 3. Модульная структура

### 3.1 Module Dependency Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        composeApp                               │
│                    (Entry Point - main.kt)                      │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  • RootComponent (Decompose)                              │  │
│  │  • ChatContent (Compose UI)                               │  │
│  │  • DI Setup (Manual Dependency Injection)                 │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
         │                    │                    │
         ↓                    ↓                    ↓
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  feature:chat   │  │feature:settings │  │  core:storage   │
│  ┌───────────┐  │  │  ┌───────────┐  │  │  ┌───────────┐  │
│  │ChatStore  │  │  │  │SettingsVM │  │  │  │LocalStorage│ │
│  │Repository │  │  │  │ModelSelect│  │  │  │  Service  │  │
│  │UI Comps   │  │  │  │UI Dialog  │  │  │  └───────────┘  │
│  └───────────┘  │  │  └───────────┘  │  │                 │
└─────────────────┘  └─────────────────┘  └─────────────────┘
         │                    │                    
         │                    │                    
         └────────┬───────────┘                    
                  ↓                                
    ┌─────────────────────────────┐                
    │        core:model            │                
    │  ┌────────────────────────┐  │                
    │  │ • ChatMessage          │  │                
    │  │ • StreamChunk          │  │                
    │  │ • ModelType            │  │                
    │  │ • ApiSettings          │  │                
    │  │ • ZAiRequest/Response  │  │                
    │  └────────────────────────┘  │                
    └─────────────────────────────┘                
                  ↑                                
                  │                                
    ┌─────────────────────────────┐                
    │       core:network           │                
    │  ┌────────────────────────┐  │                
    │  │ • ChatClient (Interface)│  │                
    │  │ • ChatClientImpl (Ktor) │  │                
    │  │ • SSE Streaming        │  │                
    │  │ • Error Handling       │  │                
    │  └────────────────────────┘  │                
    └─────────────────────────────┘                
                  ↑                                
                  │                                
    ┌─────────────────────────────┐                
    │         core:ui              │                
    │  ┌────────────────────────┐  │                
    │  │ • AppColors (Theme)    │  │                
    │  │ • DialogSurface        │  │                
    │  │ • Shared Components    │  │                
    │  └────────────────────────┘  │                
    └─────────────────────────────┘                
```

### 3.2 Module Isolation Rules

**Feature Modules** (feature:chat, feature:settings):
- ✅ Depend ONLY on `core/*` modules
- ❌ NEVER depend on each other
- ✅ Contain business logic + UI components
- ✅ Follow MVI pattern strictly

**Core Modules** (core:model, core:network, core:ui, core:storage):
- ✅ Shared across all feature modules
- ✅ No feature-specific logic
- ✅ Highly reusable
- ✅ Well-defined interfaces

### 3.3 Module Descriptions

#### feature:chat
```
feature:chat/
├── src/wasmJsMain/kotlin/chat/
│   ├── store/
│   │   ├── ChatStoreFactory.kt     (MVI Store + Executor + Reducer)
│   │   ├── ChatStore.kt            (Store Interface)
│   │   └── ChatStoreProvider.kt    (DI Provider)
│   ├── ui/components/
│   │   ├── MessageBubble.kt        (Message Display)
│   │   ├── ChatInput.kt            (Input Field)
│   │   └── *Tags.kt                (Test Tags)
│   ├── ChatRepository.kt           (Repository Interface + Impl)
│   ├── ChatState.kt                (Immutable State)
│   ├── ChatIntent.kt               (User Actions)
│   └── ChatLabel.kt                (One-Time Events)
```

**Responsibilities**:
- Chat message display (user/AI)
- Streaming message updates
- Message persistence (localStorage)
- API communication (via ChatRepository)

#### feature:settings
```
feature:settings/
├── src/wasmJsMain/kotlin/settings/
│   ├── SettingsState.kt            (ViewModel + State)
│   ├── ui/
│   │   ├── SettingsDialog.kt       (Settings UI)
│   │   ├── ModelSelector.kt        (Model Dropdown)
│   │   └── *Tags.kt                (Test Tags)
```

**Responsibilities**:
- API key configuration
- Model selection (GLM-4, GLM-4.5, GLM-4.7, GLM-5)
- Temperature, max tokens, stop sequences
- Response format (text/json)

#### core:model
```
core:model/
├── src/wasmJsMain/kotlin/model/
│   ├── ChatMessage.kt              (Message Data Class)
│   ├── StreamChunk.kt              (Streaming Events)
│   ├── ModelType.kt                (Model Enum)
│   ├── ReasoningMode.kt            (Reasoning Enum)
│   ├── MetricRecord.kt             (Metrics Data)
│   └── ZAiRequest.kt               (API Models)
```

**Responsibilities**:
- Centralized data models
- API request/response models
- Enums for type safety
- No business logic

#### core:network
```
core:network/
├── src/wasmJsMain/kotlin/network/
│   ├── ChatClient.kt               (Interface)
│   ├── ChatClientImpl.kt           (Ktor Implementation)
│   └── ResponseConstraints.kt      (API Constraints)
```

**Responsibilities**:
- HTTP client (Ktor)
- SSE streaming support
- Error handling & mapping
- API communication

#### core:storage
```
core:storage/
├── src/wasmJsMain/kotlin/storage/
│   ├── StorageService.kt           (Interface)
│   └── LocalStorageService.kt      (Browser localStorage)
```

**Responsibilities**:
- Chat history persistence
- Settings persistence
- Browser localStorage integration
- Data serialization

---

## 4. Data Flow

### 4.1 Complete Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    USER SENDS MESSAGE                           │
└─────────────────────────────────────────────────────────────────┘
         │
         ↓
┌─────────────────────────────────────────────────────────────────┐
│  1. UI EVENT                                                    │
│     User types "Hello" and clicks Send button                   │
│     ┌──────────────────────────────────────────────────────┐   │
│     │  onClick = { store.accept(ChatIntent.SendMessage) }  │   │
│     └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
         │
         ↓
┌─────────────────────────────────────────────────────────────────┐
│  2. INTENT PROCESSING (ChatStoreFactory Executor)               │
│     ┌──────────────────────────────────────────────────────┐   │
│     │  onIntent<ChatIntent.SendMessage> {                  │   │
│     │      // Create user message                          │   │
│     │      val userMessage = ChatMessage(                  │   │
│     │          role = "user",                              │   │
│     │          content = state().inputText                 │   │
│     │      )                                               │   │
│     │      val updatedMessages = state().messages + user   │   │
│     │                                                      │   │
│     │      // Dispatch Msg to update state                 │   │
│     │      dispatch(Msg.MessagesUpdated(updatedMessages))  │   │
│     │      dispatch(Msg.StreamingStarted)                  │   │
│     │                                                      │   │
│     │      // Launch streaming                             │   │
│     │      launch { startStreaming(...) }                  │   │
│     │  }                                                   │   │
│     └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
         │
         ↓
┌─────────────────────────────────────────────────────────────────┐
│  3. STREAMING REQUEST (ChatRepository)                          │
│     ┌──────────────────────────────────────────────────────┐   │
│     │  repository.sendMessageStreaming(                    │   │
│     │      prompt = "Hello",                               │   │
│     │      messages = updatedMessages,                     │   │
│     │      settings = state().settings                     │   │
│     │  )                                                   │   │
│     │      .onEach { chunk -> handleStreamChunk(...) }     │   │
│     │      .catch { e -> handleError(e) }                  │   │
│     │      .collect()                                      │   │
│     └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
         │
         ↓
┌─────────────────────────────────────────────────────────────────┐
│  4. NETWORK LAYER (ChatClient)                                  │
│     ┌──────────────────────────────────────────────────────┐   │
│     │  client.sendMessageStreaming(...)                    │   │
│     │      .collect { chunk ->                             │   │
│     │          when (chunk) {                              │   │
│     │              is StreamChunk.Content -> ...           │   │
│     │              is StreamChunk.Reasoning -> ...         │   │
│     │              is StreamChunk.Done -> ...              │   │
│     │          }                                           │   │
│     │      }                                               │   │
│     └──────────────────────────────────────────────────────┘   │
│                                                                 │
│     ┌──────────────────────────────────────────────────────┐   │
│     │  HTTP POST to Z.AI API                               │   │
│     │  • Headers: Authorization, Content-Type              │   │
│     │  • Body: ZAiRequest (JSON)                           │   │
│     │  • Stream: true                                      │   │
│     │                                                      │   │
│     │  Response: SSE (Server-Sent Events)                  │   │
│     │  data: {"choices":[{"delta":{"content":"Hi"}}]}      │   │
│     │  data: {"choices":[{"delta":{"content":" there"}}]}  │   │
│     │  data: [DONE]                                        │   │
│     └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
         │
         ↓
┌─────────────────────────────────────────────────────────────────┐
│  5. STREAMING CHUNK HANDLING (Executor)                         │
│     ┌──────────────────────────────────────────────────────┐   │
│     │  private fun handleStreamChunk(                      │   │
│     │      chunk: StreamChunk,                             │   │
│     │      ctx: ExecutorContext                            │   │
│     │  ) {                                                 │   │
│     │      when (chunk) {                                  │   │
│     │          is StreamChunk.Content -> {                 │   │
│     │              val current = state().streamingMessage  │   │
│     │              dispatch(Msg.StreamingMessageUpdated(   │   │
│     │                  current + chunk.text                │   │
│     │              ))                                      │   │
│     │          }                                           │   │
│     │          is StreamChunk.Done -> {                    │   │
│     │              val finalContent = state().streamingMsg │   │
│     │              val finalMessage = ChatMessage(         │   │
│     │                  role = "assistant",                 │   │
│     │                  content = finalContent              │   │
│     │              )                                       │   │
│     │              dispatch(Msg.MessagesUpdated(           │   │
│     │                  state().messages + finalMessage     │   │
│     │              ))                                      │   │
│     │              dispatch(Msg.StreamingFinished)         │   │
│     │              publish(ChatLabel.ScrollToBottom)       │   │
│     │          }                                           │   │
│     │      }                                               │   │
│     │  }                                                   │   │
│     └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
         │
         ↓
┌─────────────────────────────────────────────────────────────────┐
│  6. STATE UPDATE (Reducer)                                      │
│     ┌──────────────────────────────────────────────────────┐   │
│     │  override fun ChatState.reduce(message: Msg): State  │   │
│     │      when (message) {                                │   │
│     │          is Msg.StreamingMessageUpdated ->           │   │
│     │              copy(streamingMessage = message.content)│   │
│     │                                                      │   │
│     │          is Msg.MessagesUpdated ->                   │   │
│     │              copy(messages = message.messages)       │   │
│     │                                                      │   │
│     │          is Msg.StreamingFinished ->                 │   │
│     │              copy(                                   │   │
│     │                  isStreaming = false,                │   │
│     │                  streamingMessage = null,            │   │
│     │                  isLoading = false                   │   │
│     │              )                                       │   │
│     │      }                                               │   │
│     └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
         │
         ↓
┌─────────────────────────────────────────────────────────────────┐
│  7. UI RE-RENDER (Compose)                                      │
│     ┌──────────────────────────────────────────────────────┐   │
│     │  @Composable                                          │   │
│     │  fun ChatContent(component: ChatComponent) {          │   │
│     │      val state by component.state.collectAsState()    │   │
│     │                                                      │   │
│     │      // Re-composes on state change                   │   │
│     │      LazyColumn {                                     │   │
│     │          items(state.messages) { message ->           │   │
│     │              MessageBubble(message)                   │   │
│     │          }                                            │   │
│     │      }                                                │   │
│     │                                                      │   │
│     │      // Show streaming message                        │   │
│     │      if (state.isStreaming) {                         │   │
│     │          state.streamingMessage?.let { content ->     │   │
│     │              MessageBubble(                           │   │
│     │                  ChatMessage(                         │   │
│     │                      role = "assistant",              │   │
│     │                      content = content,               │   │
│     │                      isStreaming = true               │   │
│     │                  )                                    │   │
│     │              )                                        │   │
│     │          }                                            │   │
│     │      }                                                │   │
│     │  }                                                    │   │
│     └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
         │
         ↓
┌─────────────────────────────────────────────────────────────────┐
│  8. SIDE EFFECT (Label)                                         │
│     ┌──────────────────────────────────────────────────────┐   │
│     │  LaunchedEffect(component) {                         │   │
│     │      component.labels.collect { label ->             │   │
│     │          when (label) {                              │   │
│     │              is ChatLabel.ScrollToBottom -> {        │   │
│     │                  scrollState.animateScrollTo(        │   │
│     │                      scrollState.maxValue            │   │
│     │                  )                                   │   │
│     │              }                                       │   │
│     │              is ChatLabel.ShowToast -> {             │   │
│     │                  // Show toast notification          │   │
│     │              }                                       │   │
│     │          }                                           │   │
│     │      }                                               │   │
│     │  }                                                   │   │
│     └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 State Flow Summary

```
┌──────────┐   Intent    ┌──────────┐   Msg      ┌─────────┐   State    ┌──────────┐
│   User   │────────────→│ Executor │───────────→│ Reducer │───────────→│    UI    │
│  Action  │             │          │            │         │            │  Render  │
└──────────┘             └──────────┘            └─────────┘            └──────────┘
                               │                                            ↑
                               │ Label (one-time)                           │
                               └────────────────────────────────────────────┘
```

---

## 5. SSE Streaming Infrastructure

### 5.1 Streaming Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                  SSE STREAMING FLOW                              │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────┐
│  User Message    │
│  "Hello"         │
└────────┬─────────┘
         │
         ↓
┌─────────────────────────────────────────────────────────────────┐
│  ChatStoreFactory.handleSendMessage()                           │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  launch {                                                │  │
│  │      repository.sendMessageStreaming(...)                │  │
│  │          .onEach { chunk -> handleStreamChunk(chunk) }   │  │
│  │          .collect()                                      │  │
│  │  }                                                       │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
         │
         ↓
┌─────────────────────────────────────────────────────────────────┐
│  ChatClientImpl.sendMessageStreaming()                          │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  override fun sendMessageStreaming(...): Flow<StreamChunk>│  │
│  │      = channelFlow {                                     │  │
│  │          val request = ZAiRequest(                       │  │
│  │              model = "glm-5",                            │  │
│  │              messages = [...],                           │  │
│  │              stream = true  ← ENABLE STREAMING           │  │
│  │          )                                               │  │
│  │                                                          │  │
│  │          client.sse(request = {                          │  │
│  │              url { takeFrom(baseUrl) }                   │  │
│  │              method = HttpMethod.Post                    │  │
│  │              setBody(json.encodeToString(request))       │  │
│  │          }) {                                            │  │
│  │              incoming.collect { event ->                 │  │
│  │                  parseSSEEvent(event.data)?.let { chunk->│  │
│  │                      send(chunk)                         │  │
│  │                  }                                       │  │
│  │              }                                           │  │
│  │          }                                               │  │
│  │      }                                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
         │
         ↓
┌─────────────────────────────────────────────────────────────────┐
│  Z.AI API (Server-Sent Events)                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  HTTP/1.1 200 OK                                         │  │
│  │  Content-Type: text/event-stream                         │  │
│  │                                                          │  │
│  │  data: {"id":"123","choices":[{"delta":{"content":"Hi"}}]}│  │
│  │  data: {"id":"123","choices":[{"delta":{"content":" there"}}]}│
│  │  data: {"id":"123","choices":[{"delta":{"reasoning_content":"..."}}]}│
│  │  data: {"id":"123","choices":[{"finish_reason":"stop"}]} │  │
│  │  data: [DONE]                                            │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
         │
         ↓
┌─────────────────────────────────────────────────────────────────┐
│  ChatClientImpl.parseSSEEvent()                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  private fun parseSSEEvent(data: String?): StreamChunk? {│  │
│  │      when {                                              │  │
│  │          data == null || data == "[DONE]" ->             │  │
│  │              StreamChunk.Done                            │  │
│  │                                                          │  │
│  │          else -> {                                       │  │
│  │              val chunk = json.decodeFromString<          │  │
│  │                  ZAiStreamChunk                          │  │
│  │              >(data)                                     │  │
│  │              val delta = chunk.choices.first().delta     │  │
│  │                                                          │  │
│  │              when {                                      │  │
│  │                  delta.reasoningContent != null ->       │  │
│  │                      StreamChunk.Reasoning(              │  │
│  │                          delta.reasoningContent          │  │
│  │                      )                                   │  │
│  │                                                          │  │
│  │                  delta.content != null ->                │  │
│  │                      StreamChunk.Content(                │  │
│  │                          delta.content                   │  │
│  │                      )                                   │  │
│  │                                                          │  │
│  │                  else -> null                            │  │
│  │              }                                           │  │
│  │          }                                               │  │
│  │      }                                                   │  │
│  │  }                                                       │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
         │
         ↓
┌─────────────────────────────────────────────────────────────────┐
│  StreamChunk Sealed Class (from core:model)                     │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  sealed class StreamChunk {                              │  │
│  │      data class Content(val text: String) : StreamChunk()│  │
│  │      data class Reasoning(val text: String) : StreamChunk()│
│  │      data object Done : StreamChunk()                    │  │
│  │  }                                                       │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
         │
         ↓
┌─────────────────────────────────────────────────────────────────┐
│  ChatStoreFactory.handleStreamChunk()                           │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  private fun handleStreamChunk(                          │  │
│  │      chunk: StreamChunk,                                 │  │
│  │      ctx: ExecutorContext                                │  │
│  │  ) {                                                     │  │
│  │      when (chunk) {                                      │  │
│  │          is StreamChunk.Content -> {                     │  │
│  │              val current = state().streamingMessage ?: ""│  │
│  │              dispatch(Msg.StreamingMessageUpdated(       │  │
│  │                  current + chunk.text                    │  │
│  │              ))                                          │  │
│  │          }                                               │  │
│  │                                                          │  │
│  │          is StreamChunk.Reasoning -> {                   │  │
│  │              val current = state().streamingMessage ?: ""│  │
│  │              dispatch(Msg.StreamingMessageUpdated(       │  │
│  │                  current + chunk.text                    │  │
│  │              ))                                          │  │
│  │          }                                               │  │
│  │                                                          │  │
│  │          is StreamChunk.Done -> {                        │  │
│  │              handleStreamingComplete()                   │  │
│  │          }                                               │  │
│  │      }                                                   │  │
│  │  }                                                       │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
         │
         ↓
┌─────────────────────────────────────────────────────────────────┐
│  State Updates (Reducer)                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Msg.StreamingMessageUpdated("H")                        │  │
│  │  Msg.StreamingMessageUpdated("Hi")                       │  │
│  │  Msg.StreamingMessageUpdated("Hi ")                      │  │
│  │  Msg.StreamingMessageUpdated("Hi there")                 │  │
│  │  Msg.StreamingFinished                                   │  │
│  │  Msg.MessagesUpdated([...finalMessage])                  │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
         │
         ↓
┌─────────────────────────────────────────────────────────────────┐
│  UI Renders (Compose)                                           │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Frame 1: streamingMessage = "H"                         │  │
│  │  Frame 2: streamingMessage = "Hi"                        │  │
│  │  Frame 3: streamingMessage = "Hi "                       │  │
│  │  Frame 4: streamingMessage = "Hi there"                  │  │
│  │  Frame 5: messages = [..., finalMessage]                 │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 StreamChunk Types

```
┌─────────────────────────────────────────────────────────────────┐
│                    STREAMCHUNK TYPES                            │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────┐
│  StreamChunk     │ (Sealed Class)
│  ┌────────────┐  │
│  │  Content   │  │  → Regular text chunks
│  │  (text)    │  │  → Displayed as message content
│  └────────────┘  │
│  ┌────────────┐  │
│  │ Reasoning  │  │  → Thinking/reasoning content
│  │  (text)    │  │  → GLM-5 thinking mode
│  └────────────┘  │  → Displayed with special indicator
│  ┌────────────┐  │
│  │    Done    │  │  → End of stream signal
│  │  (object)  │  │  → Finalize message
│  └────────────┘  │  → Persist to storage
└──────────────────┘
```

### 5.3 Streaming State Management

```
┌─────────────────────────────────────────────────────────────────┐
│              STREAMING STATE TRANSITIONS                        │
└─────────────────────────────────────────────────────────────────┘

Initial State:
┌────────────────────────────────┐
│  isStreaming: false            │
│  streamingMessage: null        │
│  isLoading: false              │
│  messages: [...]               │
└────────────────────────────────┘
         │
         │ SendMessage Intent
         ↓
Streaming Started:
┌────────────────────────────────┐
│  isStreaming: true             │
│  streamingMessage: ""          │
│  isLoading: true               │
│  messages: [..., userMsg]      │
└────────────────────────────────┘
         │
         │ StreamChunk.Content("H")
         ↓
┌────────────────────────────────┐
│  isStreaming: true             │
│  streamingMessage: "H"         │
│  isLoading: true               │
│  messages: [..., userMsg]      │
└────────────────────────────────┘
         │
         │ StreamChunk.Content("i")
         ↓
┌────────────────────────────────┐
│  isStreaming: true             │
│  streamingMessage: "Hi"        │
│  isLoading: true               │
│  messages: [..., userMsg]      │
└────────────────────────────────┘
         │
         │ StreamChunk.Done
         ↓
Streaming Finished:
┌────────────────────────────────┐
│  isStreaming: false            │
│  streamingMessage: null        │
│  isLoading: false              │
│  messages: [..., userMsg, aiMsg]│
└────────────────────────────────┘
```

---

## 6. Ключевые компоненты

### 6.1 ChatStoreFactory (MVI Store)

```
┌─────────────────────────────────────────────────────────────────┐
│              ChatStoreFactory Architecture                      │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  ChatStoreFactory                                               │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Dependencies:                                           │  │
│  │  • storeFactory: StoreFactory (MVIKotlin)                │  │
│  │  • repository: ChatRepository                            │  │
│  │  • storage: StorageService                               │  │
│  │  • scope: CoroutineScope                                 │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Internal Msg (Sealed Interface):                        │  │
│  │  • InputTextChanged(text: String)                        │  │
│  │  • MessagesUpdated(messages: List<ChatMessage>)          │  │
│  │  • LoadingChanged(isLoading: Boolean)                    │  │
│  │  • ErrorChanged(error: String?)                          │  │
│  │  • SettingsToggled(show: Boolean)                        │  │
│  │  • SettingsUpdated(settings: ApiSettings)                │  │
│  │  • StreamingMessageUpdated(content: String)              │  │
│  │  • StreamingStarted                                      │  │
│  │  • StreamingFinished                                     │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Executor (coroutineExecutorFactory):                    │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  Intent Handlers:                                  │  │  │
│  │  │  • UpdateInputText → dispatch(Msg.InputTextChanged)│  │  │
│  │  │  • SendMessage → handleSendMessage()               │  │  │
│  │  │  • ClearChat → dispatch(Msg.MessagesUpdated)       │  │  │
│  │  │  • UpdateSettings → dispatch(Msg.SettingsUpdated)  │  │  │
│  │  │  • ToggleSettings → dispatch(Msg.SettingsToggled)  │  │  │
│  │  │  • SetError → dispatch(Msg.ErrorChanged)           │  │  │
│  │  │  • ClearError → dispatch(Msg.ErrorChanged(null))   │  │  │
│  │  │  • SetLoading → dispatch(Msg.LoadingChanged)       │  │  │
│  │  │  • UpdateMessages → dispatch(Msg.MessagesUpdated)  │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  │                                                          │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  Streaming Logic:                                  │  │  │
│  │  │  • startStreaming() → repository.sendMessageStream │  │  │
│  │  │  • handleStreamChunk() → dispatch(Msg.Streaming*)  │  │  │
│  │  │  • handleStreamingComplete() → finalize message    │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Reducer (MessageReducer):                               │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  Pattern Matching on Msg:                          │  │  │
│  │  │  • InputTextChanged → copy(inputText = text)       │  │  │
│  │  │  • MessagesUpdated → copy(messages = messages)     │  │  │
│  │  │  • LoadingChanged → copy(isLoading = isLoading)    │  │  │
│  │  │  • ErrorChanged → copy(errorMessage = error)       │  │  │
│  │  │  • SettingsToggled → copy(showSettings = show)     │  │  │
│  │  │  • SettingsUpdated → copy(settings = settings)     │  │  │
│  │  │  • StreamingMessageUpdated → copy(streamingMsg=...)│  │  │
│  │  │  • StreamingStarted → copy(isStreaming=true, ...)  │  │  │
│  │  │  • StreamingFinished → copy(isStreaming=false, ...)│  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Initialization:                                         │  │
│  │  • Load chat history from storage on creation            │  │
│  │  • scope.launch { storage.getChatHistory() }             │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 ChatRepository (Repository Pattern)

```
┌─────────────────────────────────────────────────────────────────┐
│                ChatRepository Architecture                      │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  ChatRepository (Interface)                                     │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  suspend fun sendMessage(                                │  │
│  │      prompt: String,                                     │  │
│  │      messages: List<ChatMessage>,                        │  │
│  │      settings: ApiSettings                               │  │
│  │  ): SendMessageResult                                    │  │
│  │                                                          │  │
│  │  fun sendMessageStreaming(                               │  │
│  │      prompt: String,                                     │  │
│  │      messages: List<ChatMessage>,                        │  │
│  │      settings: ApiSettings                               │  │
│  │  ): Flow<StreamChunk>                                    │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Implementation
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  ChatRepositoryImpl                                             │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Dependencies:                                           │  │
│  │  • client: ChatClient                                    │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  sendMessage():                                          │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  1. Convert ApiSettings → ResponseConstraints      │  │  │
│  │  │  2. Call client.sendMessage(...)                   │  │  │
│  │  │  3. Map Result<ChatMessage> → SendMessageResult    │  │  │
│  │  │     • Success → SendMessageResult.Success          │  │  │
│  │  │     • Failure → SendMessageResult.Error            │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  │                                                          │  │
│  │  sendMessageStreaming():                                 │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  1. Convert ApiSettings → ResponseConstraints      │  │  │
│  │  │  2. Return client.sendMessageStreaming(...)        │  │  │
│  │  │  3. Flow<StreamChunk> → UI layer                   │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 6.3 ChatClient (Network Layer)

```
┌─────────────────────────────────────────────────────────────────┐
│                  ChatClient Architecture                        │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  ChatClient (Interface)                                         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  suspend fun sendMessage(                                │  │
│  │      model: String,                                      │  │
│  │      messages: List<ChatMessage>,                        │  │
│  │      constraints: ResponseConstraints,                   │  │
│  │      systemPrompt: String?                               │  │
│  │  ): Result<ChatMessage>                                  │  │
│  │                                                          │  │
│  │  fun sendMessageStreaming(                               │  │
│  │      model: String,                                      │  │
│  │      messages: List<ChatMessage>,                        │  │
│  │      constraints: ResponseConstraints,                   │  │
│  │      systemPrompt: String?                               │  │
│  │  ): Flow<StreamChunk>                                    │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Implementation
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  ChatClientImpl                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Dependencies:                                           │  │
│  │  • HttpClient (Ktor)                                     │  │
│  │  • Json (Kotlinx Serialization)                          │  │
│  │  • apiKeyProvider: () → String                           │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Configuration:                                          │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  baseUrl = "https://api.z.ai/api/coding/paas/..."  │  │  │
│  │  │  json = Json {                                     │  │  │
│  │  │      ignoreUnknownKeys = true                      │  │  │
│  │  │      isLenient = true                              │  │  │
│  │  │      encodeDefaults = true                         │  │  │
│  │  │  }                                                 │  │  │
│  │  │  client = HttpClient {                             │  │  │
│  │  │      install(ContentNegotiation) { json(json) }    │  │  │
│  │  │      install(SSE) { bufferPolicy = All }           │  │  │
│  │  │  }                                                 │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Private Functions:                                      │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  buildAllMessages(messages, systemPrompt):         │  │  │
│  │  │    → List<Message>                                 │  │  │
│  │  │    → Adds system prompt at beginning               │  │  │
│  │  │                                                    │  │  │
│  │  │  buildRequest(model, messages, constraints, stream)│  │  │
│  │  │    → ZAiRequest                                    │  │  │
│  │  │    → Maps constraints to request fields            │  │  │
│  │  │                                                    │  │  │
│  │  │  parseSSEEvent(data): StreamChunk?                 │  │  │
│  │  │    → Parses SSE data → StreamChunk                 │  │  │
│  │  │    • null / "[DONE]" → StreamChunk.Done            │  │  │
│  │  │    • reasoningContent → StreamChunk.Reasoning      │  │  │
│  │  │    • content → StreamChunk.Content                 │  │  │
│  │  │                                                    │  │  │
│  │  │  parseResponse(responseBody, constraints, model)   │  │  │
│  │  │    → Result<ChatMessage>                           │  │  │
│  │  │    • Error response → ChatClientException          │  │  │
│  │  │    • Empty choices → ChatClientException           │  │  │
│  │  │    • Success → ChatMessage                         │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Error Handling:                                         │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  Exception Mapping:                                │  │  │
│  │  │  • ClientRequestException → ChatNetworkException   │  │  │
│  │  │  • ServerResponseException → ChatNetworkException  │  │  │
│  │  │  • SerializationException → ChatSerializationExc   │  │  │
│  │  │  • ChatException → Propagate as is                 │  │  │
│  │  │  • IllegalStateException → ChatNetworkException    │  │  │
│  │  │  • IllegalArgumentException → ChatNetworkException│  │  │
│  │  │  • API error response → ChatClientException        │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 6.4 MessageBubble (UI Component)

```
┌─────────────────────────────────────────────────────────────────┐
│                MessageBubble Component                          │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  MessageBubble(message: ChatMessage)                            │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  UI Composition:                                         │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  Row (horizontal arrangement)                      │  │  │
│  │  │    └── Surface (rounded corners, color by role)    │  │  │
│  │  │          └── Column                                │  │  │
│  │  │                ├── RoleLabel ("You" / "AI" / model)│  │  │
│  │  │                ├── Spacer                          │  │  │
│  │  │                ├── MessageContent (Markdown)       │  │  │
│  │  │                └── TokensInfo (optional)           │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Animations:                                             │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  • Fade-in animation (alpha: 0f → 1f)              │  │  │
│  │  │  • Slide-in animation (for non-streaming)          │  │  │
│  │  │  • Duration: 300ms                                 │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Message Types:                                          │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  User Message:                                     │  │  │
│  │  │    • Color: AppColors.UserBubble                   │  │  │
│  │  │    • Alignment: End (right side)                   │  │  │
│  │  │    • Label: "You"                                  │  │  │
│  │  │                                                    │  │  │
│  │  │  Assistant Message:                                │  │  │
│  │  │    • Color: AppColors.AssistantBubble              │  │  │
│  │  │    • Alignment: Start (left side)                  │  │  │
│  │  │    • Label: Model name (e.g., "GLM-5")             │  │  │
│  │  │    • Markdown rendering                            │  │  │
│  │  │                                                    │  │  │
│  │  │  Reasoning Message:                                │  │  │
│  │  │    • Border: AppColors.Warning                     │  │  │
│  │  │    • Indicator: "[!] Reasoning content"            │  │  │
│  │  │    • Scrollable (max 120dp)                        │  │  │
│  │  │    • Alpha: 0.8f (muted)                           │  │  │
│  │  │                                                    │  │  │
│  │  │  Streaming Message:                                │  │  │
│  │  │    • isStreaming = true                            │  │  │
│  │  │    • No slide animation                            │  │  │
│  │  │    • Updates on every chunk                        │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Test Tags:                                              │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  • MessageBubbleTags.ROOT                          │  │  │
│  │  │  • MessageBubbleTags.SURFACE                       │  │  │
│  │  │  • MessageBubbleTags.ROLE_TEXT                     │  │  │
│  │  │  • MessageBubbleTags.MARKDOWN_CONTENT              │  │  │
│  │  │  • MessageBubbleTags.TOKENS_INFO                   │  │  │
│  │  │  • MessageBubbleTags.REASONING_INDICATOR           │  │  │
│  │  │  • MessageBubbleTags.TYPING_INDICATOR              │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  TypingIndicator (Animated dots)                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Animated three dots:                                    │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  • Infinite transition                             │  │  │
│  │  │  • Scale animation (0.5f → 1.2f)                   │  │  │
│  │  │  • Staggered delays (0ms, 100ms, 200ms)            │  │  │
│  │  │  • Duration: 1200ms per cycle                      │  │  │
│  │  │  • RepeatMode: Restart                             │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. Технологический стек

### 7.1 Core Technologies

```
┌─────────────────────────────────────────────────────────────────┐
│                      TECHNOLOGY STACK                           │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  Language & Platform                                            │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  • Kotlin 2.3.20                                         │  │
│  │  • Kotlin Multiplatform (KMP)                            │  │
│  │  • Target: Kotlin/Wasm (WebAssembly)                     │  │
│  │  • Kotlinx Coroutines 1.10.2                             │  │
│  │  • Kotlinx Serialization 1.8.0                           │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  UI Framework                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  • Jetbrains Compose Multiplatform 1.10.2                │  │
│  │  • Material Design 3                                     │  │
│  │  • Compose UI (Wasm target)                              │  │
│  │  • Custom theming (AppColors)                            │  │
│  │  • Markdown rendering (multiplatform-markdown-renderer)  │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  Architecture & State Management                               │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  • MVIKotlin 4.0.0 (MVI framework)                       │  │
│  │  • Decompose 3.1.0 (Component lifecycle)                 │  │
│  │  • Essenty 2.1.0 (Lifecycle utils)                       │  │
│  │  • Unidirectional data flow                              │  │
│  │  • Immutable state                                       │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  Networking                                                     │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  • Ktor Client 3.4.1                                     │  │
│  │    • core                                                │  │
│  │    • content-negotiation                                 │  │
│  │    • serialization-kotlinx-json                          │  │
│  │    • sse (Server-Sent Events)                            │  │
│  │  • Kotlinx Serialization JSON 1.8.0                      │  │
│  │  • HTTPS (TLS)                                           │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  Code Quality                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  • Detekt 1.23.8 (Static analysis)                       │  │
│  │  • ktlint 14.2.0 (Code style)                            │  │
│  │  • ktlint-gradle-plugin                                  │  │
│  │  • No comments policy (self-documenting code)            │  │
│  │  • SOLID, KISS, DRY, YAGNI principles                    │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  Testing                                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  • Kotlin Test                                           │  │
│  │  • Compose UI Testing                                    │  │
│  │  • Baseline tests (characterization tests)               │  │
│  │  • Unit tests (ViewModel, Repository)                    │  │
│  │  • UI tests (Compose interactions)                       │  │
│  │  • Test tags for all components                          │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  Build Tools                                                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  • Gradle 8.x                                            │  │
│  │  • Gradle Kotlin DSL                                     │  │
│  │  • Multiplatform plugin                                  │  │
│  │  • Compose plugin                                        │  │
│  │  • Serialization plugin                                  │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  Storage                                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  • Browser LocalStorage (Wasm)                           │  │
│  │  • Kotlinx Serialization (JSON)                          │  │
│  │  • Chat history persistence                              │  │
│  │  • Settings persistence                                  │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  External APIs                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  • Z.AI API (GLM Models)                                 │  │
│  │    • REST API (HTTPS)                                    │  │
│  │    • SSE Streaming                                       │  │
│  │    • Models: GLM-4 32b, GLM-4.5 Air, GLM-4.7, GLM-5     │  │
│  │  • OpenAI-compatible API format                          │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 7.2 Dependency Versions

```kotlin
// build.gradle.kts (root)
plugins {
    kotlin("multiplatform") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
    id("org.jetbrains.compose") version "1.10.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

// Dependencies
kotlin("multiplatform"): "2.3.20"
compose: "1.10.2"
mvikotlin: "4.0.0"
decompose: "3.1.0"
essenty: "2.1.0"
ktor: "3.4.1"
kotlinx-coroutines: "1.10.2"
kotlinx-serialization: "1.8.0"
multiplatform-markdown-renderer: "0.33.0"
```

### 7.3 Project Structure

```
task6/
├── composeApp/                   # Entry point (main.kt)
│   ├── src/wasmJsMain/
│   │   └── kotlin/
│   │       └── main.kt           # App initialization
│   └── build.gradle.kts
├── feature/
│   ├── chat/                     # Chat feature module
│   │   ├── src/wasmJsMain/kotlin/chat/
│   │   │   ├── store/            # MVI Store
│   │   │   ├── ui/components/    # UI components
│   │   │   ├── ChatRepository.kt
│   │   │   ├── ChatState.kt
│   │   │   ├── ChatIntent.kt
│   │   │   └── ChatLabel.kt
│   │   └── build.gradle.kts
│   └── settings/                 # Settings feature module
│       ├── src/wasmJsMain/kotlin/settings/
│       │   ├── SettingsState.kt
│       │   └── ui/               # Settings UI
│       └── build.gradle.kts
├── core/
│   ├── model/                    # Shared data models
│   │   ├── src/wasmJsMain/kotlin/model/
│   │   │   ├── ChatMessage.kt
│   │   │   ├── StreamChunk.kt
│   │   │   ├── ModelType.kt
│   │   │   ├── ReasoningMode.kt
│   │   │   ├── MetricRecord.kt
│   │   │   └── ZAiRequest.kt
│   │   └── build.gradle.kts
│   ├── network/                  # Network layer
│   │   ├── src/wasmJsMain/kotlin/network/
│   │   │   ├── ChatClient.kt
│   │   │   ├── ChatClientImpl.kt
│   │   │   └── ResponseConstraints.kt
│   │   └── build.gradle.kts
│   ├── storage/                  # Storage layer
│   │   ├── src/wasmJsMain/kotlin/storage/
│   │   │   ├── StorageService.kt
│   │   │   └── LocalStorageService.kt
│   │   └── build.gradle.kts
│   └── ui/                       # Shared UI components
│       ├── src/wasmJsMain/kotlin/ui/
│       │   ├── theme/AppColors.kt
│       │   └── components/
│       └── build.gradle.kts
├── build.gradle.kts              # Root build config
├── settings.gradle.kts           # Module configuration
├── AGENTS.md                     # Project documentation
└── TECHNICAL_SPECIFICATION.md    # This file
```

---

## 8. Implementation Status

### 8.1 Completed Features ✅

- ✅ MVI architecture with MVIKotlin
- ✅ Chat message display (user/AI)
- ✅ SSE streaming integration
- ✅ Message persistence (localStorage)
- ✅ Settings dialog (API key, model, temperature, etc.)
- ✅ Model selection (4 models)
- ✅ Markdown rendering
- ✅ Error handling
- ✅ Loading states
- ✅ Streaming message display
- ✅ Reasoning content support
- ✅ Test tags for UI components

### 8.2 Planned Features 📋

- 📋 Metrics tracking (response time, tokens, etc.)
- 📋 Reasoning mode comparison (4 modes)
- 📋 Multi-model comparison
- 📋 Chat history search
- 📋 Message export
- 📋 Keyboard shortcuts
- 📋 Accessibility improvements

### 8.3 Known Limitations ⚠️

- ⚠️ No offline support
- ⚠️ No message search
- ⚠️ No multi-language support
- ⚠️ Settings not persisted (in-memory only)
- ⚠️ No message editing
- ⚠️ No message deletion

---

## 9. Development Workflow

### 9.1 Common Commands

```bash
# Development server
./gradlew wasmJsRun              # Start dev server on :8080

# Code quality
./gradlew check                  # Tests + lint + detekt
./gradlew ktlintFormat           # Auto-fix style issues

# Module-specific checks
./gradlew :feature:chat:check
./gradlew :core:network:check

# Run specific tests
./gradlew :composeApp:test --tests "Pattern*"

# Compile check
./gradlew :feature:chat:compileKotlinWasmJs
```

### 9.2 Development Principles

- **SOLID**: Single Responsibility, Open/Closed, Liskov, Interface Segregation, Dependency Inversion
- **KISS**: Keep It Simple, Stupid
- **DRY**: Don't Repeat Yourself
- **YAGNI**: You Aren't Gonna Need It
- **No comments**: Code should be self-documenting
- **No @Suppress**: Detekt rules (except tests and AppColors.kt)

---

## 10. API Reference

### 10.1 Z.AI API

**Base URL**: `https://api.z.ai/api/coding/paas/v4/chat/completions`

**Authentication**: Bearer token (API key)

**Request Format**:
```json
{
  "model": "glm-5",
  "messages": [
    {"role": "user", "content": "Hello"}
  ],
  "stream": true,
  "temperature": 1.0,
  "max_tokens": null,
  "stop": null,
  "response_format": null
}
```

**Response Format** (Non-streaming):
```json
{
  "id": "chatcmpl-123",
  "choices": [{
    "message": {
      "role": "assistant",
      "content": "Hi there!"
    },
    "finish_reason": "stop"
  }],
  "usage": {
    "prompt_tokens": 10,
    "completion_tokens": 5,
    "total_tokens": 15
  }
}
```

**Response Format** (Streaming):
```
data: {"id":"chatcmpl-123","choices":[{"delta":{"content":"Hi"}}]}
data: {"id":"chatcmpl-123","choices":[{"delta":{"content":" there"}}]}
data: [DONE]
```

### 10.2 Available Models

| Model ID | Display Name | Level |
|----------|--------------|-------|
| glm-4-32b-0414-128k | GLM-4 32b | Супер легкая, слабая и дешевая |
| glm-4.5-air | GLM-4.5 Air | Слабая |
| glm-4.7 | GLM-4.7 | Средняя |
| glm-5 | GLM-5 | Сильная |

---

## 11. Troubleshooting

### 11.1 Common Issues

**Issue**: Streaming not working  
**Solution**: Check API key, verify network connectivity, check console logs

**Issue**: Messages not persisting  
**Solution**: Check browser localStorage, clear cache, verify StorageService

**Issue**: Build failures  
**Solution**: Run `./gradlew clean`, check Kotlin version, update dependencies

**Issue**: Detekt violations  
**Solution**: Run `./gradlew ktlintFormat`, fix violations manually, avoid `@Suppress`

### 11.2 Debugging

- Enable browser console logs (println statements in ChatClientImpl)
- Check network tab in browser DevTools (HTTP requests, SSE events)
- Monitor state updates (add logging in Reducer)
- Inspect localStorage (Application tab → Local Storage)

---

## 12. Conclusion

This technical specification provides a comprehensive overview of the Kotlin/Compose Multiplatform chat application architecture, data flow, and implementation details. The system follows modern best practices with:

- **Clean Architecture**: Clear separation of concerns (presentation, business logic, data)
- **MVI Pattern**: Unidirectional data flow with immutable state
- **Modular Design**: Feature modules isolated from each other, shared core modules
- **Type Safety**: Sealed classes, enums, and strong typing throughout
- **Testability**: Test tags, dependency injection, repository pattern
- **Scalability**: Modular structure allows easy feature additions

The application demonstrates production-ready architecture patterns suitable for complex UI applications with real-time data streaming, state management, and persistence requirements.

---

**Document Version**: 1.0.0  
**Last Updated**: 2026-04-07  
**Maintained By**: Development Team
