package model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MetricRecordTest {

    @Test
    fun testMetricRecord() {
        val constraints = ConstraintsInfo(
            maxTokens = 1000,
            stopSequences = listOf("stop"),
            responseFormat = "text",
            temperature = 1.0,
        )
        val record = MetricRecord(
            id = 1,
            prompt = "test prompt",
            response = "test response",
            mode = "free",
            responseLength = 13,
            tokensUsed = 50,
            maxTokens = 1000,
            finishReason = "stop",
            responseTimeMs = 500L,
            constraints = constraints,
        )
        assertEquals(1, record.id)
        assertEquals("test prompt", record.prompt)
        assertEquals("test response", record.response)
        assertEquals("free", record.mode)
        assertEquals(13, record.responseLength)
        assertEquals(50, record.tokensUsed)
        assertEquals(1000, record.maxTokens)
        assertEquals("stop", record.finishReason)
        assertEquals(500L, record.responseTimeMs)
    }

    @Test
    fun testMetricRecordWithNulls() {
        val constraints = ConstraintsInfo(
            maxTokens = null,
            stopSequences = emptyList(),
            responseFormat = "text",
            temperature = 1.0,
        )
        val record = MetricRecord(
            id = 2,
            prompt = "prompt",
            response = "response",
            mode = "constrained",
            responseLength = 8,
            tokensUsed = null,
            maxTokens = null,
            finishReason = null,
            responseTimeMs = 1000L,
            constraints = constraints,
        )
        assertNull(record.tokensUsed)
        assertNull(record.maxTokens)
        assertNull(record.finishReason)
    }

    @Test
    fun testConstraintsInfoDefaults() {
        val constraints = ConstraintsInfo(
            maxTokens = null,
            stopSequences = emptyList(),
            responseFormat = "text",
            temperature = 1.0,
        )
        assertEquals("Free", constraints.toDisplayString())
    }

    @Test
    fun testConstraintsInfoWithMaxTokens() {
        val constraints = ConstraintsInfo(
            maxTokens = 500,
            stopSequences = emptyList(),
            responseFormat = "text",
            temperature = 1.0,
        )
        assertEquals("maxTokens=500", constraints.toDisplayString())
    }

    @Test
    fun testConstraintsInfoWithStopSequences() {
        val constraints = ConstraintsInfo(
            maxTokens = null,
            stopSequences = listOf("stop1", "stop2"),
            responseFormat = "text",
            temperature = 1.0,
        )
        assertEquals("stop=stop1,stop2", constraints.toDisplayString())
    }

    @Test
    fun testConstraintsInfoWithJsonFormat() {
        val constraints = ConstraintsInfo(
            maxTokens = null,
            stopSequences = emptyList(),
            responseFormat = "json",
            temperature = 1.0,
        )
        assertEquals("format=json", constraints.toDisplayString())
    }

    @Test
    fun testConstraintsInfoWithCustomTemperature() {
        val constraints = ConstraintsInfo(
            maxTokens = null,
            stopSequences = emptyList(),
            responseFormat = "text",
            temperature = 0.7,
        )
        assertEquals("temp=0.7", constraints.toDisplayString())
    }

    @Test
    fun testConstraintsInfoWithAllFields() {
        val constraints = ConstraintsInfo(
            maxTokens = 1000,
            stopSequences = listOf("end"),
            responseFormat = "json",
            temperature = 0.5,
        )
        val display = constraints.toDisplayString()
        assertTrue(display.contains("maxTokens=1000"))
        assertTrue(display.contains("stop=end"))
        assertTrue(display.contains("format=json"))
        assertTrue(display.contains("temp=0.5"))
    }

    @Test
    fun testConstraintsInfoTextFormatNotShown() {
        val constraints = ConstraintsInfo(
            maxTokens = null,
            stopSequences = emptyList(),
            responseFormat = "text",
            temperature = 1.0,
        )
        val display = constraints.toDisplayString()
        assertFalse(display.contains("format"))
    }

    @Test
    fun testConstraintsInfoDefaultTemperatureNotShown() {
        val constraints = ConstraintsInfo(
            maxTokens = null,
            stopSequences = emptyList(),
            responseFormat = "text",
            temperature = 1.0,
        )
        val display = constraints.toDisplayString()
        assertFalse(display.contains("temp"))
    }

    private fun assertTrue(condition: Boolean) {
        kotlin.test.assertTrue(condition)
    }

    private fun assertFalse(condition: Boolean) {
        kotlin.test.assertFalse(condition)
    }
}
