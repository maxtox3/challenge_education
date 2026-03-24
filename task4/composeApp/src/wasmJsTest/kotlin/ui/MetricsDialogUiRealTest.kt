@file:OptIn(ExperimentalTestApi::class)

package ui

import androidx.compose.ui.test.*
import model.ConstraintsInfo
import model.MetricRecord
import ui.components.MetricsDialog
import ui.components.MetricsDialogTags
import kotlin.test.Test
import kotlin.test.assertTrue

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
    fun metricsDialog_displaysMetrics() = runComposeUiTest {
        val metrics = listOf(sampleMetric)
        var dismissed = false

        setContent {
            MetricsDialog(
                metrics = metrics,
                onDismiss = { dismissed = true },
            )
        }

        onNodeWithTag(MetricsDialogTags.ROOT).assertExists()
        onNodeWithTag(MetricsDialogTags.CONTENT_COLUMN).assertExists()
        onNodeWithText("Metrics Comparison").assertExists()
    }

    @Test
    fun metricsDialog_displaysMetricsList() = runComposeUiTest {
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
    fun metricsDialog_closeButton_triggersDismiss() = runComposeUiTest {
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
    fun metricsDialog_emptyState_displaysMessage() = runComposeUiTest {
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
    fun metricsDialog_displaysPromptText() = runComposeUiTest {
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
    fun metricsDialog_displaysTableHeaders() = runComposeUiTest {
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
    fun metricsDialog_displaysMetricValues() = runComposeUiTest {
        val metrics = listOf(sampleMetric)

        setContent {
            MetricsDialog(
                metrics = metrics,
                onDismiss = {},
            )
        }

        onAllNodesWithText("100").assertCountEquals(2)
        onNodeWithText("50").assertExists()
        onNodeWithText("stop").assertExists()
        onNodeWithText("500").assertExists()
    }

    @Test
    fun metricsDialog_groupsByPrompt() = runComposeUiTest {
        val metrics = listOf(
            sampleMetric,
            sampleMetric.copy(id = 2, constraints = ConstraintsInfo(null, emptyList(), "text", 0.5)),
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
    fun metricsDialog_truncatesLongPrompt() = runComposeUiTest {
        val longPrompt = "A".repeat(100)
        val metrics = listOf(sampleMetric.copy(prompt = longPrompt))

        setContent {
            MetricsDialog(
                metrics = metrics,
                onDismiss = {},
            )
        }

        onNodeWithText("Prompt: \"${"A".repeat(50)}...\"").assertExists()
    }
}
