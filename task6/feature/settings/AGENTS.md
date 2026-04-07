# AGENTS.md — Settings Module

## Назначение

Settings feature — модуль для управления настройками API (API key, model, temperature, max tokens, stop sequences, response format). Содержит бизнес-логику (ViewModel + MVI) и reusable компонент ModelSelector.

**Current State**: Settings сохраняются в localStorage через `:core:storage` и загружаются при инициализации SettingsStore.

## Команды

```bash
./gradlew :feature:settings:check              # Tests + lint + detekt для модуля
./gradlew :feature:settings:ktlintFormat       # Auto-fix style
./gradlew :feature:settings:compileKotlinWasmJs # Проверка компиляции
```

## Архитектура

**Module Type**: UI + Business Logic (MVI pattern)

**Pattern**: MVI (State + Intent + SideEffect)

**Module Dependencies**:
```
feature:settings
├── core:model   (ModelType)
├── core:network (ResponseConstraints)
├── core:storage (StorageService)
└── core:ui      (shared components, AppColors, DialogSurface, primaryButtonColors)
```

**Integration**:
- Used by: `feature:chat` → `App.kt`
- Tested in: `composeApp/src/wasmJsTest/kotlin/ui/SettingsDialogUiRealTest.kt`
- ViewModel: Опционально (dialog может использовать internal state или ViewModel)

## Current State & Persistence

**Settings persisted to localStorage**

### Current Behavior
- Settings сохраняются в localStorage через `StorageService`
- Settings загружаются при инициализации `SettingsStore`
- `onSave` callback сохраняет настройки и уведомляет parent компонент
- Reset сбрасывает настройки и сохраняет дефолтные значения

### Current Storage Status
| Feature | Status |
|---------|--------|
| In-memory state | ✅ Implemented |
| onSave callback | ✅ Implemented |
| localStorage read | ✅ Implemented |
| localStorage write | ✅ Implemented |
| :core:storage module | ✅ Implemented |

## Ключевые файлы

| Файл | Назначение |
|------|------------|
| `SettingsState.kt` | ViewModel, State, Intent, SideEffect, ApiSettings data class |
| `ui/SettingsDialog.kt` | Главный Composable, form inputs, validation UI |
| `ui/ModelSelector.kt` | Reusable dropdown для выбора модели |
| `ui/SettingsDialogTags.kt` | Test tags для SettingsDialog (13 tags) |
| `ui/ModelSelectorTags.kt` | Test tags для ModelSelector (4 tags) |

## MVI Components

### SettingsViewModel
```kotlin
class SettingsViewModel(
    private val viewModelScope: CoroutineScope,
    private val onSettingsSaved: (ApiSettings) -> Unit = {},
) {
    val uiState: StateFlow<SettingsState>
    val sideEffects: SharedFlow<SettingsSideEffect>
    
    fun processIntent(intent: SettingsIntent)
    fun updateSettings(newSettings: ApiSettings)
    fun loadSettings(settings: ApiSettings)
}
```

**Responsibilities**:
- State management (ApiSettings, validation, loading)
- Validation logic (API key, temperature range)
- Settings callback + persistence to localStorage
- Side effects (show toast, validation errors)

### SettingsState
```kotlin
data class SettingsState(
    val settings: ApiSettings = ApiSettings(),
    val isLoading: Boolean = false,
    val validationError: String? = null,
    val isApiKeyVisible: Boolean = false,
)
```

### ApiSettings
```kotlin
data class ApiSettings(
    val apiKey: String = "",
    val model: String = ModelType.PRO.id,
    val maxTokens: Int? = null,
    val temperature: Double = 1.0,
    val stopSequences: String = "",
    val responseFormat: String = "text",
) {
    fun toResponseConstraints(): ResponseConstraints
}
```

**Fields**:
- `apiKey: String` — API ключ для авторизации
- `model: String` — ID модели (из ModelType)
- `maxTokens: Int?` — лимит токенов (null = unlimited)
- `temperature: Double` — temperature для генерации (0.0 - 2.0)
- `stopSequences: String` — stop sequences (comma-separated)
- `responseFormat: String` — формат ответа ("text" или "json")

### SettingsIntent (11 intents)
```kotlin
sealed class SettingsIntent {
    data class UpdateApiKey(val apiKey: String) : SettingsIntent()
    data class UpdateModel(val model: String) : SettingsIntent()
    data class UpdateMaxTokens(val maxTokens: Int?) : SettingsIntent()
    data class UpdateTemperature(val temperature: Double) : SettingsIntent()
    data class UpdateStopSequences(val stopSequences: String) : SettingsIntent()
    data class UpdateResponseFormat(val responseFormat: String) : SettingsIntent()
    data class UpdateSettings(val settings: ApiSettings) : SettingsIntent()
    data object SaveSettings : SettingsIntent()
    data object ResetSettings : SettingsIntent()
    data object ClearValidationError : SettingsIntent()
    data object ToggleApiKeyVisibility : SettingsIntent()
    data class ValidateApiKey(val apiKey: String) : SettingsIntent()
}
```

### SettingsSideEffect
```kotlin
sealed class SettingsSideEffect {
    data class ShowToast(val message: String) : SettingsSideEffect()
    data object SettingsSaved : SettingsSideEffect()
    data class ValidationError(val error: String) : SettingsSideEffect()
}
```

## UI Components Structure

```
SettingsDialog
└── Dialog
    └── DialogSurface
        └── Column (scrollable)
            ├── Title ("API Settings")
            ├── ApiKeyInput
            │   └── OutlinedTextField (API key)
            ├── ModelSelector
            │   └── TextField + DropdownMenu (4 models)
            ├── MaxTokensInput
            │   └── OutlinedTextField (digits only)
            ├── TemperatureInput
            │   ├── Text (current value)
            │   └── Slider (0.0 - 2.0)
            ├── StopSequencesInput
            │   └── OutlinedTextField (comma-separated)
            ├── ResponseFormatInput
            │   └── Row
            │       ├── ResponseFormatChip ("Text")
            │       └── ResponseFormatChip ("JSON")
            └── Buttons
                ├── TextButton ("Cancel")
                └── Button ("Save")
```

### Internal State (Dialog)
```kotlin
private data class SettingsState(
    val apiKey: String,
    val model: String,
    val maxTokensText: String,
    val temperature: Double,
    val stopSequences: String,
    val responseFormat: String,
) {
    fun toApiSettings(): ApiSettings
    companion object {
        fun from(settings: ApiSettings): SettingsState
    }
}
```

## API

### SettingsDialog
```kotlin
@Composable
fun SettingsDialog(
    currentSettings: ApiSettings,
    onDismiss: () -> Unit,
    onSave: (ApiSettings) -> Unit,
)
```

**Parameters**:
- `currentSettings: ApiSettings` — текущие настройки для отображения
- `onDismiss: () -> Unit` — callback для закрытия диалога (Cancel)
- `onSave: (ApiSettings) -> Unit` — callback для сохранения настроек (Save)

### ModelSelector
```kotlin
@Composable
fun ModelSelector(
    selectedModelId: String,
    onModelSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
)
```

**Parameters**:
- `selectedModelId: String` — ID выбранной модели (из ModelType)
- `onModelSelected: (String) -> Unit` — callback при выборе модели
- `modifier: Modifier` — modifier для компонента

### rememberSettingsViewModel
```kotlin
@Composable
fun rememberSettingsViewModel(
    onSettingsSaved: (ApiSettings) -> Unit = {},
): SettingsViewModel
```

**Usage**:
```kotlin
val viewModel = rememberSettingsViewModel { settings ->
    // Handle settings saved
}
```

## Available Models (ModelType)

```kotlin
enum class ModelType(val id: String, val displayName: String, val level: String) {
    LIGHT("glm-4-32b-0414-128k", "GLM-4 32b", "Супер легкая, слабая и дешевая"),
    AIR("glm-4.5-air", "GLM-4.5 Air", "Слабая"),
    MIDDLE("glm-4.7", "GLM-4.7", "Средняя"),
    PRO("glm-5", "GLM-5", "Сильная");

    companion object {
        fun fromId(id: String): ModelType
        val allIds: List<String>
    }
}
```

**Default**: `ModelType.PRO` (glm-5)

## Validation Logic

### Validation Rules
- **API Key**: не пустой (`apiKey.isNotBlank()`)
- **Temperature**: диапазон 0.0 - 2.0 (`temperature in 0.0..2.0`)
- **Max Tokens**: число или null (unlimited)
- **Stop Sequences**: строка (comma-separated, парсится при использовании)
- **Response Format**: "text" или "json"

### Validation in ViewModel
```kotlin
private fun validateSettings(settings: ApiSettings): Boolean {
    val error = when {
        settings.apiKey.isBlank() -> "API key is required"
        settings.temperature !in 0.0..2.0 -> "Temperature must be between 0.0 and 2.0"
        else -> null
    }
    _uiState.update { it.copy(validationError = error) }
    return error == null
}

private fun validateApiKey(apiKey: String) {
    val error = if (apiKey.isBlank()) "API key cannot be empty" else null
    _uiState.update { it.copy(validationError = error) }
}
```

## Test Tags

### SettingsDialogTags (13 tags)
```kotlin
object SettingsDialogTags {
    const val DIALOG_SURFACE = "settings_dialog_surface"
    const val ROOT = "settings_dialog_root"
    const val TITLE = "settings_dialog_title"
    const val API_KEY_FIELD = "settings_dialog_api_key_field"
    const val MODEL_FIELD = "settings_dialog_model_field"
    const val MAX_TOKENS_FIELD = "settings_dialog_max_tokens_field"
    const val TEMPERATURE_SLIDER = "settings_dialog_temperature_slider"
    const val STOP_SEQUENCES_FIELD = "settings_dialog_stop_sequences_field"
    const val TEXT_FORMAT_CHIP = "settings_dialog_text_format_chip"
    const val JSON_FORMAT_CHIP = "settings_dialog_json_format_chip"
    const val CANCEL_BUTTON = "settings_dialog_cancel_button"
    const val SAVE_BUTTON = "settings_dialog_save_button"
}
```

### ModelSelectorTags (4 tags)
```kotlin
object ModelSelectorTags {
    const val ROOT = "model_selector_root"
    const val TEXT_FIELD = "model_selector_text_field"
    const val DROPDOWN = "model_selector_dropdown"
    const val ITEM = "model_selector_item"
}
```

**Usage в тестах**:
```kotlin
onNodeWithTag(SettingsDialogTags.ROOT).assertExists()
onNodeWithTag(SettingsDialogTags.SAVE_BUTTON).performClick()
onNodeWithTag(ModelSelectorTags.TEXT_FIELD).assertExists()
```

## Функционал

### Form Inputs
- **API Key**: OutlinedTextField (password-like)
- **Model**: Dropdown с 4 моделями (LIGHT, AIR, MIDDLE, PRO)
- **Max Tokens**: OutlinedTextField (digits only, empty = unlimited)
- **Temperature**: Slider (0.0 - 2.0) с visual feedback
- **Stop Sequences**: OutlinedTextField (comma-separated)
- **Response Format**: Chips (Text / JSON toggle)

### Model Selection
- Dropdown с 4 моделями
- Display: "GLM-5 (Сильная)"
- Selection: `onModelSelected(model.id)`

### Temperature Slider
- Range: 0.0f - 2.0f
- Display: "(temperature * 100).roundToInt() / 100.0"
- Colors: Primary thumb and track

### Response Format Chips
- Two chips: "Text" и "JSON"
- Selected chip: Primary background, white text
- Unselected chip: SurfaceLight background, secondary text

### Save/Cancel Actions
- **Cancel**: вызывает `onDismiss()`
- **Save**: вызывает `onSave(state.toApiSettings())`
  - Settings сохраняются в localStorage через `SettingsStore`

## Two Usage Modes

### Mode 1: Internal State (Simple)
```kotlin
SettingsDialog(
    currentSettings = currentSettings,
    onDismiss = { showSettings = false },
    onSave = { newSettings ->
        viewModel.processIntent(UpdateSettings(newSettings))
        showSettings = false
    }
)
```

### Mode 2: ViewModel (Full MVI)
```kotlin
val viewModel = rememberSettingsViewModel { settings ->
    // Handle settings saved
}

SettingsDialog(
    currentSettings = viewModel.state.settings,
    onDismiss = { viewModel.processIntent(ClearValidationError) },
    onSave = { viewModel.processIntent(SaveSettings) }
)
```

## Integration Example

### В feature:chat (App.kt)
```kotlin
import settings.ApiSettings
import settings.ui.SettingsDialog

@Composable
fun App(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsState()
    
    // ... other UI code ...
    
    if (state.showSettings) {
        SettingsDialog(
            currentSettings = state.settings,
            onDismiss = { viewModel.processIntent(ToggleSettings(false)) },
            onSave = { newSettings ->
                viewModel.processIntent(UpdateSettings(newSettings))
                viewModel.processIntent(ToggleSettings(false))
            }
        )
    }
}
```

### ChatState integration
```kotlin
data class ChatState(
    // ... other fields ...
    val settings: ApiSettings = ApiSettings(),
    val showSettings: Boolean = false,
)
```

### ApiSettings → ResponseConstraints
```kotlin
val settings: ApiSettings = state.settings
val constraints = settings.toResponseConstraints()

client.sendMessage(
    apiKey = settings.apiKey,
    model = settings.model,
    prompt = prompt,
    constraints = constraints
)
```

## Тестирование

| Тип | Файлы | Назначение |
|-----|-------|------------|
| Unit | `composeApp/.../SettingsDialogTest.kt` | Field binding, state conversion |
| UI | `composeApp/.../SettingsDialogUiRealTest.kt` | Compose interactions, all test tags |

**Тест-кейсы**:
- Dialog отображение (all fields exist)
- Field binding (initial values)
- Input interactions (text input, slider, chips)
- Save/Cancel callbacks
- Validation (API key required, temperature range)
- Model selection (dropdown)
- State conversion (ApiSettings ↔ internal state)

## Критичные правила

- **MVI pattern**: State + Intent + SideEffect (как в chat module)
- **Validation**: перед сохранением настроек
- **Detekt**: БЕЗ `@Suppress`
- **No comments**: Без inline-комментариев. KDoc только для public API
- **Reusable**: ModelSelector можно использовать отдельно от SettingsDialog
- **Two modes**: Dialog с internal state ИЛИ с ViewModel
- **Type conversion**: `ApiSettings.toResponseConstraints()` для API calls

## Workflow при изменениях

1. Изменил code → `./gradlew :feature:settings:check`
2. Style issues → `./gradlew :feature:settings:ktlintFormat`
3. Проверил integration → `./gradlew :feature:chat:check`
4. Проверил tests → `./gradlew :composeApp:test --tests "*Settings*"`
5. Commit

## Частые задачи

### Добавить новое поле в ApiSettings
1. Добавить поле в `ApiSettings` data class
2. Обновить `toResponseConstraints()` (если нужно)
3. Добавить Intent: `UpdateNewField`
4. Добавить UI input в `SettingsDialogContent`
5. Обновить internal `SettingsState` в dialog
6. Добавить test tag
7. Написать тесты

### Изменить список моделей
1. Изменить `ModelType` enum в `core:model`
2. ModelSelector автоматически обновится (использует `ModelType.entries`)
3. Обновить tests

### Изменить validation rules
1. Изменить `validateSettings()` в ViewModel
2. Добавить/изменить error messages
3. Обновить tests

### Добавить новый ResponseFormat
1. Добавить chip в `ResponseFormatInput`
2. Обновить `toResponseConstraints()` в ApiSettings
3. Добавить test tag
4. Написать тесты

### Обновить persistence behavior
1. Изменить логику сериализации `ApiSettings` (если формат поменялся)
2. Обновить чтение/запись в `SettingsStoreFactory`
3. Добавить миграцию данных (если меняются ключи/формат)
4. Обновить тесты
5. Обновить документацию

## Особенности модуля

- **Business logic**: В отличие от metrics/reasoning, имеет полноценный ViewModel
- **Reusable component**: ModelSelector можно использовать отдельно (например, в quick settings)
- **Validation**: Валидация перед сохранением (API key required, temperature range)
- **State management**: Может работать с internal state (простой режим) или ViewModel (полный MVI)
- **Type conversion**: `ApiSettings.toResponseConstraints()` конвертирует в формат для API calls
- **Two chips**: Text/JSON selection для response format
- **Temperature visual feedback**: Slider показывает текущее значение
- **Comma-separated parsing**: Stop sequences парсятся из строки в список
- **Persistence**: Settings сохраняются в localStorage через `StorageService`
