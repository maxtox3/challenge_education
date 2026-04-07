# AGENTS.md — Core Storage Module

## Назначение

Core storage module — модуль для persistent storage на базе Web Storage API (localStorage). Содержит interface и implementation для сохранения chat history и API settings. Sync API с minimal footprint, оптимизирован для Kotlin/Wasm.

## Команды

```bash
./gradlew :core:storage:check              # Tests + lint + detekt для модуля
./gradlew :core:storage:ktlintFormat       # Auto-fix style
./gradlew :core:storage:compileKotlinWasmJs # Проверка компиляции
```

## Архитектура

**Module Type**: Core infrastructure module (persistence layer)

**Pattern**: Repository pattern (interface + implementation)

**Module Dependencies**:
```
core:storage
├── core:model   (ChatMessage для serialization)
└── kotlinx-serialization-json (JSON encode/decode)
```

**Used by**:
- `feature:chat` → ChatStoreFactory, ChatStoreProvider (chat history persistence)
- `feature:settings` → SettingsStoreFactory, SettingsStoreProvider (API settings persistence)
- `composeApp` → main.kt (initialization), DefaultRootComponent, DefaultChatComponent, DefaultSettingsComponent

## Ключевые файлы (76 строк)

| Файл | Строк | Назначение |
|------|-------|------------|
| `StorageService.kt` | 11 | Interface для storage operations |
| `LocalStorageService.kt` | 65 | Implementation на базе Web localStorage API |

## StorageService Interface

```kotlin
interface StorageService {
    suspend fun getChatHistory(): List<ChatMessage>
    suspend fun setChatHistory(messages: List<ChatMessage>)
    suspend fun clearChatHistory()
    suspend fun getApiSettings(): String?
    suspend fun setApiSettings(settings: String)
}
```

**Methods**:
- `getChatHistory()` — получить сохранённую историю чата (пустой список если нет данных)
- `setChatHistory(messages)` — сохранить историю чата
- `clearChatHistory()` — очистить историю чата
- `getApiSettings()` — получить сохранённые API settings (null если нет данных)
- `setApiSettings(settings)` — сохранить API settings (JSON string)

**Design**:
- All methods are `suspend` для consistency (хотя localStorage sync)
- Graceful degradation: errors → empty list / null
- No exceptions thrown to callers

## LocalStorageService Implementation

### Key Components

**External Declarations** (Web Storage API):
```kotlin
external interface Storage {
    fun getItem(key: String): String?
    fun setItem(key: String, value: String)
    fun removeItem(key: String)
}

external val localStorage: Storage

external object console {
    fun error(message: String)
}
```

**JSON Configuration**:
```kotlin
private val json: Json = Json { ignoreUnknownKeys = true }
```

**Storage Keys** (companion object):
```kotlin
companion object {
    private const val CHAT_HISTORY_KEY = "chat_history"
    private const val API_SETTINGS_KEY = "api_settings"
}
```

### Chat History Operations

#### getChatHistory

```kotlin
override suspend fun getChatHistory(): List<ChatMessage> {
    return try {
        val stored = localStorage.getItem(CHAT_HISTORY_KEY)
        if (stored != null) {
            json.decodeFromString(stored)
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }
}
```

**Behavior**:
- Success with data → `List<ChatMessage>` (decoded from JSON)
- No data → `emptyList()`
- Decode error → `emptyList()` (graceful degradation)

#### setChatHistory

```kotlin
override suspend fun setChatHistory(messages: List<ChatMessage>) {
    try {
        val encoded = json.encodeToString(messages)
        localStorage.setItem(CHAT_HISTORY_KEY, encoded)
    } catch (e: Exception) {
        console.error("Failed to save chat history: ${e.message}")
    }
}
```

**Behavior**:
- Success → data saved to localStorage
- Encode/Save error → logged to console.error, no exception thrown

#### clearChatHistory

```kotlin
override suspend fun clearChatHistory() {
    localStorage.removeItem(CHAT_HISTORY_KEY)
}
```

**Behavior**:
- Removes `chat_history` key from localStorage
- No error handling (removeItem never throws)

### API Settings Operations

#### getApiSettings

```kotlin
override suspend fun getApiSettings(): String? {
    return localStorage.getItem(API_SETTINGS_KEY)
}
```

**Behavior**:
- Success with data → `String` (JSON string)
- No data → `null`

#### setApiSettings

```kotlin
override suspend fun setApiSettings(settings: String) {
    try {
        localStorage.setItem(API_SETTINGS_KEY, settings)
    } catch (e: Exception) {
        console.error("Failed to save API settings: ${e.message}")
    }
}
```

**Behavior**:
- Success → data saved to localStorage
- Save error → logged to console.error, no exception thrown

## Usage Examples

### Basic Usage

```kotlin
import storage.LocalStorageService
import kotlinx.serialization.json.Json

val storage = LocalStorageService(
    json = Json { ignoreUnknownKeys = true }
)

// Save chat history
val messages = listOf(
    ChatMessage(role = "user", content = "Hello"),
    ChatMessage(role = "assistant", content = "Hi there!")
)
storage.setChatHistory(messages)

// Load chat history
val loaded = storage.getChatHistory()
// → List<ChatMessage> (or emptyList() if error/no data)

// Clear chat history
storage.clearChatHistory()

// Save API settings (JSON string)
val settingsJson = """{"model":"glm-5","temperature":0.7}"""
storage.setApiSettings(settingsJson)

// Load API settings
val loadedSettings = storage.getApiSettings()
// → String? (null if no data)
```

### Integration in main.kt

```kotlin
import storage.LocalStorageService
import kotlinx.serialization.json.Json

fun main() {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    val storage = LocalStorageService(json)
    
    // Pass to components
    val rootComponent = DefaultRootComponent(
        componentContext = DefaultComponentContext(lifecycle),
        storage = storage
    )
}
```

### Integration in ChatStoreFactory

```kotlin
import storage.StorageService

class ChatStoreFactory(
    private val repository: ChatRepository,
    private val storeFactory: StoreFactory,
    private val storage: StorageService
) {
    fun create(): ChatStore = object : ChatStore, Store<ChatIntent, ChatState, Nothing> by storeFactory.create(
        initialState = ChatState(),
        bootstrapper = Bootstrapper {
            // Load chat history on startup
            scope.launch {
                val history = storage.getChatHistory()
                dispatch(ChatAction.HistoryLoaded(history))
            }
        },
        // ...
    ) {
        // Save history on changes
        private fun saveHistory(messages: List<ChatMessage>) {
            scope.launch {
                storage.setChatHistory(messages)
            }
        }
    }
}
```

### Integration in SettingsStoreFactory

```kotlin
import storage.StorageService

class SettingsStoreFactory(
    private val storageService: StorageService,
    // ...
) {
    fun create(): SettingsStore = object : SettingsStore, Store<SettingsIntent, SettingsState, Nothing> by storeFactory.create(
        initialState = SettingsState(),
        bootstrapper = Bootstrapper {
            // Load API settings on startup
            scope.launch {
                val settingsJson = storageService.getApiSettings()
                if (settingsJson != null) {
                    val settings = json.decodeFromString<ApiSettings>(settingsJson)
                    dispatch(SettingsAction.SettingsLoaded(settings))
                }
            }
        },
        // ...
    ) {
        // Save settings on changes
        private fun saveSettings(settings: ApiSettings) {
            scope.launch {
                val settingsJson = json.encodeToString(settings)
                storageService.setApiSettings(settingsJson)
            }
        }
    }
}
```

## Technical Details

### Web Storage API (localStorage)

**External Declarations**:
- `external interface Storage` — JavaScript Storage interface
- `external val localStorage: Storage` — global localStorage object
- `external object console` — JavaScript console for error logging

**Storage Methods**:
- `getItem(key: String): String?` — get value by key (null if not found)
- `setItem(key: String, value: String)` — set value (throws QuotaExceededError if quota exceeded)
- `removeItem(key: String)` — remove key (no error if key doesn't exist)

**Browser Support**:
- All modern browsers (Chrome, Firefox, Safari, Edge)
- 5-10 MB storage quota (varies by browser)
- Same-origin policy (domain-specific storage)
- Persistent across browser sessions

### JSON Serialization

**Configuration**:
```kotlin
Json {
    ignoreUnknownKeys = true  // Skip unknown fields during decode
}
```

**Supported Types**:
- `List<ChatMessage>` — chat history (array of ChatMessage objects)
- `String` — API settings (JSON string, caller responsible for serialization)

**Error Handling**:
- Decode errors → `emptyList()` (chat history)
- Encode errors → logged to console.error
- Invalid JSON → caught and handled gracefully

### Error Handling Strategy

**Philosophy**: Graceful degradation, no exceptions to callers

**Pattern**:
```kotlin
override suspend fun getChatHistory(): List<ChatMessage> {
    return try {
        // Operation
        json.decodeFromString(stored)
    } catch (e: Exception) {
        // Fallback: emptyList()
        emptyList()
    }
}
```

**Error Logging**:
- All errors logged via `console.error()`
- Messages include context: "Failed to save chat history: ${e.message}"
- No exceptions propagated to callers

**Error Scenarios**:
| Operation | Error | Fallback |
|-----------|-------|----------|
| `getChatHistory()` | Decode error | `emptyList()` |
| `setChatHistory()` | Encode error | Log + no-op |
| `setChatHistory()` | QuotaExceededError | Log + no-op |
| `setApiSettings()` | QuotaExceededError | Log + no-op |
| `getApiSettings()` | Never fails | `null` if not found |
| `clearChatHistory()` | Never fails | No-op |

## Constraints and Limitations

### localStorage Quota

**Browser Limits**:
- 5 MB typical quota (varies by browser)
- 10 MB in some browsers (Chrome, Firefox)
- QuotaExceededError when exceeded

**Impact on Chat History**:
- Average message size: ~100-500 bytes (JSON)
- Estimated capacity: ~10,000-50,000 messages
- Mitigation: implement message limit (e.g., keep last 1000 messages)

**Mitigation Strategies**:
```kotlin
// Option 1: Limit message count
val limitedMessages = messages.takeLast(1000)
storage.setChatHistory(limitedMessages)

// Option 2: Clear old messages periodically
if (messages.size > 1000) {
    storage.clearChatHistory()
    storage.setChatHistory(messages.takeLast(500))
}

// Option 3: Compress large messages (not implemented)
```

### Sync API

**Current Implementation**: All methods are synchronous localStorage calls

**Why `suspend`?**
- Interface design for future async implementations (e.g., IndexedDB)
- Consistency with repository pattern
- Coroutines-friendly (can be called from coroutine scope)

**Performance**:
- localStorage is synchronous and blocking
- Fast for small data (< 100 KB)
- Can block main thread for large data (> 1 MB)
- No performance issues in practice for chat history

**Future Improvements**:
```kotlin
// Potential async implementation (IndexedDB)
class IndexedDBStorageService : StorageService {
    override suspend fun getChatHistory(): List<ChatMessage> {
        // Async IndexedDB operations
        return withContext(Dispatchers.Default) {
            // ...
        }
    }
}
```

### Browser Compatibility

**Web Storage API**:
- ✅ All modern browsers
- ✅ WASM-compatible
- ❌ No Node.js support (requires browser environment)

**Testing Constraints**:
- Cannot test in pure Kotlin/JVM
- Requires WASM browser environment
- Tests in composeApp use FakeStorageService

### Data Persistence

**Lifetime**:
- Persistent across browser sessions
- Survives page reload
- Cleared when user clears browser data
- Domain-specific (same origin)

**No Encryption**:
- Data stored in plain text
- Sensitive data (API keys) should be encrypted (not implemented)
- Recommendation: warn users about API key storage

### Error Recovery

**Current Behavior**:
- Errors → fallback values (emptyList, null)
- No retry mechanism
- No user notification

**Improvement Opportunities**:
```kotlin
// Potential: return Result type
override suspend fun setChatHistory(messages: List<ChatMessage>): Result<Unit>

// Potential: notify user on error
override suspend fun setChatHistory(messages: List<ChatMessage>) {
    try {
        // ...
    } catch (e: Exception) {
        console.error("Failed to save chat history: ${e.message}")
        // Emit event to notify user
        onError("Failed to save chat history. Your data may be lost.")
    }
}
```

## Testing Guidelines

### Test Files (in composeApp)

| File | Назначение |
|------|------------|
| `composeApp/.../SettingsStoreTest.kt` | Uses FakeStorageService for testing |
| `composeApp/.../ChatStoreTest.kt` | Uses FakeStorageService for testing |

### FakeStorageService Example

```kotlin
import storage.StorageService
import model.ChatMessage

class FakeStorageService : StorageService {
    private var chatHistory: List<ChatMessage> = emptyList()
    private var apiSettings: String? = null
    
    override suspend fun getChatHistory(): List<ChatMessage> {
        return chatHistory
    }
    
    override suspend fun setChatHistory(messages: List<ChatMessage>) {
        chatHistory = messages
    }
    
    override suspend fun clearChatHistory() {
        chatHistory = emptyList()
    }
    
    override suspend fun getApiSettings(): String? {
        return apiSettings
    }
    
    override suspend fun setApiSettings(settings: String) {
        apiSettings = settings
    }
}
```

**Usage in Tests**:
```kotlin
class SettingsStoreTest {
    private lateinit var storageService: FakeStorageService
    
    @BeforeTest
    fun setup() {
        storageService = FakeStorageService()
    }
    
    @Test
    fun testSaveAndLoadSettings() = runTest {
        // Save settings
        val settings = """{"model":"glm-5"}"""
        storageService.setApiSettings(settings)
        
        // Load settings
        val loaded = storageService.getApiSettings()
        assertEquals(settings, loaded)
    }
}
```

### No Tests in core/storage

**Reason**: localStorage requires browser environment (WASM)

**Alternative**: Integration tests in composeApp module

## Критичные правила

- **Interface + Implementation**: StorageService interface, LocalStorageService implementation
- **Graceful degradation**: Errors → fallback values (emptyList, null), no exceptions
- **suspend functions**: All methods are suspend для consistency (хотя localStorage sync)
- **console.error**: Errors logged to console (no proper logging library)
- **Detekt**: БЕЗ `@Suppress` (исключение: build.gradle.kts)
- **No comments**: Код self-documenting
- **External declarations**: Web Storage API declared as external
- **JSON configuration**: `ignoreUnknownKeys = true` для flexibility

## Workflow при изменениях

1. Изменил code → `./gradlew :core:storage:check`
2. Style issues → `./gradlew :core:storage:ktlintFormat`
3. Проверил integration → `./gradlew :feature:chat:check :feature:settings:check`
4. Проверил tests → `./gradlew :composeApp:test --tests "*Storage*" --tests "*Settings*"`
5. Commit

## Частые задачи

### Добавить новое хранилище данных

1. Добавить методы в StorageService interface:
```kotlin
interface StorageService {
    // existing methods
    suspend fun getUserPreferences(): String?
    suspend fun setUserPreferences(preferences: String)
}
```

2. Реализовать в LocalStorageService:
```kotlin
class LocalStorageService : StorageService {
    // existing implementation
    
    override suspend fun getUserPreferences(): String? {
        return localStorage.getItem(USER_PREFERENCES_KEY)
    }
    
    override suspend fun setUserPreferences(preferences: String) {
        try {
            localStorage.setItem(USER_PREFERENCES_KEY, preferences)
        } catch (e: Exception) {
            console.error("Failed to save user preferences: ${e.message}")
        }
    }
    
    companion object {
        // existing keys
        private const val USER_PREFERENCES_KEY = "user_preferences"
    }
}
```

3. Обновить usage в feature modules
4. Написать tests в composeApp

### Изменить error handling

1. Добавить callback/interface для error notification:
```kotlin
interface StorageService {
    var onError: ((String) -> Unit)?
    // ...
}

class LocalStorageService : StorageService {
    override var onError: ((String) -> Unit)? = null
    
    override suspend fun setChatHistory(messages: List<ChatMessage>) {
        try {
            // ...
        } catch (e: Exception) {
            val message = "Failed to save chat history: ${e.message}"
            console.error(message)
            onError?.invoke(message)
        }
    }
}
```

2. Обновить usage в main.kt:
```kotlin
val storage = LocalStorageService(json).apply {
    onError = { msg -> println("Storage error: $msg") }
}
```

3. Написать tests

### Добавить message limit

1. Изменить setChatHistory в LocalStorageService:
```kotlin
override suspend fun setChatHistory(messages: List<ChatMessage>) {
    try {
        val limited = messages.takeLast(1000)  // Keep last 1000 messages
        val encoded = json.encodeToString(limited)
        localStorage.setItem(CHAT_HISTORY_KEY, encoded)
    } catch (e: Exception) {
        console.error("Failed to save chat history: ${e.message}")
    }
}
```

2. Добавить константу для limit:
```kotlin
companion object {
    private const val CHAT_HISTORY_KEY = "chat_history"
    private const val API_SETTINGS_KEY = "api_settings"
    private const val MAX_MESSAGES = 1000
}
```

3. Протестировать с большим количеством сообщений

### Заменить на async storage (IndexedDB)

1. Создать новую implementation:
```kotlin
class IndexedDBStorageService : StorageService {
    override suspend fun getChatHistory(): List<ChatMessage> {
        // IndexedDB async operations
    }
}
```

2. Обновить initialization в main.kt:
```kotlin
val storage = IndexedDBStorageService()
```

3. Протестировать в браузере

### Добавить encryption для API keys

1. Добавить encryption helper:
```kotlin
private fun encrypt(data: String): String {
    // Simple encryption (for demo, use proper encryption in production)
    return data.toByteArray().joinToString(",") { it.toString() }
}

private fun decrypt(data: String): String {
    return data.split(",").map { it.toByte() }.toByteArray().toString(Charsets.UTF_8)
}
```

2. Изменить setApiSettings:
```kotlin
override suspend fun setApiSettings(settings: String) {
    try {
        val encrypted = encrypt(settings)
        localStorage.setItem(API_SETTINGS_KEY, encrypted)
    } catch (e: Exception) {
        console.error("Failed to save API settings: ${e.message}")
    }
}
```

3. Изменить getApiSettings:
```kotlin
override suspend fun getApiSettings(): String? {
    val encrypted = localStorage.getItem(API_SETTINGS_KEY) ?: return null
    return try {
        decrypt(encrypted)
    } catch (e: Exception) {
        console.error("Failed to decrypt API settings: ${e.message}")
        null
    }
}
```

4. Протестировать

## Особенности модуля

- **Core infrastructure**: Базовый модуль для persistent storage
- **Web Storage API**: Использует browser localStorage (sync API)
- **WASM-compatible**: Оптимизирован для Kotlin/Wasm target
- **External declarations**: Web Storage API declared as external interfaces
- **Graceful degradation**: Errors → fallback values (emptyList, null), no exceptions
- **console.error**: Error logging via JavaScript console (no proper logging library)
- **JSON serialization**: Kotlinx serialization с ignoreUnknownKeys
- **suspend functions**: All methods suspend для consistency (future async implementations)
- **Minimal dependencies**: Только core:model + kotlinx-serialization-json
- **No tests in module**: Tests in composeApp using FakeStorageService
- **Persistent storage**: Data survives page reload and browser sessions
- **Browser-only**: No Node.js/JVM support (requires browser environment)
- **5-10 MB quota**: Browser localStorage limits (QuotaExceededError)
- **Same-origin policy**: Domain-specific storage
- **Two storage keys**: chat_history (List<ChatMessage>), api_settings (String)
- **Interface + Implementation**: StorageService interface, LocalStorageService implementation
- **Future-ready**: Interface supports async implementations (IndexedDB, server-side storage)
