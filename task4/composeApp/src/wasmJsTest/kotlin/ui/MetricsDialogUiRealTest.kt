@file:OptIn(ExperimentalTestApi::class)

package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import model.ConstraintsInfo
import model.MetricRecord
import ui.components.MetricsDialog
import ui.components.MetricsDialogTags
import kotlin.test.Test
import kotlin.test.assertTrue

@ExperimentalWasmJsInterop
class MetricsDialogUiRealTest {

    private val sampleMetric = MetricRecord(
        id = 1,
        prompt = "Test prompt",
        response = "Test response",
        mode = "default",
        responseLength = 100,
        tokensUsed = 50,
        maxTokens = 100,
        finishReason = "stop",
        responseTimeMs = 500,
        constraints = ConstraintsInfo(
            maxTokens = 100,
            stopSequences = emptyList(),
            responseFormat = "text",
            temperature = 1.0,
        ),
    )

    @Test
    fun metricsDialogDisplaysMetrics() = runComposeUiTest {
        val metrics = listOf(sampleMetric)

        setContent {
            MetricsDialog(
                metrics = metrics,
                onDismiss = {},
            )
        }

        onNodeWithTag(MetricsDialogTags.ROOT).assertExists()
        onNodeWithTag(MetricsDialogTags.CONTENT_COLUMN).assertExists()
        onNodeWithText("Metrics Comparison").assertExists()
    }

    @Test
    fun metricsDialogDisplaysMetricsList() = runComposeUiTest {
        val metrics = listOf(sampleMetric)

        setContent {
            MetricsDialog(
                metrics = metrics,
                onDismiss = {},
            )
        }

        onNodeWithTag(MetricsDialogTags.METRICS_LIST).assertExists()
        onNodeWithTag(MetricsDialogTags.METRICS_TABLE).assertExists()
        onNodeWithTag(MetricsDialogTags.PROMPT_TEXT).assertExists()
    }

    @Test
    fun metricsDialogCloseButtonTriggersDismiss() = runComposeUiTest {
        var dismissed = false

        setContent {
            MetricsDialog(
                metrics = listOf(sampleMetric),
                onDismiss = { dismissed = true },
            )
        }

        onNodeWithText("Close").performClick()

        assertTrue(dismissed)
    }

    @Test
    fun metricsDialogEmptyStateDisplaysMessage() = runComposeUiTest {
        setContent {
            MetricsDialog(
                metrics = emptyList(),
                onDismiss = {},
            )
        }

        onNodeWithTag(MetricsDialogTags.EMPTY_STATE).assertExists()
        onNodeWithText("No metrics yet.", substring = true).assertExists()
    }

    @Test
    fun metricsDialogDisplaysPromptText() = runComposeUiTest {
        val metrics = listOf(sampleMetric)

        setContent {
            MetricsDialog(
                metrics = metrics,
                onDismiss = {},
            )
        }

        onNodeWithText("Prompt: \"Test prompt\"").assertExists()
    }

    @Test
    fun metricsDialogDisplaysTableHeaders() = runComposeUiTest {
        val metrics = listOf(sampleMetric)

        setContent {
            MetricsDialog(
                metrics = metrics,
                onDismiss = {},
            )
        }

        onNodeWithText("Metric").assertExists()
        onNodeWithText("Length (chars)").assertExists()
        onNodeWithText("Tokens").assertExists()
        onNodeWithText("Max Tokens").assertExists()
        onNodeWithText("Finish Reason").assertExists()
        onNodeWithText("Time (ms)").assertExists()
    }

    @Test
    fun metricsDialogDisplaysMetricValues() = runComposeUiTest {
        val metrics = listOf(sampleMetric)

        setContent {
            MetricsDialog(
                metrics = metrics,
                onDismiss = {},
            )
        }

        onAllNodesWithText("100").assertCountEquals(EXPECTED_NODE_COUNT)
        onNodeWithText("50").assertExists()
        onNodeWithText("stop").assertExists()
        onNodeWithText("500").assertExists()
    }

    @Test
    fun metricsDialogGroupsByPrompt() = runComposeUiTest {
        val metrics = listOf(
            sampleMetric,
            sampleMetric.copy(
                id = SECOND_METRIC_ID,
                constraints = ConstraintsInfo(null, emptyList(), "text", LOW_TEMPERATURE),
            ),
        )

        setContent {
            MetricsDialog(
                metrics = metrics,
                onDismiss = {},
            )
        }

        onNodeWithTag(MetricsDialogTags.PROMPT_TEXT).assertExists()
    }

    @Test
    fun metricsDialogTruncatesLongPrompt() = runComposeUiTest {
        val longPrompt = "A".repeat(LONG_PROMPT_LENGTH)
        val metrics = listOf(sampleMetric.copy(prompt = longPrompt))

        setContent {
            MetricsDialog(
                metrics = metrics,
                onDismiss = {},
            )
        }

        onNodeWithText("Prompt: \"${"A".repeat(TRUNCATED_PROMPT_LENGTH)}...\"").assertExists()
    }

    private companion object {
        const val EXPECTED_NODE_COUNT = 2
        const val SECOND_METRIC_ID = 2
        const val LOW_TEMPERATURE = 0.5
        const val LONG_PROMPT_LENGTH = 100
        const val TRUNCATED_PROMPT_LENGTH = 50
    }
}
