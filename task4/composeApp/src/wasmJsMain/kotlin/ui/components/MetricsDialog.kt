package ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import model.MetricRecord
import ui.theme.AppColors
@Composable
fun MetricsDialog(metrics: List<MetricRecord>, onDismiss: () -> Unit,) {
    val groupedByPrompt = metrics.groupBy { it.prompt }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = AppColors.Surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Metrics Comparison",
                        style = MaterialTheme.typography.h6,
                        color = AppColors.TextPrimary,
                    )

                    TextButton(onClick = onDismiss) {
                        Text("Close", color = AppColors.TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (metrics.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        Text(
                            text = "No metrics yet.\nSend some messages to see comparison.",
                            color = AppColors.TextMuted,
                            style = MaterialTheme.typography.body1,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        groupedByPrompt.forEach { (prompt, records) ->
                            Text(
                                text = "Prompt: \"${prompt.take(50)}${if (prompt.length > 50) "..." else ""}\"",
                                style = MaterialTheme.typography.subtitle2,
                                color = AppColors.TextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )

                            MetricsTable(records)

                            Spacer(modifier = Modifier.height(24.dp))
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

    Column {
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
