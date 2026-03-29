package baseline

import kotlinx.serialization.json.Json
import model.ChatMessage
import model.ConstraintsInfo
import model.Delta
import model.Message
import model.MetricRecord
import model.ReasoningComparison
import model.ReasoningMode
import model.ReasoningResult
import model.ResponseFormat
import model.StreamChunk
import model.Usage
import model.ZAiError
import model.ZAiErrorResponse
import model.ZAiRequest
import model.ZAiResponse
import model.ZAiStreamChunk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Characterization tests for core:model module baseline.
 * Tests capture CURRENT behavior - not desired behavior.
 */

class ModelBaselineTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // ============================================
    // ChatMessage Tests
    // ============================================

    @Test
    fun testChatMessageDefaultValues() {
        val msg = ChatMessage(role = "user", content = "hello")

        assertEquals("user", msg.role)
        assertEquals("hello", msg.content)
        assertEquals(0L, msg.timestamp)
        assertEquals("free", msg.mode)
        assertNull(msg.tokensUsed)
        assertNull(msg.maxTokens)
        assertNull(msg.finishReason)
        assertFalse(msg.isReasoningContent)
        assertFalse(msg.isStreaming)
    }

    @Test
    fun testChatMessageWithAllFields() {
        val msg = ChatMessage(
            role = "assistant",
            content = "response",
            timestamp = 12345L,
            mode = "constrained",
            tokensUsed = 100,
            maxTokens = 500,
            finishReason = "stop",
            isReasoningContent = true,
            isStreaming = true
        )

        assertEquals("assistant", msg.role)
        assertEquals("response", msg.content)
        assertEquals(12345L, msg.timestamp)
        assertEquals("constrained", msg.mode)
        assertEquals(100, msg.tokensUsed)
        assertEquals(500, msg.maxTokens)
        assertEquals("stop", msg.finishReason)
        assertTrue(msg.isReasoningContent)
        assertTrue(msg.isStreaming)
    }

    @Test
    fun testChatMessageCopy() {
        val original = ChatMessage(role = "user", content = "test")
        val copy = original.copy(content = "modified")

        assertEquals("user", copy.role)
        assertEquals("modified", copy.content)
        assertEquals(original.timestamp, copy.timestamp)
    }

    @Test
    fun testChatMessageEquality() {
        val msg1 = ChatMessage(role = "user", content = "test")
        val msg2 = ChatMessage(role = "user", content = "test")
        val msg3 = ChatMessage(role = "user", content = "different")

        assertEquals(msg1, msg2)
        assertNotEquals(msg1, msg3)
    }

    // ============================================
    // StreamChunk Tests
    // ============================================

    @Test
    fun testStreamChunkContent() {
        val chunk = StreamChunk.Content("hello")

        assertIs<StreamChunk.Content>(chunk)
        assertEquals("hello", chunk.text)
    }

    @Test
    fun testStreamChunkReasoning() {
        val chunk = StreamChunk.Reasoning("thinking...")

        assertIs<StreamChunk.Reasoning>(chunk)
        assertEquals("thinking...", chunk.text)
    }

    @Test
    fun testStreamChunkDone() {
        val chunk = StreamChunk.Done

        assertIs<StreamChunk.Done>(chunk)
    }

    @Test
    fun testStreamChunkDoneIsSingleton() {
        val chunk1 = StreamChunk.Done
        val chunk2 = StreamChunk.Done

        assertEquals(chunk1, chunk2)
    }

    @Test
    fun testStreamChunkContentEquality() {
        val chunk1 = StreamChunk.Content("test")
        val chunk2 = StreamChunk.Content("test")
        val chunk3 = StreamChunk.Content("other")

        assertEquals(chunk1, chunk2)
        assertNotEquals(chunk1, chunk3)
    }

    @Test
    fun testStreamChunkReasoningEquality() {
        val chunk1 = StreamChunk.Reasoning("think")
        val chunk2 = StreamChunk.Reasoning("think")
        val chunk3 = StreamChunk.Reasoning("other")

        assertEquals(chunk1, chunk2)
        assertNotEquals(chunk1, chunk3)
    }

    // ============================================
    // ReasoningMode Tests
    // ============================================

    @Test
    fun testReasoningModeDirect() {
        val mode = ReasoningMode.DIRECT

        assertEquals("Прямой ответ", mode.displayName)
        assertEquals("Без дополнительных инструкций", mode.description)
    }

    @Test
    fun testReasoningModeStepByStep() {
        val mode = ReasoningMode.STEP_BY_STEP

        assertEquals("Пошагово", mode.displayName)
        assertEquals("Инструкция: решай пошагово", mode.description)
    }

    @Test
    fun testReasoningModeMetaPrompt() {
        val mode = ReasoningMode.META_PROMPT

        assertEquals("Мета-промпт", mode.displayName)
        assertEquals("Сначала составляет промпт, затем использует его", mode.description)
    }

    @Test
    fun testReasoningModeExpertPanel() {
        val mode = ReasoningMode.EXPERT_PANEL

        assertEquals("Эксперты", mode.displayName)
        assertEquals("Группа экспертов: аналитик, инженер, критик", mode.description)
    }

    @Test
    fun testReasoningModeValues() {
        val values = ReasoningMode.entries

        assertEquals(4, values.size)
        assertTrue(values.contains(ReasoningMode.DIRECT))
        assertTrue(values.contains(ReasoningMode.STEP_BY_STEP))
        assertTrue(values.contains(ReasoningMode.META_PROMPT))
        assertTrue(values.contains(ReasoningMode.EXPERT_PANEL))
    }

    // ============================================
    // ReasoningResult Tests
    // ============================================

    @Test
    fun testReasoningResultDefaultValues() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "system",
            actualPrompt = "actual"
        )

        assertEquals(ReasoningMode.DIRECT, result.mode)
        assertEquals("system", result.systemPrompt)
        assertEquals("actual", result.actualPrompt)
        assertEquals("", result.response)
        assertEquals(0L, result.responseTimeMs)
        assertNull(result.tokensUsed)
        assertFalse(result.isLoading)
        assertNull(result.error)
    }

    @Test
    fun testReasoningResultWithAllFields() {
        val result = ReasoningResult(
            mode = ReasoningMode.STEP_BY_STEP,
            systemPrompt = "sys",
            actualPrompt = "act",
            response = "resp",
            responseTimeMs = 500L,
            tokensUsed = 100,
            isLoading = true,
            error = "some error"
        )

        assertEquals(ReasoningMode.STEP_BY_STEP, result.mode)
        assertEquals("resp", result.response)
        assertEquals(500L, result.responseTimeMs)
        assertEquals(100, result.tokensUsed)
        assertTrue(result.isLoading)
        assertEquals("some error", result.error)
    }

    // ============================================
    // ReasoningComparison Tests
    // ============================================

    @Test
    fun testReasoningComparisonIsCompleteWhenAllResultsPresent() {
        val results = mapOf(
            ReasoningMode.DIRECT to ReasoningResult(
                mode = ReasoningMode.DIRECT,
                systemPrompt = "s",
                actualPrompt = "a",
                response = "response"
            )
        )
        val comparison = ReasoningComparison(task = "test", results = results)

        assertTrue(comparison.isComplete)
        assertTrue(comparison.hasAnyResult)
    }

    @Test
    fun testReasoningComparisonNotCompleteWhenLoading() {
        val results = mapOf(
            ReasoningMode.DIRECT to ReasoningResult(
                mode = ReasoningMode.DIRECT,
                systemPrompt = "s",
                actualPrompt = "a",
                response = "response",
                isLoading = true
            )
        )
        val comparison = ReasoningComparison(task = "test", results = results)

        assertFalse(comparison.isComplete)
        assertTrue(comparison.hasAnyResult)
    }

    @Test
    fun testReasoningComparisonNotCompleteWhenError() {
        val results = mapOf(
            ReasoningMode.DIRECT to ReasoningResult(
                mode = ReasoningMode.DIRECT,
                systemPrompt = "s",
                actualPrompt = "a",
                response = "",
                error = "failed"
            )
        )
        val comparison = ReasoningComparison(task = "test", results = results)

        assertFalse(comparison.isComplete)
        assertFalse(comparison.hasAnyResult)
    }

    @Test
    fun testReasoningComparisonNotCompleteWhenEmptyResponse() {
        val results = mapOf(
            ReasoningMode.DIRECT to ReasoningResult(
                mode = ReasoningMode.DIRECT,
                systemPrompt = "s",
                actualPrompt = "a",
                response = ""
            )
        )
        val comparison = ReasoningComparison(task = "test", results = results)

        assertFalse(comparison.isComplete)
        assertFalse(comparison.hasAnyResult)
    }

    // ============================================
    // MetricRecord Tests
    // ============================================

    @Test
    fun testMetricRecordCreation() {
        val constraints = ConstraintsInfo(
            maxTokens = 100,
            stopSequences = listOf("stop"),
            responseFormat = "text",
            temperature = 0.7
        )
        val record = MetricRecord(
            id = 1,
            prompt = "test prompt",
            response = "test response",
            mode = "free",
            responseLength = 13,
            tokensUsed = 50,
            maxTokens = 100,
            finishReason = "stop",
            responseTimeMs = 200L,
            constraints = constraints
        )

        assertEquals(1, record.id)
        assertEquals("test prompt", record.prompt)
        assertEquals("test response", record.response)
        assertEquals("free", record.mode)
        assertEquals(13, record.responseLength)
        assertEquals(50, record.tokensUsed)
        assertEquals(100, record.maxTokens)
        assertEquals("stop", record.finishReason)
        assertEquals(200L, record.responseTimeMs)
        assertEquals(constraints, record.constraints)
    }

    // ============================================
    // ConstraintsInfo Tests
    // ============================================

    @Test
    fun testConstraintsInfoToDisplayStringFree() {
        val info = ConstraintsInfo(
            maxTokens = null,
            stopSequences = emptyList(),
            responseFormat = "text",
            temperature = 1.0
        )

        assertEquals("Free", info.toDisplayString())
    }

    @Test
    fun testConstraintsInfoToDisplayStringWithMaxTokens() {
        val info = ConstraintsInfo(
            maxTokens = 100,
            stopSequences = emptyList(),
            responseFormat = "text",
            temperature = 1.0
        )

        assertEquals("maxTokens=100", info.toDisplayString())
    }

    @Test
    fun testConstraintsInfoToDisplayStringWithStopSequences() {
        val info = ConstraintsInfo(
            maxTokens = null,
            stopSequences = listOf("stop1", "stop2"),
            responseFormat = "text",
            temperature = 1.0
        )

        assertEquals("stop=stop1,stop2", info.toDisplayString())
    }

    @Test
    fun testConstraintsInfoToDisplayStringWithResponseFormat() {
        val info = ConstraintsInfo(
            maxTokens = null,
            stopSequences = emptyList(),
            responseFormat = "json",
            temperature = 1.0
        )

        assertEquals("format=json", info.toDisplayString())
    }

    @Test
    fun testConstraintsInfoToDisplayStringWithTemperature() {
        val info = ConstraintsInfo(
            maxTokens = null,
            stopSequences = emptyList(),
            responseFormat = "text",
            temperature = 0.5
        )

        assertEquals("temp=0.5", info.toDisplayString())
    }

    @Test
    fun testConstraintsInfoToDisplayStringWithMultipleConstraints() {
        val info = ConstraintsInfo(
            maxTokens = 200,
            stopSequences = listOf("END"),
            responseFormat = "json",
            temperature = 0.8
        )

        val result = info.toDisplayString()

        assertTrue(result.contains("maxTokens=200"))
        assertTrue(result.contains("stop=END"))
        assertTrue(result.contains("format=json"))
        assertTrue(result.contains("temp=0.8"))
    }

    // ============================================
    // ZAiRequest Serialization Tests
    // ============================================

    @Test
    fun testZAiRequestSerialization() {
        val request = ZAiRequest(
            model = "glm-5",
            messages = listOf(Message("user", "hello")),
            stream = false,
            temperature = 1.0,
            maxTokens = null,
            stop = null,
            responseFormat = null
        )

        val jsonStr = json.encodeToString(request)

        assertTrue(jsonStr.contains("\"model\":\"glm-5\""))
        assertTrue(jsonStr.contains("\"stream\":false"))
        assertTrue(jsonStr.contains("\"temperature\":1.0"))
    }

    @Test
    fun testZAiRequestSerializationWithMaxTokens() {
        val request = ZAiRequest(
            model = "glm-5",
            messages = listOf(Message("user", "test")),
            maxTokens = 100
        )

        val jsonStr = json.encodeToString(request)

        assertTrue(jsonStr.contains("\"max_tokens\":100"))
    }

    @Test
    fun testZAiRequestSerializationWithResponseFormat() {
        val request = ZAiRequest(
            model = "glm-5",
            messages = listOf(Message("user", "test")),
            responseFormat = ResponseFormat("json_object")
        )

        val jsonStr = json.encodeToString(request)

        assertTrue(jsonStr.contains("\"response_format\""))
        assertTrue(jsonStr.contains("\"type\":\"json_object\""))
    }

    @Test
    fun testZAiRequestDeserialization() {
        val jsonStr = """
            {"model":"glm-5","messages":[{"role":"user","content":"hello"}],"stream":false,"temperature":1.0}
        """.trimIndent()
        val request = json.decodeFromString<ZAiRequest>(jsonStr)

        assertEquals("glm-5", request.model)
        assertEquals(1, request.messages.size)
        assertEquals("user", request.messages[0].role)
        assertEquals("hello", request.messages[0].content)
        assertFalse(request.stream)
        assertEquals(1.0, request.temperature)
    }

    // ============================================
    // Message Tests
    // ============================================

    @Test
    fun testMessageSerialization() {
        val message = Message("user", "test content", "reasoning")

        val jsonStr = json.encodeToString(message)

        assertTrue(jsonStr.contains("\"role\":\"user\""))
        assertTrue(jsonStr.contains("\"content\":\"test content\""))
        assertTrue(jsonStr.contains("\"reasoning_content\":\"reasoning\""))
    }

    @Test
    fun testMessageSerializationWithoutReasoning() {
        val message = Message("assistant", "response")

        val jsonStr = json.encodeToString(message)

        assertTrue(jsonStr.contains("\"role\":\"assistant\""))
        assertTrue(jsonStr.contains("\"content\":\"response\""))
    }

    @Test
    fun testMessageDeserialization() {
        val jsonStr = """{"role":"system","content":"You are helpful"}"""
        val message = json.decodeFromString<Message>(jsonStr)

        assertEquals("system", message.role)
        assertEquals("You are helpful", message.content)
        assertNull(message.reasoningContent)
    }

    // ============================================
    // ZAiResponse Tests
    // ============================================

    @Test
    fun testZAiResponseDeserialization() {
        val jsonStr = """
            {"id":"chat-123","choices":[{"message":{"role":"assistant","content":"Hello!"},"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}
        """.trimIndent()
        val response = json.decodeFromString<ZAiResponse>(jsonStr)

        assertEquals("chat-123", response.id)
        assertEquals(1, response.choices.size)
        assertEquals("assistant", response.choices[0].message.role)
        assertEquals("Hello!", response.choices[0].message.content)
        assertEquals("stop", response.choices[0].finishReason)
        assertEquals(10, response.usage?.promptTokens)
        assertEquals(5, response.usage?.completionTokens)
        assertEquals(15, response.usage?.totalTokens)
    }

    @Test
    fun testZAiResponseWithNullId() {
        val jsonStr = """{"id":null,"choices":[],"usage":null}"""
        val response = json.decodeFromString<ZAiResponse>(jsonStr)

        assertNull(response.id)
        assertTrue(response.choices.isEmpty())
        assertNull(response.usage)
    }

    // ============================================
    // ZAiErrorResponse Tests
    // ============================================

    @Test
    fun testZAiErrorResponseDeserialization() {
        val jsonStr = """{"error":{"code":"rate_limit_exceeded","message":"Too many requests"}}"""
        val errorResponse = json.decodeFromString<ZAiErrorResponse>(jsonStr)

        assertEquals("rate_limit_exceeded", errorResponse.error.code)
        assertEquals("Too many requests", errorResponse.error.message)
    }

    @Test
    fun testZAiErrorWithoutMessage() {
        val jsonStr = """{"code":"internal_error","message":null}"""
        val error = json.decodeFromString<ZAiError>(jsonStr)

        assertEquals("internal_error", error.code)
        assertNull(error.message)
    }

    // ============================================
    // ZAiStreamChunk Tests
    // ============================================

    @Test
    fun testZAiStreamChunkDeserialization() {
        val jsonStr = """{"id":"stream-1","choices":[{"delta":{"content":"Hello"},"finish_reason":null}]}"""
        val chunk = json.decodeFromString<ZAiStreamChunk>(jsonStr)

        assertEquals("stream-1", chunk.id)
        assertEquals(1, chunk.choices.size)
        assertEquals("Hello", chunk.choices[0].delta.content)
        assertNull(chunk.choices[0].delta.reasoningContent)
        assertNull(chunk.choices[0].finishReason)
    }

    @Test
    fun testZAiStreamChunkWithReasoningContent() {
        val jsonStr = """{"choices":[{"delta":{"reasoning_content":"thinking..."}}]}"""
        val chunk = json.decodeFromString<ZAiStreamChunk>(jsonStr)

        assertEquals("thinking...", chunk.choices[0].delta.reasoningContent)
        assertNull(chunk.choices[0].delta.content)
    }

    @Test
    fun testDeltaWithNullContent() {
        val jsonStr = """{"content":null,"reasoning_content":null}"""
        val delta = json.decodeFromString<Delta>(jsonStr)

        assertNull(delta.content)
        assertNull(delta.reasoningContent)
    }

    // ============================================
    // ResponseFormat Tests
    // ============================================

    @Test
    fun testResponseFormatSerialization() {
        val format = ResponseFormat("json_object")

        val jsonStr = json.encodeToString(format)

        assertEquals("""{"type":"json_object"}""", jsonStr)
    }

    @Test
    fun testResponseFormatDeserialization() {
        val jsonStr = """{"type":"text"}"""
        val format = json.decodeFromString<ResponseFormat>(jsonStr)

        assertEquals("text", format.type)
    }

    // ============================================
    // Usage Tests
    // ============================================

    @Test
    fun testUsageSerialization() {
        val usage = Usage(promptTokens = 10, completionTokens = 20, totalTokens = 30)

        val jsonStr = json.encodeToString(usage)

        assertTrue(jsonStr.contains("\"prompt_tokens\":10"))
        assertTrue(jsonStr.contains("\"completion_tokens\":20"))
        assertTrue(jsonStr.contains("\"total_tokens\":30"))
    }

    @Test
    fun testUsageDeserialization() {
        val jsonStr = """{"prompt_tokens":5,"completion_tokens":15,"total_tokens":20}"""
        val usage = json.decodeFromString<Usage>(jsonStr)

        assertEquals(5, usage.promptTokens)
        assertEquals(15, usage.completionTokens)
        assertEquals(20, usage.totalTokens)
    }
}
