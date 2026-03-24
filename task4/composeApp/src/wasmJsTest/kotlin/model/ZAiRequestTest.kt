package model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ZAiRequestTest {
    @Test
    fun testZAiRequestDefaults() {
        val request = ZAiRequest(messages = listOf(Message("user", "test")))
        assertEquals("glm-5", request.model)
        assertEquals(1, request.messages.size)
        assertFalse(request.stream)
        assertEquals(1.0, request.temperature)
        assertNull(request.maxTokens)
        assertNull(request.stop)
        assertNull(request.responseFormat)
    }

    @Test
    fun testZAiRequestWithAllFields() {
        val request = ZAiRequest(
            model = "custom-model",
            messages = listOf(Message("user", "test"), Message("assistant", "response")),
            stream = true,
            temperature = 0.5,
            maxTokens = 1000,
            stop = listOf("stop1", "stop2"),
            responseFormat = ResponseFormat("json_object"),
        )
        assertEquals("custom-model", request.model)
        assertEquals(2, request.messages.size)
        assertEquals(true, request.stream)
        assertEquals(0.5, request.temperature)
        assertEquals(1000, request.maxTokens)
        assertEquals(listOf("stop1", "stop2"), request.stop)
        assertEquals(ResponseFormat("json_object"), request.responseFormat)
    }

    @Test
    fun testMessageDefaults() {
        val message = Message("user", "content")
        assertEquals("user", message.role)
        assertEquals("content", message.content)
        assertNull(message.reasoningContent)
    }

    @Test
    fun testMessageWithReasoningContent() {
        val message = Message("assistant", "", reasoningContent = "reasoning")
        assertEquals("assistant", message.role)
        assertEquals("", message.content)
        assertEquals("reasoning", message.reasoningContent)
    }

    @Test
    fun testResponseFormat() {
        val format = ResponseFormat("json_object")
        assertEquals("json_object", format.type)
    }

    @Test
    fun testZAiResponseDefaults() {
        val response = ZAiResponse(choices = listOf())
        assertNull(response.id)
        assertEquals(0, response.choices.size)
        assertNull(response.usage)
    }

    @Test
    fun testZAiResponseWithFields() {
        val response = ZAiResponse(
            id = "test-id",
            choices = listOf(Choice(Message("assistant", "response"), "stop")),
            usage = Usage(10, 20, 30),
        )
        assertEquals("test-id", response.id)
        assertEquals(1, response.choices.size)
        assertEquals(10, response.usage?.promptTokens)
        assertEquals(20, response.usage?.completionTokens)
        assertEquals(30, response.usage?.totalTokens)
    }

    @Test
    fun testChoice() {
        val message = Message("assistant", "test response")
        val choice = Choice(message, "stop")
        assertEquals(message, choice.message)
        assertEquals("stop", choice.finishReason)
    }

    @Test
    fun testChoiceWithNullFinishReason() {
        val choice = Choice(Message("assistant", "test"), null)
        assertNull(choice.finishReason)
    }

    @Test
    fun testUsage() {
        val usage = Usage(100, 200, 300)
        assertEquals(100, usage.promptTokens)
        assertEquals(200, usage.completionTokens)
        assertEquals(300, usage.totalTokens)
    }

    @Test
    fun testZAiErrorResponse() {
        val errorResponse = ZAiErrorResponse(ZAiError("invalid_api_key", "Invalid API key"))
        assertEquals("invalid_api_key", errorResponse.error.code)
        assertEquals("Invalid API key", errorResponse.error.message)
    }

    @Test
    fun testZAiErrorWithNullMessage() {
        val error = ZAiError("error_code", null)
        assertEquals("error_code", error.code)
        assertNull(error.message)
    }
}
