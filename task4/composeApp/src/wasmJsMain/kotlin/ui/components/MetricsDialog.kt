package ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import model.MetricRecord
import ui.theme.AppColors

object MetricsDialogTags {
    const val ROOT = "metrics_dialog_root"
    const val CONTENT_COLUMN = "metrics_dialog_content"
    const val METRICS_LIST = "metrics_dialog_list"
    const val EMPTY_STATE = "metrics_dialog_empty_state"
    const val METRICS_TABLE = "metrics_table"
    const val PROMPT_TEXT = "metrics_prompt_text"
}

@Composable
fun MetricsDialog(metrics: List<MetricRecord>, onDismiss: () -> Unit,) {
    val groupedByPrompt = metrics.groupBy { it.prompt }

    Dialog(onDismissRequest = onDismiss) {
        DialogSurface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp)
                .testTag(MetricsDialogTags.ROOT),
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .testTag(MetricsDialogTags.CONTENT_COLUMN),
            ) {
                DialogHeader(
                    title = "Metrics Comparison",
                    onDismiss = onDismiss,
                )

                SectionSpacer(16)

                if (metrics.isEmpty()) {
                    EmptyStateBox(
                        message = "No metrics yet.\nSend some messages to see comparison.",
                        modifier = Modifier.testTag(MetricsDialogTags.EMPTY_STATE),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                            .testTag(MetricsDialogTags.METRICS_LIST),
                    ) {
                        groupedByPrompt.forEach { (prompt, records) ->
                            Text(
                                text = "Prompt: \"${prompt.take(50)}${if (prompt.length > 50) "..." else ""}\"",
                                style = MaterialTheme.typography.subtitle2,
                                color = AppColors.TextSecondary,
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .testTag(MetricsDialogTags.PROMPT_TEXT),
                            )

                            MetricsTable(records)

                            SectionSpacer(24)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricsTable(records: List<MetricRecord>) {
    val headers = listOf("Metric") + records.map { it.constraints.toDisplayString() }

    Column(modifier = Modifier.testTag(MetricsDialogTags.METRICS_TABLE)) {
        Row {
            headers.forEachIndexed { index, header ->
                Box(
                    modifier = Modifier
                        .width(if (index == 0) 120.dp else 140.dp)
                        .padding(4.dp),
                ) {
                    Text(
                        text = header,
                        style = MaterialTheme.typography.caption,
                        color = AppColors.Primary,
                    )
                }
            }
        }

        Divider(color = AppColors.Border, thickness = 1.dp)

        MetricRow("Length (chars)", records.map { it.responseLength.toString() })
        MetricRow("Tokens", records.map { it.tokensUsed?.toString() ?: "-" })
        MetricRow("Max Tokens", records.map { it.maxTokens?.toString() ?: "unlimited" })
        MetricRow("Finish Reason", records.map { it.finishReason ?: "-" })
        MetricRow("Time (ms)", records.map { it.responseTimeMs.toString() })
    }
}

@Composable
private fun MetricRow(label: String, values: List<String>) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .padding(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.body2,
                color = AppColors.TextSecondary,
            )
        }

        values.forEach { value ->
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .padding(4.dp),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.body2,
                    color = AppColors.TextPrimary,
                )
            }
        }
    }
}
