package ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import model.MetricRecord
import ui.theme.AppColors

private const val PROMPT_PREVIEW_LENGTH = 50
private const val DIALOG_HEIGHT_FRACTION = 0.8f
private const val SPACING_SMALL = 16
private const val SPACING_MEDIUM = 20
private const val SPACING_LARGE = 24
private const val COLUMN_WIDTH_NARROW = 120
private const val COLUMN_WIDTH_WIDE = 140
private const val PADDING_SMALL = 4
private const val PADDING_MEDIUM = 8

@Composable
fun MetricsDialog(metrics: List<MetricRecord>, onDismiss: () -> Unit) {
    val groupedByPrompt = metrics.groupBy { it.prompt }

    Dialog(onDismissRequest = onDismiss) {
        DialogSurface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(DIALOG_HEIGHT_FRACTION)
                .padding(SPACING_SMALL.dp)
                .testTag(MetricsDialogTags.ROOT),
        ) {
            Column(
                modifier = Modifier
                    .padding(SPACING_MEDIUM.dp)
                    .testTag(MetricsDialogTags.CONTENT_COLUMN),
            ) {
                DialogHeader(
                    title = "Metrics Comparison",
                    onDismiss = onDismiss,
                )

                SectionSpacer(SPACING_SMALL)

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
                            val promptPreview = prompt.take(PROMPT_PREVIEW_LENGTH)
                            val ellipsis = if (prompt.length > PROMPT_PREVIEW_LENGTH) "..." else ""
                            Text(
                                text = "Prompt: \"$promptPreview$ellipsis\"",
                                style = MaterialTheme.typography.subtitle2,
                                color = AppColors.TextSecondary,
                                modifier = Modifier
                                    .padding(bottom = PADDING_MEDIUM.dp)
                                    .testTag(MetricsDialogTags.PROMPT_TEXT),
                            )

                            MetricsTable(records)

                            SectionSpacer(SPACING_LARGE)
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
                        .width(if (index == 0) COLUMN_WIDTH_NARROW.dp else COLUMN_WIDTH_WIDE.dp)
                        .padding(PADDING_SMALL.dp),
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
        modifier = Modifier.padding(vertical = PADDING_SMALL.dp),
    ) {
        Box(
            modifier = Modifier
                .width(COLUMN_WIDTH_NARROW.dp)
                .padding(PADDING_SMALL.dp),
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
                    .width(COLUMN_WIDTH_WIDE.dp)
                    .padding(PADDING_SMALL.dp),
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
