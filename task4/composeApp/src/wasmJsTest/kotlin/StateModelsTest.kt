import chat.ChatIntent
import chat.ChatSideEffect
import chat.ChatState
import model.ChatMessage
import model.ConstraintsInfo
import model.MetricRecord
import model.ReasoningComparison
import settings.ApiSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StateModelsTest {

    @Test
    fun testChatStateDefaultValues() {
        val state = ChatState()
        assertEquals("", state.inputText)
        assertEquals(emptyList(), state.messages)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertFalse(state.showSettings)
        assertFalse(state.showMetrics)
        assertFalse(state.showReasoning)
        assertEquals(emptyList(), state.metrics)
        assertEquals(0, state.metricCounter)
        assertEquals(ApiSettings(), state.settings)
        assertFalse(state.isReasoningLoading)
    }

    @Test
    fun testChatStateDefaultReasoningComparison() {
        val state = ChatState()
        assertTrue(state.reasoningComparison.task.isNotEmpty())
        assertEquals(emptyMap(), state.reasoningComparison.results)
    }

    @Test
    fun testChatStateCopyWithInputText() {
        val original = ChatState()
        val copied = original.copy(inputText = "Hello")
        assertEquals("", original.inputText)
        assertEquals("Hello", copied.inputText)
    }

    @Test
    fun testChatStateCopyWithMessages() {
        val message = ChatMessage(role = "user", content = "test")
        val original = ChatState()
        val copied = original.copy(messages = listOf(message))
        assertEquals(emptyList(), original.messages)
        assertEquals(1, copied.messages.size)
        assertEquals("user", copied.messages[0].role)
        assertEquals("test", copied.messages[0].content)
    }

    @Test
    fun testChatStateCopyWithLoading() {
        val original = ChatState()
        val copied = original.copy(isLoading = true)
        assertFalse(original.isLoading)
        assertTrue(copied.isLoading)
    }

    @Test
    fun testChatStateCopyWithErrorMessage() {
        val original = ChatState()
        val copied = original.copy(errorMessage = "Error occurred")
        assertNull(original.errorMessage)
        assertEquals("Error occurred", copied.errorMessage)
    }

    @Test
    fun testChatStateCopyWithShowSettings() {
        val original = ChatState()
        val copied = original.copy(showSettings = true)
        assertFalse(original.showSettings)
        assertTrue(copied.showSettings)
    }

    @Test
    fun testChatStateCopyWithShowMetrics() {
        val original = ChatState()
        val copied = original.copy(showMetrics = true)
        assertFalse(original.showMetrics)
        assertTrue(copied.showMetrics)
    }

    @Test
    fun testChatStateCopyWithShowReasoning() {
        val original = ChatState()
        val copied = original.copy(showReasoning = true)
        assertFalse(original.showReasoning)
        assertTrue(copied.showReasoning)
    }

    @Test
    fun testChatStateCopyWithMetrics() {
        val metric = MetricRecord(
            id = 1,
            prompt = "test",
            response = "response",
            mode = "free",
            responseLength = 8,
            tokensUsed = 10,
            maxTokens = 100,
            finishReason = "stop",
            responseTimeMs = 500,
            constraints = ConstraintsInfo(null, emptyList(), "text", 1.0)
        )
        val original = ChatState()
        val copied = original.copy(metrics = listOf(metric))
        assertEquals(emptyList(), original.metrics)
        assertEquals(1, copied.metrics.size)
        assertEquals(1, copied.metrics[0].id)
    }

    @Test
    fun testChatStateCopyWithMetricCounter() {
        val original = ChatState()
        val copied = original.copy(metricCounter = 5)
        assertEquals(0, original.metricCounter)
        assertEquals(5, copied.metricCounter)
    }

    @Test
    fun testChatStateCopyWithSettings() {
        val newSettings = ApiSettings(apiKey = "test-key", model = "custom")
        val original = ChatState()
        val copied = original.copy(settings = newSettings)
        assertEquals(ApiSettings(), original.settings)
        assertEquals("test-key", copied.settings.apiKey)
        assertEquals("custom", copied.settings.model)
    }

    @Test
    fun testChatStateCopyWithReasoningComparison() {
        val newComparison = ReasoningComparison(
            task = "New task",
            results = emptyMap()
        )
        val original = ChatState()
        val copied = original.copy(reasoningComparison = newComparison)
        assertEquals("New task", copied.reasoningComparison.task)
    }

    @Test
    fun testChatStateCopyWithReasoningLoading() {
        val original = ChatState()
        val copied = original.copy(isReasoningLoading = true)
        assertFalse(original.isReasoningLoading)
        assertTrue(copied.isReasoningLoading)
    }

    @Test
    fun testChatStateImmutability() {
        val original = ChatState()
        val copied = original.copy(inputText = "modified")
        assertEquals("", original.inputText)
        assertEquals("modified", copied.inputText)
    }

    @Test
    fun testChatIntentUpdateInputText() {
        val intent = ChatIntent.UpdateInputText("Hello")
        assertEquals("Hello", intent.text)
    }

    @Test
    fun testChatIntentSendMessage() {
        val intent = ChatIntent.SendMessage
        assertEquals(ChatIntent.SendMessage, intent)
    }

    @Test
    fun testChatIntentClearChat() {
        val intent = ChatIntent.ClearChat
        assertEquals(ChatIntent.ClearChat, intent)
    }

    @Test
    fun testChatIntentUpdateSettings() {
        val settings = ApiSettings(apiKey = "key")
        val intent = ChatIntent.UpdateSettings(settings)
        assertEquals("key", intent.settings.apiKey)
    }

    @Test
    fun testChatIntentToggleSettings() {
        val intent = ChatIntent.ToggleSettings(true)
        assertTrue(intent.show)
    }

    @Test
    fun testChatIntentToggleMetrics() {
        val intent = ChatIntent.ToggleMetrics(true)
        assertTrue(intent.show)
    }

    @Test
    fun testChatIntentToggleReasoning() {
        val intent = ChatIntent.ToggleReasoning(true)
        assertTrue(intent.show)
    }

    @Test
    fun testChatIntentRunReasoningComparison() {
        val intent = ChatIntent.RunReasoningComparison("task")
        assertEquals("task", intent.task)
    }

    @Test
    fun testChatIntentMessageSent() {
        val message = ChatMessage(role = "assistant", content = "response")
        val metric = MetricRecord(
            id = 1,
            prompt = "test",
            response = "response",
            mode = "free",
            responseLength = 8,
            tokensUsed = 10,
            maxTokens = 100,
            finishReason = "stop",
            responseTimeMs = 500,
            constraints = ConstraintsInfo(null, emptyList(), "text", 1.0)
        )
        val intent = ChatIntent.MessageSent(message, metric)
        assertEquals("assistant", intent.response.role)
        assertEquals(1, intent.metric.id)
    }

    @Test
    fun testChatIntentMessageSendFailed() {
        val intent = ChatIntent.MessageSendFailed
        assertEquals(ChatIntent.MessageSendFailed, intent)
    }

    @Test
    fun testChatIntentSetError() {
        val intent = ChatIntent.SetError("error message")
        assertEquals("error message", intent.message)
    }

    @Test
    fun testChatIntentClearError() {
        val intent = ChatIntent.ClearError
        assertEquals(ChatIntent.ClearError, intent)
    }

    @Test
    fun testChatIntentSetLoading() {
        val intent = ChatIntent.SetLoading(true)
        assertTrue(intent.loading)
    }

    @Test
    fun testChatIntentUpdateReasoningComparison() {
        val comparison = ReasoningComparison(task = "task", results = emptyMap())
        val intent = ChatIntent.UpdateReasoningComparison(comparison)
        assertEquals("task", intent.comparison.task)
    }

    @Test
    fun testChatIntentSetReasoningLoading() {
        val intent = ChatIntent.SetReasoningLoading(true)
        assertTrue(intent.loading)
    }

    @Test
    fun testChatSideEffectScrollToBottom() {
        val effect = ChatSideEffect.ScrollToBottom
        assertEquals(ChatSideEffect.ScrollToBottom, effect)
    }

    @Test
    fun testChatSideEffectShowToast() {
        val effect = ChatSideEffect.ShowToast("message")
        assertEquals("message", effect.message)
    }

    @Test
    fun testChatSideEffectHideKeyboard() {
        val effect = ChatSideEffect.HideKeyboard
        assertEquals(ChatSideEffect.HideKeyboard, effect)
    }

    @Test
    fun testChatSideEffectEquality() {
        val effect1 = ChatSideEffect.ScrollToBottom
        val effect2 = ChatSideEffect.ScrollToBottom
        assertEquals(effect1, effect2)
    }

    @Test
    fun testChatSideEffectShowToastEquality() {
        val effect1 = ChatSideEffect.ShowToast("test")
        val effect2 = ChatSideEffect.ShowToast("test")
        assertEquals(effect1, effect2)
    }
}
