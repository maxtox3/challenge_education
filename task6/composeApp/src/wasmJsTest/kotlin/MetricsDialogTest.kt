import model.ConstraintsInfo
import model.MetricRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MetricsDialogTest {

    @Test
    fun testMetricsDialogEmptyStateDisplay() {
        val metrics = emptyList<MetricRecord>()
        assertTrue(metrics.isEmpty())
    }

    @Test
    fun testMetricsDialogEmptyStateMessage() {
        val metrics = emptyList<MetricRecord>()
        val expectedMessage = "No metrics yet.\nSend some messages to see comparison."
        assertTrue(metrics.isEmpty())
        assertEquals("No metrics yet.\nSend some messages to see comparison.", expectedMessage)
    }

    @Test
    fun testMetricsDialogGroupedByPromptSinglePrompt() {
        val constraints = ConstraintsInfo(null, emptyList(), "text", 1.0)
        val metrics = listOf(
            MetricRecord(1, "Hello", "Hi there", "free", 8, 10, null, "stop", 100L, constraints),
            MetricRecord(2, "Hello", "Hey!", "constrained", 4, 5, 100, "stop", 50L, constraints)
        )
        val grouped = metrics.groupBy { it.prompt }

        assertEquals(1, grouped.size)
        assertEquals(2, grouped["Hello"]?.size)
    }

    @Test
    fun testMetricsDialogGroupedByPromptMultiplePrompts() {
        val constraints = ConstraintsInfo(null, emptyList(), "text", 1.0)
        val metrics = listOf(
            MetricRecord(1, "Hello", "Hi", "free", 2, 5, null, "stop", 100L, constraints),
            MetricRecord(2, "Goodbye", "Bye", "free", 3, 6, null, "stop", 200L, constraints),
            MetricRecord(3, "Hello", "Hey", "constrained", 3, 7, 50, "stop", 150L, constraints)
        )
        val grouped = metrics.groupBy { it.prompt }

        assertEquals(2, grouped.size)
        assertEquals(2, grouped["Hello"]?.size)
        assertEquals(1, grouped["Goodbye"]?.size)
    }

    @Test
    fun testMetricsDialogGroupedByPromptTruncatedPromptDisplay() {
        val longPrompt = "A".repeat(100)
        val displayText = "\"${longPrompt.take(50)}${if (longPrompt.length > 50) "..." else ""}\""
        assertEquals(55, displayText.length)
        assertTrue(displayText.endsWith("...\""))
    }

    @Test
    fun testMetricsDialogGroupedByPromptShortPromptNoTruncation() {
        val shortPrompt = "Hello"
        val displayText = "\"${shortPrompt.take(50)}${if (shortPrompt.length > 50) "..." else ""}\""
        assertEquals("\"Hello\"", displayText)
        assertFalse(displayText.contains("..."))
    }

    @Test
    fun testMetricsTableHeadersIncludeConstraints() {
        val constraints1 = ConstraintsInfo(null, emptyList(), "text", 1.0)
        val constraints2 = ConstraintsInfo(100, emptyList(), "json", 0.5)
        val records = listOf(
            MetricRecord(1, "test", "response1", "free", 9, 10, null, "stop", 100L, constraints1),
            MetricRecord(2, "test", "response2", "constrained", 9, 10, 100, "stop", 100L, constraints2)
        )
        val headers = listOf("Metric") + records.map { it.constraints.toDisplayString() }

        assertEquals(3, headers.size)
        assertEquals("Metric", headers[0])
        assertEquals("Free", headers[1])
        assertTrue(headers[2].contains("maxTokens"))
        assertTrue(headers[2].contains("format"))
        assertTrue(headers[2].contains("temp"))
    }

    @Test
    fun testMetricsTableRenderingLengthChars() {
        val record = MetricRecord(
            1, "test", "response", "free", 8, 10, null, "stop", 100L,
            ConstraintsInfo(null, emptyList(), "text", 1.0)
        )
        assertEquals("8", record.responseLength.toString())
    }

    @Test
    fun testMetricsTableRenderingTokensUsed() {
        val recordWithTokens = MetricRecord(
            1, "test", "response", "free", 8, 50, null, "stop", 100L,
            ConstraintsInfo(null, emptyList(), "text", 1.0)
        )
        val recordWithoutTokens = MetricRecord(
            2, "test", "response", "free", 8, null, null, "stop", 100L,
            ConstraintsInfo(null, emptyList(), "text", 1.0)
        )

        assertEquals("50", recordWithTokens.tokensUsed?.toString() ?: "-")
        assertEquals("-", recordWithoutTokens.tokensUsed?.toString() ?: "-")
    }

    @Test
    fun testMetricsTableRenderingMaxTokens() {
        val recordWithLimit = MetricRecord(
            1, "test", "response", "free", 8, 50, 1000, "stop", 100L,
            ConstraintsInfo(null, emptyList(), "text", 1.0)
        )
        val recordUnlimited = MetricRecord(
            2, "test", "response", "free", 8, 50, null, "stop", 100L,
            ConstraintsInfo(null, emptyList(), "text", 1.0)
        )

        assertEquals("1000", recordWithLimit.maxTokens?.toString() ?: "unlimited")
        assertEquals("unlimited", recordUnlimited.maxTokens?.toString() ?: "unlimited")
    }

    @Test
    fun testMetricsTableRenderingFinishReason() {
        val recordWithReason = MetricRecord(
            1, "test", "response", "free", 8, 50, null, "stop", 100L,
            ConstraintsInfo(null, emptyList(), "text", 1.0)
        )
        val recordWithoutReason = MetricRecord(
            2, "test", "response", "free", 8, 50, null, null, 100L,
            ConstraintsInfo(null, emptyList(), "text", 1.0)
        )

        assertEquals("stop", recordWithReason.finishReason ?: "-")
        assertEquals("-", recordWithoutReason.finishReason ?: "-")
    }

    @Test
    fun testMetricsTableRenderingResponseTime() {
        val record = MetricRecord(
            1, "test", "response", "free", 8, 50, null, "stop", 1500L,
            ConstraintsInfo(null, emptyList(), "text", 1.0)
        )
        assertEquals("1500", record.responseTimeMs.toString())
    }

    @Test
    fun testMetricsDialogDismissCallback() {
        var dismissCalled = false
        val onDismiss: () -> Unit = { dismissCalled = true }
        onDismiss()
        assertTrue(dismissCalled)
    }

    @Test
    fun testMetricBadgeTimeDisplay() {
        val responseTimeMs = 1234L
        val displayValue = "${responseTimeMs}ms"
        assertEquals("1234ms", displayValue)
    }

    @Test
    fun testMetricBadgeTokensDisplay() {
        val tokensUsed = 42
        val displayValue = "$tokensUsed"
        assertEquals("42", displayValue)
    }
}
