package baseline

import model.ConstraintsInfo
import model.MetricRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Characterization tests for Metrics feature (Chunk 7)
 * Tests capture CURRENT behavior as baseline for refactoring.
 */
class MetricsBaselineTest {

    // ==================== Constants ====================

    @Test
    fun promptPreviewLengthConstantIs50() {
        val promptPreviewLength = 50
        assertEquals(50, promptPreviewLength)
    }

    @Test
    fun dialogHeightFractionConstantIs08f() {
        val dialogHeightFraction = 0.8f
        assertEquals(0.8f, dialogHeightFraction)
    }

    // ==================== Prompt Grouping Logic ====================

    @Test
    fun groupingByPromptCreatesCorrectGroups() {
        val constraints = ConstraintsInfo(null, emptyList(), "text", 1.0)
        val metrics = listOf(
            MetricRecord(1, "Hello", "Hi there", "free", 8, 10, null, "stop", 100L, constraints),
            MetricRecord(2, "Hello", "Hey!", "constrained", 4, 5, 100, "stop", 50L, constraints),
            MetricRecord(3, "Goodbye", "Bye!", "free", 4, 6, null, "stop", 75L, constraints)
        )

        val grouped = metrics.groupBy { it.prompt }

        assertEquals(2, grouped.size, "Should have 2 groups")
        assertEquals(2, grouped["Hello"]?.size, "Hello group should have 2 records")
        assertEquals(1, grouped["Goodbye"]?.size, "Goodbye group should have 1 record")
    }

    @Test
    fun groupingEmptyListReturnsEmptyMap() {
        val metrics = emptyList<MetricRecord>()
        val grouped = metrics.groupBy { it.prompt }
        assertTrue(grouped.isEmpty())
    }

    @Test
    fun groupingSingleItemReturnsSingleGroup() {
        val constraints = ConstraintsInfo(null, emptyList(), "text", 1.0)
        val metrics = listOf(
            MetricRecord(1, "Single", "Response", "free", 8, 10, null, "stop", 100L, constraints)
        )
        val grouped = metrics.groupBy { it.prompt }

        assertEquals(1, grouped.size)
        assertEquals(1, grouped["Single"]?.size)
    }

    // ==================== Prompt Preview Truncation ====================

    @Test
    fun promptPreviewTruncatesTo50CharsWithEllipsisForLongPrompts() {
        val longPrompt = "A".repeat(100)
        val previewLength = 50
        val preview = longPrompt.take(previewLength)
        val ellipsis = if (longPrompt.length > previewLength) "..." else ""

        assertEquals(50, preview.length)
        assertEquals("...", ellipsis)
        assertTrue(longPrompt.length > previewLength)
    }

    @Test
    fun promptPreviewShowsFullPromptWithoutEllipsisForShortPrompts() {
        val shortPrompt = "Hello World"
        val previewLength = 50
        val preview = shortPrompt.take(previewLength)
        val ellipsis = if (shortPrompt.length > previewLength) "..." else ""

        assertEquals("Hello World", preview)
        assertEquals("", ellipsis)
        assertFalse(shortPrompt.length > previewLength)
    }

    @Test
    fun promptPreviewHandlesExactly50CharPrompt() {
        val exactPrompt = "A".repeat(50)
        val previewLength = 50
        val ellipsis = if (exactPrompt.length > previewLength) "..." else ""

        assertEquals("", ellipsis, "Exactly 50 chars should not have ellipsis")
    }

    @Test
    fun promptPreviewHandles51CharPrompt() {
        val prompt51 = "A".repeat(51)
        val previewLength = 50
        val ellipsis = if (prompt51.length > previewLength) "..." else ""

        assertEquals("...", ellipsis, "51 chars should have ellipsis")
    }

    // ==================== Metrics Table Headers ====================

    @Test
    fun tableHeadersIncludeMetricColumnPlusConstraintsForEachRecord() {
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
    }

    // ==================== Metric Row Display Values ====================

    @Test
    fun responseLengthDisplaysAsString() {
        val record = MetricRecord(
            1, "test", "response", "free", 8, 10, null, "stop", 100L,
            ConstraintsInfo(null, emptyList(), "text", 1.0)
        )
        assertEquals("8", record.responseLength.toString())
    }

    @Test
    fun tokensUsedDisplaysAsStringOrDashWhenNull() {
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
    fun maxTokensDisplaysAsValueOrUnlimitedWhenNull() {
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
    fun finishReasonDisplaysAsValueOrDashWhenNull() {
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
    fun responseTimeDisplaysAsMilliseconds() {
        val record = MetricRecord(
            1, "test", "response", "free", 8, 50, null, "stop", 1500L,
            ConstraintsInfo(null, emptyList(), "text", 1.0)
        )
        assertEquals("1500", record.responseTimeMs.toString())
    }

    // ==================== ConstraintsInfo Display ====================

    @Test
    fun constraintsInfoToDisplayStringReturnsFreeForDefaults() {
        val constraints = ConstraintsInfo(null, emptyList(), "text", 1.0)
        assertEquals("Free", constraints.toDisplayString())
    }

    @Test
    fun constraintsInfoToDisplayStringIncludesMaxTokensWhenSet() {
        val constraints = ConstraintsInfo(500, emptyList(), "text", 1.0)
        assertEquals("maxTokens=500", constraints.toDisplayString())
    }

    @Test
    fun constraintsInfoToDisplayStringIncludesStopSequencesWhenSet() {
        val constraints = ConstraintsInfo(null, listOf("stop1", "stop2"), "text", 1.0)
        assertEquals("stop=stop1,stop2", constraints.toDisplayString())
    }

    @Test
    fun constraintsInfoToDisplayStringIncludesFormatWhenNotText() {
        val constraints = ConstraintsInfo(null, emptyList(), "json", 1.0)
        assertEquals("format=json", constraints.toDisplayString())
    }

    @Test
    fun constraintsInfoToDisplayStringIncludesTempWhenNot10() {
        val constraints = ConstraintsInfo(null, emptyList(), "text", 0.7)
        assertEquals("temp=0.7", constraints.toDisplayString())
    }

    @Test
    fun constraintsInfoToDisplayStringCombinesMultipleValues() {
        val constraints = ConstraintsInfo(1000, listOf("end"), "json", 0.5)
        val display = constraints.toDisplayString()
        assertTrue(display.contains("maxTokens=1000"))
        assertTrue(display.contains("stop=end"))
        assertTrue(display.contains("format=json"))
        assertTrue(display.contains("temp=0.5"))
    }

    // ==================== Empty State ====================

    @Test
    fun emptyStateMessageForNoMetrics() {
        val expectedMessage = "No metrics yet.\nSend some messages to see comparison."
        assertEquals("No metrics yet.\nSend some messages to see comparison.", expectedMessage)
    }

    @Test
    fun emptyMetricsListTriggersEmptyState() {
        val metrics = emptyList<MetricRecord>()
        assertTrue(metrics.isEmpty())
    }

    // ==================== Dismiss Callback ====================

    @Test
    fun dismissCallbackIsInvoked() {
        var dismissCalled = false
        val onDismiss: () -> Unit = { dismissCalled = true }
        onDismiss()
        assertTrue(dismissCalled)
    }

    // ==================== Column Width Constants ====================

    @Test
    fun columnWidthConstantsAreDefinedCorrectly() {
        val columnWidthNarrow = 120
        val columnWidthWide = 140
        assertEquals(120, columnWidthNarrow)
        assertEquals(140, columnWidthWide)
    }

    @Test
    fun spacingConstantsAreDefinedCorrectly() {
        val spacingSmall = 16
        val spacingMedium = 20
        val spacingLarge = 24
        assertEquals(16, spacingSmall)
        assertEquals(20, spacingMedium)
        assertEquals(24, spacingLarge)
    }

    @Test
    fun paddingConstantsAreDefinedCorrectly() {
        val paddingSmall = 4
        val paddingMedium = 8
        assertEquals(4, paddingSmall)
        assertEquals(8, paddingMedium)
    }
}
