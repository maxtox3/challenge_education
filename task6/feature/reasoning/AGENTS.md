# AGENTS.md — Reasoning Module

## Назначение

Reasoning feature — UI-only модуль для сравнения разных способов рассуждения AI моделей. Отображение результатов в табличном формате с метриками для 4 режимов: Direct, Step-by-Step, Meta-Prompt, Expert Panel.

## Команды

```bash
./gradlew :feature:reasoning:check              # Tests + lint + detekt для модуля
./gradlew :feature:reasoning:ktlintFormat       # Auto-fix style
./gradlew :feature:reasoning:compileKotlinWasmJs # Проверка компиляции
```

## Архитектура

**Module Type**: UI-only (no ViewModel, Repository, business logic)

**Pattern**: Stateless Composable + Internal State (tabs, input)

**Module Dependencies**:
```
feature:reasoning
├── core:model   (ReasoningComparison, ReasoningMode, ReasoningResult)
├── core:ui      (shared components, AppColors, primaryButtonColors)
└── markdown renderer (multiplatform-markdown-renderer-m3)
```

**Integration**:
- Used by: `feature:chat` → `App.kt`
- Tested in: `composeApp/src/wasmJsTest/kotlin/ui/ReasoningDialogUiRealTest.kt`
- Business logic: `feature:chat` → `ChatRepository.runReasoningComparison()`

## Ключевые файлы

| Файл | Назначение |
|------|------------|
| `ui/ReasoningDialog.kt` | Главный Composable, tabs, loading/error states |
| `ui/ReasoningDialogComponents.kt` | Вспомогательные компоненты (MetricBadge, ComparisonTable, SystemPromptCard) |
| `ui/ReasoningDialogTags.kt` | Test tags для UI тестов (16 tags) |

## API

### ReasoningDialog
```kotlin
@Composable
fun ReasoningDialog(
    comparison: ReasoningComparison,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRunComparison: (task: String) -> Unit,
)
```

**Parameters**:
- `comparison: ReasoningComparison` — данные для сравнения (task + results map)
- `isLoading: Boolean` — глобальное состояние загрузки (для кнопки)
- `onDismiss: () -> Unit` — callback для закрытия диалога
- `onRunComparison: (task: String) -> Unit` — callback для запуска сравнения

## Reasoning Modes

4 режима рассуждения с разными подходами:

| Mode | DisplayName | Description |
|------|-------------|-------------|
| `DIRECT` | Прямой ответ | Без дополнительных инструкций |
| `STEP_BY_STEP` | Пошагово | Инструкция: решай пошагово |
| `META_PROMPT` | Мета-промпт | Сначала составляет промпт, затем использует его |
| `EXPERT_PANEL` | Эксперты | Группа экспертов: аналитик, инженер, критик |

```kotlin
enum class ReasoningMode(val displayName: String, val description: String) {
    DIRECT("Прямой ответ", "Без дополнительных инструкций"),
    STEP_BY_STEP("Пошагово", "Инструкция: решай пошагово"),
    META_PROMPT("Мета-промпт", "Сначала составляет промпт, затем использует его"),
    EXPERT_PANEL("Эксперты", "Группа экспертов: аналитик, инженер, критик"),
}
```

## Data Models

### ReasoningResult (from core:model)
```kotlin
data class ReasoningResult(
    val mode: ReasoningMode,
    val systemPrompt: String,
    val actualPrompt: String,
    val response: String = "",
    val responseTimeMs: Long = 0,
    val tokensUsed: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)
```

### ReasoningComparison (from core:model)
```kotlin
data class ReasoningComparison(
    val task: String,
    val results: Map<ReasoningMode, ReasoningResult>,
) {
    val isComplete: Boolean
        get() = results.values.all { !it.isLoading && it.error == null && it.response.isNotEmpty() }

    val hasAnyResult: Boolean
        get() = results.values.any { it.response.isNotEmpty() }
}
```

**Computed Properties**:
- `isComplete` — все 4 режима завершились успешно
- `hasAnyResult` — хотя бы один режим имеет результат

## UI Components Structure

```
ReasoningDialog
└── Dialog (usePlatformDefaultWidth = false)
    └── Surface (90% height)
        └── Column
            ├── DialogHeader (title + close button)
            ├── TaskInputSection
            │   ├── OutlinedTextField (task input)
            │   └── Button (run comparison)
            └── TabContentSection
                ├── ScrollableTabRow (4 tabs with status indicators)
                ├── ResultContentArea
                │   ├── Loading Indicator (when isLoading)
                │   ├── Error Text (when error != null)
                │   └── Result Content (Markdown + metrics)
                └── ComparisonTable (when isComplete)
```

### Dialog Components

**DialogHeader**: Title + Close button

**TaskInputSection**:
- OutlinedTextField для ввода задачи
- Button "Запустить сравнение" (disabled when loading or blank task)

**TabContentSection**:
- ScrollableTabRow с 4 вкладками
- Status indicators в названиях вкладок:
  - `[..]` — loading
  - `[X]` — error
  - `[OK]` — success

**ResultContentArea**:
- Loading indicator при `isLoading == true`
- Error text при `error != null`
- Markdown content при наличии response
- SystemPromptCard + response + MetricBadge

**ComparisonTable**:
- Отображается только когда `isComplete == true`
- Сводная таблица по всем 4 режимам
- Колонки: Режим, Время, Токены, Длина

## Test Tags

```kotlin
object ReasoningDialogTags {
    const val DIALOG_SURFACE = "reasoning_dialog_surface"
    const val ROOT = "reasoning_dialog_root"
    const val EMPTY_STATE = "reasoning_dialog_empty_state"
    const val TITLE = "reasoning_dialog_title"
    const val CLOSE_BUTTON = "reasoning_dialog_close_button"
    const val TASK_INPUT_FIELD = "reasoning_dialog_task_input_field"
    const val RUN_BUTTON = "reasoning_dialog_run_button"
    const val TAB_ROW = "reasoning_dialog_tab_row"
    const val COMPARISON_TABLE = "reasoning_dialog_comparison_table"
    const val CONTENT_AREA = "reasoning_dialog_content_area"
    const val LOADING_INDICATOR = "reasoning_dialog_loading_indicator"
    const val ERROR_TEXT = "reasoning_dialog_error_text"
}
```

**Usage в тестах**:
```kotlin
onNodeWithTag(ReasoningDialogTags.ROOT).assertExists()
onNodeWithTag(ReasoningDialogTags.RUN_BUTTON).performClick()
onNodeWithTag(ReasoningDialogTags.TAB_ROW).assertExists()
```

## Функционал

### Task Input
- Поле ввода для задачи (по умолчанию из `comparison.task`)
- Валидация: кнопка disabled при пустом поле
- Синхронизация с internal state

### Run Comparison
- Запускает сравнение всех 4 режимов через `onRunComparison(task)`
- Кнопка disabled во время loading
- Показывает CircularProgressIndicator при loading

### Tabs Navigation
- 4 вкладки для каждого ReasoningMode
- Status indicators в названиях вкладок
- Переключение контента при выборе вкладки

### Status Indicators
```kotlin
internal fun getStatusText(result: ReasoningResult?): String = when {
    result?.isLoading == true -> " [..]"
    result?.error != null -> " [X]"
    result?.response?.isNotEmpty() == true -> " [OK]"
    else -> ""
}
```

### Markdown Rendering
- Используется `multiplatform-markdown-renderer-m3`
- Отображение ответов моделей с markdown форматированием

### Metrics Display
- MetricBadge компонент для отображения метрик
- Показывает: Время (ms), Токены
- Badge styling с AppColors

### Comparison Table
- Отображается только когда все результаты готовы (`isComplete`)
- Строки: по одной для каждого ReasoningMode
- Колонки: Режим, Время, Токены, Длина ответа

## Internal Components

### MetricBadge
```kotlin
@Composable
internal fun MetricBadge(label: String, value: String)
```
Badge для отображения метрик (Время, Токены).

### SystemPromptCard
```kotlin
@Composable
internal fun SystemPromptCard(systemPrompt: String)
```
Карточка с системным промптом для текущего режима.

### ComparisonTable
```kotlin
@Composable
internal fun ComparisonTable(comparison: ReasoningComparison, modifier: Modifier = Modifier)
```
Сводная таблица с метриками для всех режимов.

### getStatusText
```kotlin
internal fun getStatusText(result: ReasoningResult?): String
```
Возвращает status indicator для вкладки ([..], [X], [OK]).

## Константы

```kotlin
private const val DIALOG_HEIGHT_FRACTION = 0.9f
```

## Тестирование

| Тип | Файлы | Назначение |
|-----|-------|------------|
| Unit | `composeApp/.../ReasoningDialogTest.kt` | Tab selection, button states, callbacks |
| UI | `composeApp/.../ReasoningDialogUiRealTest.kt` | Compose interactions, all test tags |
| Baseline | `composeApp/.../ReasoningBaselineTest.kt` | Characterization tests |

**Тест-кейсы**:
- Tab selection (default + переключение между modes)
- Run button enabled/disabled states
- Empty state отображение
- Loading indicator отображение
- Error text отображение
- Comparison table отображение (только при isComplete)
- Task input field interaction
- Dismiss callback

## Критичные правила

- **Stateless composable**: Но с internal state для tabs/input
- **No business logic**: Вся логика в `feature:chat` (ChatRepository)
- **Detekt**: БЕЗ `@Suppress`
- **No comments**: Код self-documenting (исключение: KDoc для public API)
- **Markdown**: Используется multiplatform-markdown-renderer-m3
- **All 4 modes**: Все ReasoningMode должны поддерживаться
- **Status indicators**: Обязательны для tabs

## Integration Example

### В feature:chat (App.kt)
```kotlin
import reasoning.ui.ReasoningDialog

@Composable
fun App(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsState()
    
    // ... other UI code ...
    
    if (state.showReasoning) {
        ReasoningDialog(
            comparison = state.reasoningComparison,
            isLoading = state.isReasoningLoading,
            onDismiss = { viewModel.processIntent(ToggleReasoning(false)) },
            onRunComparison = { task ->
                viewModel.processIntent(RunReasoningComparison(task))
            }
        )
    }
}
```

### ChatState integration
```kotlin
data class ChatState(
    // ... other fields ...
    val reasoningComparison: ReasoningComparison = ReasoningComparison(
        task = "Default task...",
        results = emptyMap(),
    ),
    val isReasoningLoading: Boolean = false,
    val showReasoning: Boolean = false,
)
```

### ChatRepository integration
```kotlin
suspend fun runReasoningComparison(
    task: String,
    settings: ApiSettings,
    onProgress: (ReasoningComparison) -> Unit,
): ReasoningComparison
```

## Workflow при изменениях

1. Изменил UI → `./gradlew :feature:reasoning:check`
2. Style issues → `./gradlew :feature:reasoning:ktlintFormat`
3. Проверил integration → `./gradlew :feature:chat:check`
4. Проверил tests → `./gradlew :composeApp:test --tests "*Reasoning*"`
5. Commit

## Частые задачи

### Добавить новый ReasoningMode
1. Добавить enum value в `core:model/ReasoningMode.kt`
2. UI автоматически обновится (ScrollableTabRow использует `ReasoningMode.entries`)
3. Обновить business logic в `ChatRepository.runReasoningComparison()`
4. Добавить тесты для нового режима

### Изменить status indicators
1. Изменить `getStatusText()` в `ReasoningDialogComponents.kt`
2. Обновить тесты в `ReasoningDialogTest.kt`

### Изменить layout диалога
1. Изменить `DIALOG_HEIGHT_FRACTION` константу
2. Или изменить структуру в `ReasoningDialog` composable

### Добавить новую метрику в ComparisonTable
1. Добавить колонку в `ComparisonTableHeader()`
2. Добавить значение в `ComparisonTableRow()`
3. Обновить `ReasoningResult` в `core:model` (если нужно новое поле)

## Особенности модуля

- **4 режима рассуждения** с разными подходами к problem-solving
- **Markdown rendering** для форматированных ответов
- **Status indicators** в tabs для визуального feedback
- **Comparison table** только когда все результаты готовы (`isComplete`)
- **Loading/Error states** для каждого режима отдельно
- **System prompt display** показывает какой промпт использовался
- **Progressive loading** результаты появляются по мере готовности
- **Reusable** может использоваться в других features для сравнения approaches
