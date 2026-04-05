import chat.ChatIntent
import chat.ChatRepository
import chat.SendMessageResult
import chat.store.ChatStore
import chat.store.ChatStoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import model.ChatMessage
import model.ConstraintsInfo
import model.MetricRecord
import model.ReasoningComparison
import model.ReasoningMode
import model.ReasoningResult
import model.StreamChunk
import settings.ApiSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatStoreTest {
    private val fakeRepository = FakeChatRepository()

    private fun createStore(): ChatStore = ChatStoreFactory(
        storeFactory = DefaultStoreFactory(),
        repository = fakeRepository
    ).create()

    @Test
    fun testInitialState() = runTest {
        val store = createStore()

        assertEquals("", store.state.inputText)
        assertEquals(emptyList<ChatMessage>(), store.state.messages)
        assertFalse(store.state.isLoading)
        assertNull(store.state.errorMessage)
        assertFalse(store.state.showSettings)
        assertFalse(store.state.showMetrics)
        assertFalse(store.state.showReasoning)
        assertEquals(emptyList<MetricRecord>(), store.state.metrics)
        assertEquals(0, store.state.metricCounter)
        assertEquals("glm-5", store.state.settings.model)
        assertFalse(store.state.isReasoningLoading)
    }

    @Test
    fun testIntentUpdateInputText() = runTest {
        val store = createStore()
        store.accept(ChatIntent.UpdateInputText("test message"))

        assertEquals("test message", store.state.inputText)
    }

    @Test
    fun testIntentToggleSettings() = runTest {
        val store = createStore()

        store.accept(ChatIntent.ToggleSettings(true))
        assertTrue(store.state.showSettings)

        store.accept(ChatIntent.ToggleSettings(false))
        assertFalse(store.state.showSettings)
    }

    @Test
    fun testIntentToggleMetrics() = runTest {
        val store = createStore()

        store.accept(ChatIntent.ToggleMetrics(true))
        assertTrue(store.state.showMetrics)

        store.accept(ChatIntent.ToggleMetrics(false))
        assertFalse(store.state.showMetrics)
    }

    @Test
    fun testIntentToggleReasoning() = runTest {
        val store = createStore()

        store.accept(ChatIntent.ToggleReasoning(true))
        assertTrue(store.state.showReasoning)

        store.accept(ChatIntent.ToggleReasoning(false))
        assertFalse(store.state.showReasoning)
    }

    @Test
    fun testIntentUpdateSettings() = runTest {
        val store = createStore()
        val newSettings = ApiSettings(
            apiKey = "test-key",
            model = "glm-4",
            temperature = 0.7
        )

        store.accept(ChatIntent.UpdateSettings(newSettings))

        assertEquals("test-key", store.state.settings.apiKey)
        assertEquals("glm-4", store.state.settings.model)
        assertEquals(0.7, store.state.settings.temperature)
    }

    @Test
    fun testIntentSetError() = runTest {
        val store = createStore()

        store.accept(ChatIntent.SetError("Test error"))

        assertEquals("Test error", store.state.errorMessage)
    }

    @Test
    fun testIntentClearError() = runTest {
        val store = createStore()
        store.accept(ChatIntent.SetError("Test error"))

        store.accept(ChatIntent.ClearError)

        assertNull(store.state.errorMessage)
    }

    @Test
    fun testIntentSetLoading() = runTest {
        val store = createStore()

        store.accept(ChatIntent.SetLoading(true))
        assertTrue(store.state.isLoading)

        store.accept(ChatIntent.SetLoading(false))
        assertFalse(store.state.isLoading)
    }

    @Test
    fun testIntentSetReasoningLoading() = runTest {
        val store = createStore()

        store.accept(ChatIntent.SetReasoningLoading(true))
        assertTrue(store.state.isReasoningLoading)

        store.accept(ChatIntent.SetReasoningLoading(false))
        assertFalse(store.state.isReasoningLoading)
    }

    @Test
    fun testIntentClearChat() = runTest {
        val store = createStore()
        store.accept(ChatIntent.UpdateInputText("test"))
        store.accept(ChatIntent.SetError("error"))

        store.accept(ChatIntent.ClearChat)

        assertEquals(emptyList<ChatMessage>(), store.state.messages)
        assertEquals(emptyList<MetricRecord>(), store.state.metrics)
        assertEquals(0, store.state.metricCounter)
        assertNull(store.state.errorMessage)
    }

    @Test
    fun testIntentMessageSent() = runTest {
        val store = createStore()
        val message = ChatMessage(role = "assistant", content = "Test response")
        val metric = createTestMetric()

        store.accept(ChatIntent.MessageSent(message, metric))

        assertEquals(1, store.state.messages.size)
        assertEquals("Test response", store.state.messages.first().content)
        assertEquals(1, store.state.metrics.size)
        assertEquals(1, store.state.metricCounter)
    }

    @Test
    fun testIntentMessageSentWithoutMetric() = runTest {
        val store = createStore()
        val message = ChatMessage(role = "assistant", content = "Test response")

        store.accept(ChatIntent.MessageSent(message, null))

        assertEquals(1, store.state.messages.size)
        assertEquals("Test response", store.state.messages.first().content)
        assertEquals(0, store.state.metrics.size)
        assertEquals(0, store.state.metricCounter)
    }

    @Test
    fun testIntentUpdateReasoningComparison() = runTest {
        val store = createStore()
        val comparison = ReasoningComparison(
            task = "Test task",
            results = mapOf(
                ReasoningMode.DIRECT to ReasoningResult(
                    mode = ReasoningMode.DIRECT,
                    systemPrompt = "",
                    actualPrompt = "Test task",
                    response = "Test response",
                    isLoading = false
                )
            )
        )

        store.accept(ChatIntent.UpdateReasoningComparison(comparison))

        assertEquals("Test task", store.state.reasoningComparison.task)
        assertTrue(store.state.reasoningComparison.results.containsKey(ReasoningMode.DIRECT))
    }

    @Test
    fun testSendMessageSuccess() = runTest {
        val store = createStore()
        fakeRepository.sendMessageResult = SendMessageResult.Success(
            response = ChatMessage(role = "assistant", content = "Hello!"),
            metric = createTestMetric()
        )
        store.accept(ChatIntent.UpdateInputText("Hello"))

        store.accept(ChatIntent.SendMessage)

        assertTrue(store.state.messages.isNotEmpty())
        assertEquals("", store.state.inputText)
        assertFalse(store.state.isLoading)
    }

    @Test
    fun testSendMessageError() = runTest {
        val store = createStore()
        fakeRepository.sendMessageResult = SendMessageResult.Error("API Error")
        store.accept(ChatIntent.UpdateInputText("Hello"))

        store.accept(ChatIntent.SendMessage)

        assertFalse(store.state.isLoading)
        assertEquals("API Error", store.state.errorMessage)
    }

    @Test
    fun testSendMessageWithBlankInputDoesNothing() = runTest {
        val store = createStore()
        store.accept(ChatIntent.UpdateInputText("   "))

        store.accept(ChatIntent.SendMessage)

        assertEquals(emptyList<ChatMessage>(), store.state.messages)
        assertFalse(store.state.isLoading)
    }

    @Test
    fun testMultipleIntentsInSequence() = runTest {
        val store = createStore()

        store.accept(ChatIntent.UpdateInputText("First"))
        assertEquals("First", store.state.inputText)

        store.accept(ChatIntent.UpdateInputText("Second"))
        assertEquals("Second", store.state.inputText)

        store.accept(ChatIntent.ToggleSettings(true))
        assertTrue(store.state.showSettings)

        store.accept(ChatIntent.SetError("Error"))
        assertEquals("Error", store.state.errorMessage)

        store.accept(ChatIntent.ClearError)
        assertNull(store.state.errorMessage)
    }

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
        constraints = ConstraintsInfo(
            maxTokens = null,
            stopSequences = emptyList(),
            responseFormat = "text",
            temperature = 1.0
        )
    )
}

class FakeChatRepository : ChatRepository {
    var sendMessageResult: SendMessageResult = SendMessageResult.Success(
        ChatMessage(role = "assistant", content = "Default"),
        MetricRecord(
            id = 0,
            prompt = "",
            response = "Default",
            mode = "free",
            responseLength = 7,
            tokensUsed = 5,
            maxTokens = null,
            finishReason = "stop",
            responseTimeMs = 100,
            constraints = ConstraintsInfo(
                maxTokens = null,
                stopSequences = emptyList(),
                responseFormat = "text",
                temperature = 1.0
            )
        )
    )

    override suspend fun sendMessage(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings
    ): SendMessageResult = sendMessageResult

    override suspend fun runReasoningComparison(
        task: String,
        settings: ApiSettings,
        onProgress: (ReasoningComparison) -> Unit
    ): ReasoningComparison {
        val results = ReasoningMode.entries.associateWith { mode ->
            ReasoningResult(
                mode = mode,
                systemPrompt = "",
                actualPrompt = task,
                response = "Test response",
                isLoading = false
            )
        }
        return ReasoningComparison(task, results)
    }

    override fun sendMessageStreaming(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings
    ): Flow<StreamChunk> = flow {
        emit(StreamChunk.Content("Test "))
        emit(StreamChunk.Content("streaming "))
        emit(StreamChunk.Done)
    }
}
