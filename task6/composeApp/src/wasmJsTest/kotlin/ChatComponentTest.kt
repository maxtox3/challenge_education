import chat.ChatIntent
import chat.ChatState
import chat.DefaultChatComponent
import chat.DefaultChatComponentFactory
import chat.SendMessageResult
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import model.ChatMessage
import model.ConstraintsInfo
import model.MetricRecord
import model.StreamChunk
import settings.ApiSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatComponentTest {
    private val fakeRepository = FakeChatRepositoryForComponent()
    private val componentFactory = DefaultChatComponentFactory(DefaultStoreFactory())

    private fun createComponent(): DefaultChatComponent = componentFactory.create(fakeRepository)

    @Test
    fun testComponentInitialState() = runTest {
        val component = createComponent()
        val state = component.state.value

        assertEquals("", state.inputText)
        assertEquals(emptyList<ChatMessage>(), state.messages)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertFalse(state.showSettings)
        assertFalse(state.showMetrics)
        assertFalse(state.showReasoning)
    }

    @Test
    fun testComponentAcceptIntent() = runTest {
        val component = createComponent()

        component.accept(ChatIntent.UpdateInputText("test message"))

        val state = component.state.value
        assertEquals("test message", state.inputText)
    }

    @Test
    fun testComponentStateUpdates() = runTest {
        val component = createComponent()

        component.accept(ChatIntent.UpdateInputText("First"))
        assertEquals("First", component.state.value.inputText)

        component.accept(ChatIntent.UpdateInputText("Second"))
        assertEquals("Second", component.state.value.inputText)
    }

    @Test
    fun testComponentToggleSettings() = runTest {
        val component = createComponent()

        component.accept(ChatIntent.ToggleSettings(true))
        assertTrue(component.state.value.showSettings)

        component.accept(ChatIntent.ToggleSettings(false))
        assertFalse(component.state.value.showSettings)
    }

    @Test
    fun testComponentToggleMetrics() = runTest {
        val component = createComponent()

        component.accept(ChatIntent.ToggleMetrics(true))
        assertTrue(component.state.value.showMetrics)

        component.accept(ChatIntent.ToggleMetrics(false))
        assertFalse(component.state.value.showMetrics)
    }

    @Test
    fun testComponentToggleReasoning() = runTest {
        val component = createComponent()

        component.accept(ChatIntent.ToggleReasoning(true))
        assertTrue(component.state.value.showReasoning)

        component.accept(ChatIntent.ToggleReasoning(false))
        assertFalse(component.state.value.showReasoning)
    }

    @Test
    fun testComponentUpdateSettings() = runTest {
        val component = createComponent()
        val newSettings = ApiSettings(
            apiKey = "test-key",
            model = "glm-4"
        )

        component.accept(ChatIntent.UpdateSettings(newSettings))

        val state = component.state.value
        assertEquals("test-key", state.settings.apiKey)
        assertEquals("glm-4", state.settings.model)
    }

    @Test
    fun testComponentSetError() = runTest {
        val component = createComponent()

        component.accept(ChatIntent.SetError("Test error"))

        val state = component.state.value
        assertEquals("Test error", state.errorMessage)
    }

    @Test
    fun testComponentClearError() = runTest {
        val component = createComponent()
        component.accept(ChatIntent.SetError("Test error"))

        component.accept(ChatIntent.ClearError)

        val state = component.state.value
        assertNull(state.errorMessage)
    }

    @Test
    fun testComponentSetLoading() = runTest {
        val component = createComponent()

        component.accept(ChatIntent.SetLoading(true))
        assertTrue(component.state.value.isLoading)

        component.accept(ChatIntent.SetLoading(false))
        assertFalse(component.state.value.isLoading)
    }

    @Test
    fun testComponentClearChat() = runTest {
        val component = createComponent()
        component.accept(ChatIntent.UpdateInputText("test"))
        component.accept(ChatIntent.SetError("error"))

        component.accept(ChatIntent.ClearChat)

        val state = component.state.value
        assertEquals(emptyList<ChatMessage>(), state.messages)
        assertEquals(emptyList<MetricRecord>(), state.metrics)
        assertEquals(0, state.metricCounter)
        assertNull(state.errorMessage)
    }

    @Test
    fun testComponentMessageSent() = runTest {
        val component = createComponent()
        val message = ChatMessage(role = "assistant", content = "Test response")
        val metric = createTestMetric()

        component.accept(ChatIntent.MessageSent(message, metric))

        val state = component.state.value
        assertEquals(1, state.messages.size)
        assertEquals("Test response", state.messages.first().content)
        assertEquals(1, state.metrics.size)
        assertEquals(1, state.metricCounter)
    }

    @Test
    fun testComponentSendMessageSuccess() = runTest {
        val component = createComponent()
        fakeRepository.sendMessageResult = SendMessageResult.Success(
            response = ChatMessage(role = "assistant", content = "Hello!"),
            metric = createTestMetric()
        )
        component.accept(ChatIntent.UpdateInputText("Hello"))

        component.accept(ChatIntent.SendMessage)

        val state = component.state.value
        assertTrue(state.messages.isNotEmpty())
        assertEquals("", state.inputText)
        assertFalse(state.isLoading)
    }

    @Test
    fun testComponentSendMessageError() = runTest {
        val component = createComponent()
        fakeRepository.sendMessageResult = SendMessageResult.Error("API Error")
        component.accept(ChatIntent.UpdateInputText("Hello"))

        component.accept(ChatIntent.SendMessage)

        val state = component.state.value
        assertFalse(state.isLoading)
        assertEquals("API Error", state.errorMessage)
    }

    @Test
    fun testComponentMultipleIntentsInSequence() = runTest {
        val component = createComponent()

        component.accept(ChatIntent.UpdateInputText("First"))
        assertEquals("First", component.state.value.inputText)

        component.accept(ChatIntent.UpdateInputText("Second"))
        assertEquals("Second", component.state.value.inputText)

        component.accept(ChatIntent.ToggleSettings(true))
        assertTrue(component.state.value.showSettings)

        component.accept(ChatIntent.SetError("Error"))
        assertEquals("Error", component.state.value.errorMessage)

        component.accept(ChatIntent.ClearError)
        assertNull(component.state.value.errorMessage)
    }

    @Test
    fun testComponentStateValueObservable() = runTest {
        val component = createComponent()
        val states = mutableListOf<ChatState>()

        states.add(component.state.value)

        component.accept(ChatIntent.UpdateInputText("test"))
        states.add(component.state.value)

        assertEquals(2, states.size)
        assertEquals("", states[0].inputText)
        assertEquals("test", states[1].inputText)
    }

    @Test
    fun testComponentDispose() = runTest {
        val component = createComponent()

        component.accept(ChatIntent.UpdateInputText("test"))

        component.dispose()
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

class FakeChatRepositoryForComponent : chat.ChatRepository {
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
        onProgress: (model.ReasoningComparison) -> Unit
    ): model.ReasoningComparison {
        val results = model.ReasoningMode.entries.associateWith { mode ->
            model.ReasoningResult(
                mode = mode,
                systemPrompt = "",
                actualPrompt = task,
                response = "Test response",
                isLoading = false
            )
        }
        return model.ReasoningComparison(task, results)
    }

    override fun sendMessageStreaming(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings
    ): kotlinx.coroutines.flow.Flow<StreamChunk> = kotlinx.coroutines.flow.flow {
        emit(StreamChunk.Content("Test "))
        emit(StreamChunk.Content("streaming "))
        emit(StreamChunk.Done)
    }
}
