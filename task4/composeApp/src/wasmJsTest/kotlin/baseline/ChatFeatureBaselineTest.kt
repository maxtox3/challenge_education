package baseline

import ApiSettings
import ChatIntent
import ChatSideEffect
import ChatState
import MessageHandler
import SendMessageResult
import model.ChatMessage
import model.MetricRecord
import model.ReasoningComparison
import model.ReasoningMode
import model.ReasoningResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Characterization tests for Chat feature baseline behavior.
 * Tests capture CURRENT behavior, not desired behavior.
 */
class ChatFeatureBaselineTest {

    // ========== ChatState Tests ==========

    @Test
    fun chatState_default_values() {
        val state = ChatState()

        assertEquals("", state.inputText, "Default inputText should be empty string")
        assertEquals(emptyList<ChatMessage>(), state.messages, "Default messages should be empty list")
        assertFalse(state.isLoading, "Default isLoading should be false")
        assertNull(state.errorMessage, "Default errorMessage should be null")
        assertFalse(state.showSettings, "Default showSettings should be false")
        assertFalse(state.showMetrics, "Default showMetrics should be false")
        assertFalse(state.showReasoning, "Default showReasoning should be false")
        assertEquals(emptyList<MetricRecord>(), state.metrics, "Default metrics should be empty list")
        assertEquals(0, state.metricCounter, "Default metricCounter should be 0")
        assertEquals("", state.settings.apiKey, "Default settings.apiKey should be empty")
        assertEquals("glm-5", state.settings.model, "Default settings.model should be glm-5")
        assertFalse(state.isReasoningLoading, "Default isReasoningLoading should be false")
    }

    @Test
    fun chatState_reasoningComparison_default_task() {
        val state = ChatState()

        assertTrue(
            state.reasoningComparison.task.contains("12 монет"),
            "Default reasoning task should contain '12 монет'"
        )
        assertTrue(
            state.reasoningComparison.task.contains("фальшивая"),
            "Default reasoning task should contain 'фальшивая'"
        )
        assertEquals(mapOf<ReasoningMode, ReasoningResult>(), state.reasoningComparison.results)
    }

    @Test
    fun chatState_copy_with_new_values() {
        val original = ChatState()
        val message = ChatMessage(role = "user", content = "test")

        val updated = original.copy(
            inputText = "new input",
            messages = listOf(message),
            isLoading = true,
            errorMessage = "error"
        )

        assertEquals("new input", updated.inputText)
        assertEquals(1, updated.messages.size)
        assertTrue(updated.isLoading)
        assertEquals("error", updated.errorMessage)
        // Original should be unchanged
        assertEquals("", original.inputText)
        assertEquals(0, original.messages.size)
    }

    // ========== ChatIntent Tests ==========

    @Test
    fun chatIntent_UpdateInputText_preserves_text() {
        val intent = ChatIntent.UpdateInputText("test message")
        assertEquals("test message", intent.text)
    }

    @Test
    fun chatIntent_all_intent_types_are_instantiable() {
        val intents = listOf(
            ChatIntent.UpdateInputText("test"),
            ChatIntent.SendMessage,
            ChatIntent.ClearChat,
            ChatIntent.UpdateSettings(ApiSettings()),
            ChatIntent.ToggleSettings(true),
            ChatIntent.ToggleMetrics(true),
            ChatIntent.ToggleReasoning(true),
            ChatIntent.RunReasoningComparison("task"),
            ChatIntent.MessageSent(ChatMessage(role = "assistant", content = ""), createTestMetric()),
            ChatIntent.MessageSendFailed,
            ChatIntent.SetError("error"),
            ChatIntent.ClearError,
            ChatIntent.SetLoading(true),
            ChatIntent.UpdateReasoningComparison(ReasoningComparison("task", emptyMap())),
            ChatIntent.SetReasoningLoading(true)
        )

        assertEquals(15, intents.size, "All 15 intent types should be instantiable")
    }

    // ========== MessageHandler Tests ==========

    @Test
    fun messageHandler_validateInput_accepts_non_blank_text_when_not_loading() {
        val result = MessageHandler.validateInput("valid input", isLoading = false)
        assertTrue(result, "Should accept non-blank text when not loading")
    }

    @Test
    fun messageHandler_validateInput_rejects_blank_text() {
        val result = MessageHandler.validateInput("   ", isLoading = false)
        assertFalse(result, "Should reject blank text")
    }

    @Test
    fun messageHandler_formatUserMessage_creates_correct_message() {
        val message = MessageHandler.formatUserMessage("  test message  ")
        assertEquals("user", message.role, "Role should be 'user'")
        assertEquals("test message", message.content, "Content should be trimmed")
    }

    @Test
    fun messageHandler_validateAndPrepare_returns_Valid_for_good_input() {
        val result = MessageHandler.validateAndPrepare("  valid input  ", isLoading = false)

        assertTrue(result is MessageHandler.ValidationResult.Valid, "Should return Valid for good input")
        result as MessageHandler.ValidationResult.Valid
        assertEquals("valid input", result.prompt, "Prompt should be trimmed")
        assertEquals("user", result.message.role, "Message role should be 'user'")
        assertEquals("valid input", result.message.content, "Message content should be trimmed")
    }

    // ========== ChatSideEffect Tests ==========

    @Test
    fun chatSideEffect_types_are_instantiable() {
        val scrollToBottom = ChatSideEffect.ScrollToBottom
        val showToast = ChatSideEffect.ShowToast("test message")
        val hideKeyboard = ChatSideEffect.HideKeyboard

        assertEquals("test message", showToast.message, "ShowToast should preserve message")
        assertNotNull(scrollToBottom, "ScrollToBottom should be instantiable")
        assertNotNull(hideKeyboard, "HideKeyboard should be instantiable")
    }

    // ========== SendMessageResult Tests ==========

    @Test
    fun sendMessageResult_Success_contains_response_and_metric() {
        val response = ChatMessage(role = "assistant", content = "test")
        val metric = createTestMetric()
        val result = SendMessageResult.Success(response, metric)

        assertEquals(response, result.response)
        assertEquals(metric, result.metric)
    }

    // ========== Helper Functions ==========

    private fun createTestMetric(): MetricRecord = MetricRecord(
        id = 0,
        prompt = "test",
        response = "response",
        mode = "free",
        responseLength = 8,
        tokensUsed = 10,
        maxTokens = null,
        finishReason = "stop",
        responseTimeMs = 100,
        constraints = model.ConstraintsInfo(
            maxTokens = null,
            stopSequences = emptyList(),
            responseFormat = "text",
            temperature = 1.0
        )
    )
}
