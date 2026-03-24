package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import model.ReasoningComparison
import model.ReasoningMode
import ui.theme.AppColors

object ReasoningDialogTags {
    const val ROOT = "reasoning_dialog_root"
    const val DIALOG_SURFACE = "reasoning_dialog_surface"
    const val TITLE = "reasoning_dialog_title"
    const val CLOSE_BUTTON = "reasoning_dialog_close_button"
    const val TASK_INPUT_FIELD = "reasoning_dialog_task_input"
    const val RUN_BUTTON = "reasoning_dialog_run_button"
    const val TAB_ROW = "reasoning_dialog_tab_row"
    const val CONTENT_AREA = "reasoning_dialog_content_area"
    const val LOADING_INDICATOR = "reasoning_dialog_loading"
    const val ERROR_TEXT = "reasoning_dialog_error"
    const val EMPTY_STATE = "reasoning_dialog_empty_state"
    const val COMPARISON_TABLE = "reasoning_dialog_comparison_table"
}

@Composable
fun ReasoningDialog(
    comparison: ReasoningComparison,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRunComparison: (task: String) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(0) }
    var taskInput by remember { mutableStateOf(comparison.task) }
    val listState = rememberLazyListState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 24.dp)
                .testTag(ReasoningDialogTags.DIALOG_SURFACE),
            shape = RoundedCornerShape(16.dp),
            color = AppColors.Background,
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .testTag(ReasoningDialogTags.ROOT),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Сравнение способов рассуждения",
                        style = MaterialTheme.typography.h6,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag(ReasoningDialogTags.TITLE),
                    )
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag(ReasoningDialogTags.CLOSE_BUTTON),
                    ) {
                        Text("Закрыть", color = AppColors.Primary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = taskInput,
                    onValueChange = { taskInput = it },
                    label = { Text("Задача") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ReasoningDialogTags.TASK_INPUT_FIELD),
                    textStyle = MaterialTheme.typography.body2,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onRunComparison(taskInput) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ReasoningDialogTags.RUN_BUTTON),
                    enabled = !isLoading && taskInput.isNotBlank(),
                    colors = primaryButtonColors(),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(20.dp).height(20.dp),
                            color = AppColors.TextPrimary,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Выполняю...", color = AppColors.TextPrimary)
                    } else {
                        Text("Запустить сравнение", color = AppColors.TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (comparison.hasAnyResult) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        backgroundColor = AppColors.Surface,
                        contentColor = AppColors.Primary,
                        indicator = { },
                        divider = { },
                        modifier = Modifier.testTag(ReasoningDialogTags.TAB_ROW),
                    ) {
                        ReasoningMode.entries.forEachIndexed { index, mode ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    val result = comparison.results[mode]
                                    val statusText = when {
                                        result?.isLoading == true -> " [..]"
                                        result?.error != null -> " [X]"
                                        result?.response?.isNotEmpty() == true -> " [OK]"
                                        else -> ""
                                    }
                                    Text(
                                        mode.displayName + statusText,
                                        color = if (selectedTab == index) {
                                            AppColors.Primary
                                        } else {
                                            AppColors.TextSecondary
                                        },
                                    )
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val currentMode = ReasoningMode.entries.getOrNull(selectedTab) ?: ReasoningMode.DIRECT
                    val currentResult = comparison.results[currentMode]

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(AppColors.Surface, RoundedCornerShape(8.dp))
                            .testTag(ReasoningDialogTags.CONTENT_AREA),
                    ) {
                        when {
                            currentResult?.isLoading == true -> {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .testTag(ReasoningDialogTags.LOADING_INDICATOR),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    CircularProgressIndicator(color = AppColors.Primary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Запрашиваю...", color = AppColors.TextSecondary)
                                }
                            }

                            currentResult?.error != null -> {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .testTag(ReasoningDialogTags.ERROR_TEXT),
                                ) {
                                    Text("Ошибка", color = AppColors.Error, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(currentResult.error, color = AppColors.TextSecondary)
                                }
                            }

                            currentResult != null && currentResult.response.isNotEmpty() -> {
                                LazyColumn(
                                    modifier = Modifier.padding(12.dp),
                                    state = listState,
                                ) {
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            backgroundColor = AppColors.SurfaceLight,
                                            elevation = 0.dp,
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    "Системный промпт:",
                                                    style = MaterialTheme.typography.caption,
                                                    color = AppColors.TextMuted,
                                                )
                                                Text(
                                                    currentResult.systemPrompt.ifEmpty { "(нет)" },
                                                    style = MaterialTheme.typography.body2,
                                                    color = AppColors.TextSecondary,
                                                )
                                            }
                                        }
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "Ответ модели:",
                                            style = MaterialTheme.typography.caption,
                                            color = AppColors.TextMuted,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Markdown(
                                            content = currentResult.response,
                                            colors = markdownColor(text = AppColors.TextPrimary),
                                        )
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        ) {
                                            MetricBadge("Время", "${currentResult.responseTimeMs}ms")
                                            currentResult.tokensUsed?.let {
                                                MetricBadge("Токены", "$it")
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                Text(
                                    "Нажмите 'Запустить сравнение' для получения результатов",
                                    modifier = Modifier.align(Alignment.Center),
                                    color = AppColors.TextMuted,
                                )
                            }
                        }
                    }

                    if (comparison.isComplete) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ComparisonTable(
                            comparison = comparison,
                            modifier = Modifier.testTag(ReasoningDialogTags.COMPARISON_TABLE),
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag(ReasoningDialogTags.EMPTY_STATE),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Введите задачу и нажмите 'Запустить сравнение'",
                            color = AppColors.TextMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBadge(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = AppColors.SurfaceLight,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$label: ",
                style = MaterialTheme.typography.caption,
                color = AppColors.TextMuted,
            )
            Text(
                value,
                style = MaterialTheme.typography.caption,
                color = AppColors.Primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ComparisonTable(comparison: ReasoningComparison, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = AppColors.Surface,
        elevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Сводная таблица",
                style = MaterialTheme.typography.subtitle2,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Режим",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.caption,
                    color = AppColors.TextMuted
                )
                Text(
                    "Время",
                    modifier = Modifier.width(60.dp),
                    style = MaterialTheme.typography.caption,
                    color = AppColors.TextMuted
                )
                Text(
                    "Токены",
                    modifier = Modifier.width(60.dp),
                    style = MaterialTheme.typography.caption,
                    color = AppColors.TextMuted
                )
                Text(
                    "Длина",
                    modifier = Modifier.width(60.dp),
                    style = MaterialTheme.typography.caption,
                    color = AppColors.TextMuted
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            ReasoningMode.entries.forEach { mode ->
                val result = comparison.results[mode]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        mode.displayName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.body2,
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        "${result?.responseTimeMs ?: "-"}ms",
                        modifier = Modifier.width(60.dp),
                        style = MaterialTheme.typography.body2,
                        color = AppColors.TextSecondary,
                    )
                    Text(
                        result?.tokensUsed?.toString() ?: "-",
                        modifier = Modifier.width(60.dp),
                        style = MaterialTheme.typography.body2,
                        color = AppColors.TextSecondary,
                    )
                    Text(
                        "${result?.response?.length ?: 0}",
                        modifier = Modifier.width(60.dp),
                        style = MaterialTheme.typography.body2,
                        color = AppColors.TextSecondary,
                    )
                }
            }
        }
    }
}
