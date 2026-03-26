import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import model.ChatMessage
import model.ConstraintsInfo
import model.MetricRecord
import model.ReasoningComparison
import model.ReasoningMode
import model.ReasoningResult
import model.StreamChunk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppUiTest {
    private lateinit var mockRepository: MockChatRepository
    private lateinit var viewModel: ChatViewModel
    private lateinit var listState: LazyListState

    @BeforeTest
    fun setup() {
        mockRepository = MockChatRepository()
        listState = LazyListState()
        viewModel = ChatViewModel(
            repository = mockRepository,
            viewModelScope = CoroutineScope(Dispatchers.Default),
            listState = listState,
        )
    }

    @Test
    fun initialStateRendersCorrectly() {
        val state = viewModel.state

        assertEquals("", state.inputText)
        assertEquals(emptyList<ChatMessage>(), state.messages)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertFalse(state.showSettings)
        assertFalse(state.showMetrics)
        assertFalse(state.showReasoning)
        assertEquals(emptyList<MetricRecord>(), state.metrics)
        assertEquals(0, state.metricCounter)
        assertEquals("glm-5", state.settings.model)
    }

    @Test
    fun messageListDisplaysAllMessages() {
        val messages = listOf(
            ChatMessage(role = "user", content = "Hello"),
            ChatMessage(role = "assistant", content = "Hi there!"),
            ChatMessage(role = "user", content = "How are you?"),
        )

        messages.forEach { msg ->
            viewModel.processIntent(
                ChatIntent.MessageSent(
                    response = msg,
                    metric = MetricRecord(
                        id = 0,
                        prompt = msg.content,
                        response = msg.content,
                        mode = "free",
                        responseLength = msg.content.length,
                        tokensUsed = 10,
                        maxTokens = null,
                        finishReason = "stop",
                        responseTimeMs = 100,
                        constraints = ConstraintsInfo(null, emptyList(), "text", 1.0),
                    ),
                )
            )
        }

        assertEquals(3, viewModel.messages.size)
        assertEquals("Hello", viewModel.messages[0].content)
        assertEquals("Hi there!", viewModel.messages[1].content)
        assertEquals("How are you?", viewModel.messages[2].content)
    }

    @Test
    fun loadingStateShowsIndicator() {
        assertFalse(viewModel.isLoading)

        viewModel.processIntent(ChatIntent.SetLoading(true))
        assertTrue(viewModel.isLoading)

        viewModel.processIntent(ChatIntent.SetLoading(false))
        assertFalse(viewModel.isLoading)
    }

    @Test
    fun errorMessageDisplaysInSnackbar() {
        assertNull(viewModel.errorMessage)

        viewModel.processIntent(ChatIntent.SetError("Network error"))
        assertEquals("Network error", viewModel.errorMessage)

        viewModel.processIntent(ChatIntent.ClearError)
        assertNull(viewModel.errorMessage)
    }

    @Test
    fun settingsDialogOpensAndCloses() {
        assertFalse(viewModel.showSettings)

        viewModel.processIntent(ChatIntent.ToggleSettings(true))
        assertTrue(viewModel.showSettings)

        viewModel.processIntent(ChatIntent.ToggleSettings(false))
        assertFalse(viewModel.showSettings)
    }

    @Test
    fun metricsDialogOpensAndCloses() {
        assertFalse(viewModel.showMetrics)

        viewModel.processIntent(ChatIntent.ToggleMetrics(true))
        assertTrue(viewModel.showMetrics)

        viewModel.processIntent(ChatIntent.ToggleMetrics(false))
        assertFalse(viewModel.showMetrics)
    }

    @Test
    fun reasoningDialogOpensAndCloses() {
        assertFalse(viewModel.showReasoning)

        viewModel.processIntent(ChatIntent.ToggleReasoning(true))
        assertTrue(viewModel.showReasoning)

        viewModel.processIntent(ChatIntent.ToggleReasoning(false))
        assertFalse(viewModel.showReasoning)
    }

    @Test
    fun clearChatButtonWorks() {
        viewModel.processIntent(
            ChatIntent.MessageSent(
                response = ChatMessage(role = "user", content = "Test"),
                metric = MetricRecord(
                    id = 1,
                    prompt = "Test",
                    response = "Test",
                    mode = "free",
                    responseLength = 4,
                    tokensUsed = 5,
                    maxTokens = null,
                    finishReason = "stop",
                    responseTimeMs = 50,
                    constraints = ConstraintsInfo(null, emptyList(), "text", 1.0),
                ),
            )
        )
        viewModel.processIntent(ChatIntent.SetError("Some error"))

        assertTrue(viewModel.messages.isNotEmpty())
        assertNotNull(viewModel.errorMessage)

        viewModel.processIntent(ChatIntent.ClearChat)

        assertTrue(viewModel.messages.isEmpty())
        assertNull(viewModel.errorMessage)
    }

    @Test
    fun sendMessageInteractionValidInput() {
        viewModel.processIntent(ChatIntent.UpdateInputText("Hello, world!"))
        assertEquals("Hello, world!", viewModel.inputText)

        viewModel.processIntent(ChatIntent.SendMessage)

        assertEquals("", viewModel.inputText)
        assertTrue(viewModel.messages.any { it.content == "Hello, world!" && it.role == "user" })
    }

    @Test
    fun sendMessageInteractionEmptyInputDoesNotSend() {
        val initialMessageCount = viewModel.messages.size

        viewModel.processIntent(ChatIntent.UpdateInputText(""))
        viewModel.processIntent(ChatIntent.SendMessage)

        assertEquals(initialMessageCount, viewModel.messages.size)
    }

    @Test
    fun sendMessageInteractionWhitespaceOnlyDoesNotSend() {
        val initialMessageCount = viewModel.messages.size

        viewModel.processIntent(ChatIntent.UpdateInputText("   "))
        viewModel.processIntent(ChatIntent.SendMessage)

        assertEquals(initialMessageCount, viewModel.messages.size)
    }

    @Test
    fun inputFieldUpdatesOnTyping() {
        assertEquals("", viewModel.inputText)

        viewModel.processIntent(ChatIntent.UpdateInputText("H"))
        assertEquals("H", viewModel.inputText)

        viewModel.processIntent(ChatIntent.UpdateInputText("He"))
        assertEquals("He", viewModel.inputText)

        viewModel.processIntent(ChatIntent.UpdateInputText("Hello"))
        assertEquals("Hello", viewModel.inputText)
    }

    @Test
    fun updateSettingsSavesCorrectly() {
        val newSettings = ApiSettings(
            apiKey = "new-api-key",
            model = "custom-model",
            maxTokens = 500,
            temperature = 0.7,
            stopSequences = "stop1,stop2",
            responseFormat = "json",
        )

        viewModel.processIntent(ChatIntent.UpdateSettings(newSettings))

        assertEquals("new-api-key", viewModel.settings.apiKey)
        assertEquals("custom-model", viewModel.settings.model)
        assertEquals(500, viewModel.settings.maxTokens)
        assertEquals(0.7, viewModel.settings.temperature)
        assertEquals("stop1,stop2", viewModel.settings.stopSequences)
        assertEquals("json", viewModel.settings.responseFormat)
        assertFalse(viewModel.showSettings)
    }

    @Test
    fun reasoningComparisonUpdatesCorrectly() {
        val comparison = ReasoningComparison(
            task = "Test task",
            results = mapOf(
                ReasoningMode.DIRECT to ReasoningResult(
                    mode = ReasoningMode.DIRECT,
                    systemPrompt = "Test prompt",
                    actualPrompt = "Test actual",
                    response = "Test response",
                    responseTimeMs = 100,
                    tokensUsed = 50,
                ),
            ),
        )

        viewModel.processIntent(ChatIntent.UpdateReasoningComparison(comparison))

        assertEquals("Test task", viewModel.reasoningComparison.task)
        assertTrue(viewModel.reasoningComparison.results.containsKey(ReasoningMode.DIRECT))
    }

    @Test
    fun reasoningLoadingStateToggles() {
        assertFalse(viewModel.isReasoningLoading)

        viewModel.processIntent(ChatIntent.SetReasoningLoading(true))
        assertTrue(viewModel.isReasoningLoading)

        viewModel.processIntent(ChatIntent.SetReasoningLoading(false))
        assertFalse(viewModel.isReasoningLoading)
    }

    @Test
    fun metricsAccumulateCorrectly() {
        assertEquals(0, viewModel.metrics.size)
        assertEquals(0, viewModel.metricCounter)

        viewModel.processIntent(
            ChatIntent.MessageSent(
                response = ChatMessage(role = "assistant", content = "Response 1"),
                metric = MetricRecord(
                    id = 0,
                    prompt = "Prompt 1",
                    response = "Response 1",
                    mode = "free",
                    responseLength = 10,
                    tokensUsed = 5,
                    maxTokens = null,
                    finishReason = "stop",
                    responseTimeMs = 100,
                    constraints = ConstraintsInfo(null, emptyList(), "text", 1.0),
                ),
            )
        )

        assertEquals(1, viewModel.metrics.size)
        assertEquals(1, viewModel.metricCounter)

        viewModel.processIntent(
            ChatIntent.MessageSent(
                response = ChatMessage(role = "assistant", content = "Response 2"),
                metric = MetricRecord(
                    id = 0,
                    prompt = "Prompt 2",
                    response = "Response 2",
                    mode = "free",
                    responseLength = 10,
                    tokensUsed = 5,
                    maxTokens = null,
                    finishReason = "stop",
                    responseTimeMs = 100,
                    constraints = ConstraintsInfo(null, emptyList(), "text", 1.0),
                ),
            )
        )

        assertEquals(2, viewModel.metrics.size)
        assertEquals(2, viewModel.metricCounter)
    }

    @Test
    fun chatStateDefaultValues() {
        val state = ChatState()

        assertEquals("", state.inputText)
        assertEquals(emptyList<ChatMessage>(), state.messages)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertFalse(state.showSettings)
        assertFalse(state.showMetrics)
        assertFalse(state.showReasoning)
        assertEquals(emptyList<MetricRecord>(), state.metrics)
        assertEquals(0, state.metricCounter)
        assertFalse(state.isReasoningLoading)
    }

    @Test
    fun chatStateCustomValues() {
        val messages = listOf(ChatMessage(role = "user", content = "Test"))
        val metrics = listOf(
            MetricRecord(
                id = 1,
                prompt = "Test",
                response = "Response",
                mode = "free",
                responseLength = 8,
                tokensUsed = 10,
                maxTokens = 100,
                finishReason = "stop",
                responseTimeMs = 200,
                constraints = ConstraintsInfo(100, emptyList(), "text", 1.0),
            ),
        )

        val state = ChatState(
            inputText = "test input",
            messages = messages,
            isLoading = true,
            errorMessage = "Error",
            showSettings = true,
            showMetrics = true,
            showReasoning = true,
            metrics = metrics,
            metricCounter = 5,
            isReasoningLoading = true,
        )

        assertEquals("test input", state.inputText)
        assertEquals(messages, state.messages)
        assertTrue(state.isLoading)
        assertEquals("Error", state.errorMessage)
        assertTrue(state.showSettings)
        assertTrue(state.showMetrics)
        assertTrue(state.showReasoning)
        assertEquals(metrics, state.metrics)
        assertEquals(5, state.metricCounter)
        assertTrue(state.isReasoningLoading)
    }

    @Test
    fun apiSettingsDefaultValues() {
        val settings = ApiSettings()

        assertEquals("", settings.apiKey)
        assertEquals("glm-5", settings.model)
        assertNull(settings.maxTokens)
        assertEquals(1.0, settings.temperature)
        assertEquals("", settings.stopSequences)
        assertEquals("text", settings.responseFormat)
    }

    @Test
    fun messageSentIntentUpdatesStateCorrectly() {
        val message = ChatMessage(role = "assistant", content = "AI response")
        val metric = MetricRecord(
            id = 0,
            prompt = "User prompt",
            response = "AI response",
            mode = "free",
            responseLength = 11,
            tokensUsed = 25,
            maxTokens = null,
            finishReason = "stop",
            responseTimeMs = 500,
            constraints = ConstraintsInfo(null, emptyList(), "text", 1.0),
        )

        val initialMessageCount = viewModel.messages.size
        val initialMetricCount = viewModel.metrics.size

        viewModel.processIntent(ChatIntent.MessageSent(message, metric))

        assertEquals(initialMessageCount + 1, viewModel.messages.size)
        assertEquals(initialMetricCount + 1, viewModel.metrics.size)
        assertEquals(message, viewModel.messages.last())
    }

    @Test
    fun messageSendFailedSetsLoadingFalse() {
        viewModel.processIntent(ChatIntent.SetLoading(true))
        assertTrue(viewModel.isLoading)

        viewModel.processIntent(ChatIntent.MessageSendFailed)
        assertFalse(viewModel.isLoading)
    }

    @Test
    fun clearChatViaViewModelMethod() {
        viewModel.processIntent(
            ChatIntent.MessageSent(
                response = ChatMessage(role = "user", content = "Test message"),
                metric = MetricRecord(
                    id = 1,
                    prompt = "Test",
                    response = "Test",
                    mode = "free",
                    responseLength = 4,
                    tokensUsed = 5,
                    maxTokens = null,
                    finishReason = "stop",
                    responseTimeMs = 50,
                    constraints = ConstraintsInfo(null, emptyList(), "text", 1.0),
                ),
            )
        )
        viewModel.processIntent(ChatIntent.SetError("Error message"))

        assertTrue(viewModel.messages.isNotEmpty())
        assertNotNull(viewModel.errorMessage)

        viewModel.clearChat()

        assertTrue(viewModel.messages.isEmpty())
        assertNull(viewModel.errorMessage)
    }

    @Test
    fun updateSettingsViaViewModelMethod() {
        val newSettings = ApiSettings(
            apiKey = "test-key",
            model = "test-model",
            maxTokens = 1000,
        )

        viewModel.updateSettings(newSettings)

        assertEquals("test-key", viewModel.settings.apiKey)
        assertEquals("test-model", viewModel.settings.model)
        assertEquals(1000, viewModel.settings.maxTokens)
        assertFalse(viewModel.showSettings)
    }
}

class MockChatRepository : ChatRepository {

    override suspend fun sendMessage(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings,
    ): SendMessageResult = SendMessageResult.Success(
        response = ChatMessage(
            role = "assistant",
            content = "Mock response for: $prompt",
            tokensUsed = 10,
            maxTokens = settings.maxTokens,
            finishReason = "stop",
        ),
        metric = MetricRecord(
            id = 0,
            prompt = prompt,
            response = "Mock response for: $prompt",
            mode = "free",
            responseLength = "Mock response for: $prompt".length,
            tokensUsed = 10,
            maxTokens = settings.maxTokens,
            finishReason = "stop",
            responseTimeMs = 100,
            constraints = ConstraintsInfo(
                maxTokens = settings.maxTokens,
                stopSequences = emptyList(),
                responseFormat = settings.responseFormat,
                temperature = settings.temperature,
            ),
        ),
    )

    override suspend fun runReasoningComparison(
        task: String,
        settings: ApiSettings,
        onProgress: (ReasoningComparison) -> Unit,
    ): ReasoningComparison {
        val results = ReasoningMode.entries.associateWith { mode ->
            ReasoningResult(
                mode = mode,
                systemPrompt = "Mock system prompt for $mode",
                actualPrompt = task,
                response = "Mock response for $mode",
                responseTimeMs = 100,
                tokensUsed = 50,
            )
        }
        val comparison = ReasoningComparison(task = task, results = results)
        onProgress(comparison)
        return comparison
    }

    override fun sendMessageStreaming(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings,
    ): Flow<StreamChunk> = flow {
        emit(StreamChunk.Content("Mock "))
        emit(StreamChunk.Content("response "))
        emit(StreamChunk.Content("for: $prompt"))
        emit(StreamChunk.Done)
    }
}
