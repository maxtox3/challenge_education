package ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import model.ReasoningComparison
import model.ReasoningMode
import model.ReasoningResult
import ui.theme.AppColors

/**
 * Returns status indicator text for a reasoning result.
 */
internal fun getStatusText(result: ReasoningResult?): String = when {
    result?.isLoading == true -> " [..]"
    result?.error != null -> " [X]"
    result?.response?.isNotEmpty() == true -> " [OK]"
    else -> ""
}

/**
 * Displays a metric badge with label and value.
 */
@Composable
internal fun MetricBadge(label: String, value: String) {
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

/**
 * Displays a card with the system prompt.
 */
@Composable
internal fun SystemPromptCard(systemPrompt: String) {
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
                systemPrompt.ifEmpty { "(нет)" },
                style = MaterialTheme.typography.body2,
                color = AppColors.TextSecondary,
            )
        }
    }
}

/**
 * Displays a comparison table with metrics for all reasoning modes.
 */
@Composable
internal fun ComparisonTable(comparison: ReasoningComparison, modifier: Modifier = Modifier) {
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
            ComparisonTableHeader()
            Spacer(modifier = Modifier.height(4.dp))
            ComparisonTableRows(comparison)
        }
    }
}

@Composable
private fun ComparisonTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "Режим",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.caption,
            color = AppColors.TextMuted,
        )
        Text(
            "Время",
            modifier = Modifier.width(60.dp),
            style = MaterialTheme.typography.caption,
            color = AppColors.TextMuted,
        )
        Text(
            "Токены",
            modifier = Modifier.width(60.dp),
            style = MaterialTheme.typography.caption,
            color = AppColors.TextMuted,
        )
        Text(
            "Длина",
            modifier = Modifier.width(60.dp),
            style = MaterialTheme.typography.caption,
            color = AppColors.TextMuted,
        )
    }
}

@Composable
private fun ComparisonTableRows(comparison: ReasoningComparison) {
    ReasoningMode.entries.forEach { mode ->
        val result = comparison.results[mode]
        ComparisonTableRow(mode, result)
    }
}

@Composable
private fun ComparisonTableRow(mode: ReasoningMode, result: ReasoningResult?) {
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
