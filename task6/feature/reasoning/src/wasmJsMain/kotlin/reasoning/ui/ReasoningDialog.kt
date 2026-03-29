package reasoning.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
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
import model.ReasoningResult
import ui.components.primaryButtonColors
import ui.theme.AppColors

private const val DIALOG_HEIGHT_FRACTION = 0.9f

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
                .fillMaxHeight(DIALOG_HEIGHT_FRACTION)
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
                DialogHeader(onDismiss = onDismiss)
                Spacer(modifier = Modifier.height(12.dp))
                TaskInputSection(
                    taskInput = taskInput,
                    isLoading = isLoading,
                    onTaskInputChange = { taskInput = it },
                    onRunComparison = { onRunComparison(taskInput) },
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (comparison.hasAnyResult) {
                    TabContentSection(
                        comparison = comparison,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        listState = listState,
                    )
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
private fun DialogHeader(onDismiss: () -> Unit) {
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
}

@Composable
private fun TaskInputSection(
    taskInput: String,
    isLoading: Boolean,
    onTaskInputChange: (String) -> Unit,
    onRunComparison: () -> Unit,
) {
    OutlinedTextField(
        value = taskInput,
        onValueChange = onTaskInputChange,
        label = { Text("Задача") },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ReasoningDialogTags.TASK_INPUT_FIELD),
        textStyle = MaterialTheme.typography.body2,
    )

    Spacer(modifier = Modifier.height(12.dp))

    Button(
        onClick = onRunComparison,
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
}

@Composable
private fun ColumnScope.TabContentSection(
    comparison: ReasoningComparison,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    listState: LazyListState,
) {
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
                onClick = { onTabSelected(index) },
                text = {
                    val result = comparison.results[mode]
                    val statusText = getStatusText(result)
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

    ResultContentArea(
        currentResult = currentResult,
        listState = listState,
    )

    if (comparison.isComplete) {
        Spacer(modifier = Modifier.height(12.dp))
        ComparisonTable(
            comparison = comparison,
            modifier = Modifier.testTag(ReasoningDialogTags.COMPARISON_TABLE),
        )
    }
}

@Composable
private fun ColumnScope.ResultContentArea(currentResult: ReasoningResult?, listState: LazyListState) {
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
                val error = currentResult.error
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag(ReasoningDialogTags.ERROR_TEXT),
                ) {
                    Text("Ошибка", color = AppColors.Error, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (error != null) {
                        Text(error, color = AppColors.TextSecondary)
                    }
                }
            }

            currentResult != null && currentResult.response.isNotEmpty() -> {
                ResultItemContent(currentResult, listState)
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
}

@Composable
private fun ResultItemContent(result: ReasoningResult, listState: LazyListState) {
    LazyColumn(
        modifier = Modifier.padding(12.dp),
        state = listState,
    ) {
        item {
            SystemPromptCard(result.systemPrompt)
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
                content = result.response,
                colors = markdownColor(text = AppColors.TextPrimary),
            )
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MetricBadge("Время", "${result.responseTimeMs}ms")
                result.tokensUsed?.let {
                    MetricBadge("Токены", "$it")
                }
            }
        }
    }
}
