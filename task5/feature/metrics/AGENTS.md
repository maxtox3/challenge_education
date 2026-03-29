# AGENTS.md — Metrics Module

## Назначение

Metrics feature — UI-only модуль для отображения метрик запросов к AI моделям. Сравнение response time, tokens, length и constraints в табличном формате.

## Команды

```bash
./gradlew :feature:metrics:check              # Tests + lint + detekt для модуля
./gradlew :feature:metrics:ktlintFormat       # Auto-fix style
./gradlew :feature:metrics:compileKotlinWasmJs # Проверка компиляции
```

## Архитектура

**Module Type**: UI-only (no ViewModel, Repository, business logic)

**Pattern**: Stateless Composable

**Module Dependencies**:
```
feature:metrics
├── core:model   (MetricRecord, ConstraintsInfo)
└── core:ui      (DialogHeader, DialogSurface, EmptyStateBox, SectionSpacer)
```

**Integration**:
- Used by: `feature:chat` → `App.kt`
- Tested in: `composeApp/src/wasmJsTest/kotlin/ui/MetricsDialogUiRealTest.kt`

## Ключевые файлы

| Файл | Назначение |
|------|------------|
| `ui/MetricsDialog.kt` | Главный Composable, отображение таблицы метрик |
| `ui/MetricsDialogTags.kt` | Test tags для UI тестов |

## API

### MetricsDialog
```kotlin
@Composable
fun MetricsDialog(
    metrics: List<MetricRecord>,
    onDismiss: () -> Unit
)
```

**Parameters**:
- `metrics: List<MetricRecord>` — список метрик для отображения
- `onDismiss: () -> Unit` — callback для закрытия диалога

## Функционал

### Группировка по Prompt
- Метрики группируются по `prompt` полю
- Для каждого prompt отображается отдельная таблица
- Prompt обрезается до 50 символов с добавлением "..."

### Таблица метрик
Отображаемые колонки:
- **Metric** — название метрики
- **[Constraints]** — колонки для каждой конфигурации (maxTokens, temperature, etc.)

Отображаемые метрики:
- `Length (chars)` — длина ответа в символах
- `Tokens` — использованные токены (или "-" если null)
- `Max Tokens` — лимит токенов (или "unlimited" если null)
- `Finish Reason` — причина завершения (или "-")
- `Time (ms)` — время ответа в миллисекундах

### Empty State
При отсутствии метрик отображается сообщение:
```
No metrics yet.
Send some messages to see comparison.
```

### Scrollable Content
- Вертикальный скролл для длинных списков
- Горизонтальный скролл для широких таблиц

## Data Models

### MetricRecord (from core:model)
```kotlin
data class MetricRecord(
    val id: Int,
    val prompt: String,
    val response: String,
    val mode: String,
    val responseLength: Int,
    val tokensUsed: Int?,
    val maxTokens: Int?,
    val finishReason: String?,
    val responseTimeMs: Long,
    val constraints: ConstraintsInfo,
)
```

### ConstraintsInfo (from core:model)
```kotlin
data class ConstraintsInfo(
    val maxTokens: Int?,
    val stopSequences: List<String>,
    val responseFormat: String,
    val temperature: Double,
) {
    fun toDisplayString(): String
}
```

`toDisplayString()` форматирует constraints для заголовков колонок:
- `maxTokens=100` → "maxTokens=100"
- `temperature=0.7` → "temp=0.7"
- Empty constraints → "Free"

## Test Tags

```kotlin
object MetricsDialogTags {
    const val ROOT = "metrics_dialog_root"
    const val CONTENT_COLUMN = "metrics_dialog_content_column"
    const val EMPTY_STATE = "metrics_dialog_empty_state"
    const val METRICS_LIST = "metrics_dialog_metrics_list"
    const val PROMPT_TEXT = "metrics_dialog_prompt_text"
    const val METRICS_TABLE = "metrics_dialog_metrics_table"
}
```

**Usage в тестах**:
```kotlin
onNodeWithTag(MetricsDialogTags.ROOT).assertExists()
onNodeWithTag(MetricsDialogTags.METRICS_TABLE).assertExists()
```

## UI Components Structure

```
MetricsDialog
└── Dialog
    └── DialogSurface
        └── Column
            ├── DialogHeader (title + close button)
            ├── SectionSpacer
            └── [if metrics.isEmpty()]
                └── EmptyStateBox
            └── [else]
                └── Column (scrollable)
                    └── For each prompt group:
                        ├── Text (prompt preview)
                        └── MetricsTable
                            └── Row (headers)
                            └── Divider
                            └── MetricRow (Length, Tokens, Max Tokens, Finish Reason, Time)
```

## Константы

Все константы вынесены в начало файла:
```kotlin
private const val PROMPT_PREVIEW_LENGTH = 50
private const val DIALOG_HEIGHT_FRACTION = 0.8f
private const val SPACING_SMALL = 16
private const val SPACING_MEDIUM = 20
private const val SPACING_LARGE = 24
private const val COLUMN_WIDTH_NARROW = 120
private const val COLUMN_WIDTH_WIDE = 140
private const val PADDING_SMALL = 4
private const val PADDING_MEDIUM = 8
```

## Тестирование

| Тип | Файлы | Назначение |
|-----|-------|------------|
| Unit | `composeApp/.../MetricsDialogTest.kt` | Logic tests (группировка, empty state) |
| UI | `composeApp/.../MetricsDialogUiRealTest.kt` | Compose interactions |

**Тест-кейсы**:
- Empty state отображение
- Группировка по prompt (single/multiple)
- Truncation длинных prompts
- Таблица метрик с разными constraints
- Dismiss callback

## Критичные правила

- **Stateless**: Диалог не хранит состояние, только отображает переданные данные
- **No business logic**: Вся логика в `feature:chat` (ChatState, ChatViewModel)
- **Detekt**: БЕЗ `@Suppress`
- **KDoc**: документация приветствуется
- **No comments**: Код self-documenting
- **Constants**: Все магические числа вынесены в константы

## Integration Example

### В feature:chat (App.kt)
```kotlin
import metrics.ui.MetricsDialog

@Composable
fun App(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsState()
    
    // ... other UI code ...
    
    if (state.showMetrics) {
        MetricsDialog(
            metrics = state.metrics,
            onDismiss = { viewModel.processIntent(ToggleMetrics(false)) }
        )
    }
}
```

### ChatState integration
```kotlin
data class ChatState(
    // ... other fields ...
    val metrics: List<MetricRecord> = emptyList(),
    val showMetrics: Boolean = false,
)
```

## Workflow при изменениях

1. Изменил UI → `./gradlew :feature:metrics:check`
2. Style issues → `./gradlew :feature:metrics:ktlintFormat`
3. Проверил integration → `./gradlew :feature:chat:check`
4. Commit

## Частые задачи

### Добавить новую метрику в таблицу
1. Добавить строку в `MetricsTable`:
```kotlin
MetricRow("New Metric", records.map { it.newField.toString() })
```
2. Обновить `MetricRecord` в `core:model` (если нужно новое поле)
3. Добавить тест-кейс в `MetricsDialogTest.kt`

### Изменить форматирование constraints
1. Изменить `ConstraintsInfo.toDisplayString()` в `core:model`
2. Таблица автоматически обновится

### Изменить layout диалога
1. Изменить константы (DIALOG_HEIGHT_FRACTION, COLUMN_WIDTH_*, etc.)
2. Или изменить структуру в `MetricsDialog` composable

## Особенности модуля

- **Простота**: UI-only, без сложной логики
- **Reusable**: Может использоваться в других features
- **Grouped display**: Метрики группируются по prompt для сравнения
- **Responsive**: Scrollable контент для больших данных
- **Testability**: Полный набор test tags для UI тестов
