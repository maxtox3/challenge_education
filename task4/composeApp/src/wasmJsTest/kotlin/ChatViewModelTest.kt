import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChatViewModelTest {
    private lateinit var fakeRepository: FakeChatRepository
    private lateinit var viewModel: ChatViewModel
    private lateinit var listState: LazyListState
    private lateinit var testScope: CoroutineScope

    @BeforeTest
    fun setup() {
        fakeRepository = FakeChatRepository()
        listState = LazyListState()
        testScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        viewModel = ChatViewModel(
            repository = fakeRepository,
            viewModelScope = testScope,
            listState = listState,
        )
    }

    @Test
    fun testInitialState() {
        assertEquals("", viewModel.state.inputText)
        assertEquals(emptyList<ChatMessage>(), viewModel.state.messages)
        assertFalse(viewModel.state.isLoading)
        assertNull(viewModel.state.errorMessage)
        assertFalse(viewModel.state.showSettings)
        assertFalse(viewModel.state.showMetrics)
        assertFalse(viewModel.state.showReasoning)
        assertEquals(emptyList<MetricRecord>(), viewModel.state.metrics)
        assertEquals(0, viewModel.state.metricCounter)
        assertFalse(viewModel.state.isReasoningLoading)
    }

    @Test
    fun testProcessIntent_UpdateInputText() {
        viewModel.processIntent(ChatIntent.UpdateInputText("Hello"))
        assertEquals("Hello", viewModel.state.inputText)
    }

    @Test
    fun testProcessIntent_ToggleSettings_Show() {
        viewModel.processIntent(ChatIntent.ToggleSettings(true))
        assertTrue(viewModel.state.showSettings)
    }

    @Test
    fun testProcessIntent_ToggleSettings_Hide() {
        viewModel.processIntent(ChatIntent.ToggleSettings(true))
        viewModel.processIntent(ChatIntent.ToggleSettings(false))
        assertFalse(viewModel.state.showSettings)
    }

    @Test
    fun testProcessIntent_ToggleMetrics_Show() {
        viewModel.processIntent(ChatIntent.ToggleMetrics(true))
        assertTrue(viewModel.state.showMetrics)
    }

    @Test
    fun testProcessIntent_ToggleMetrics_Hide() {
        viewModel.processIntent(ChatIntent.ToggleMetrics(true))
        viewModel.processIntent(ChatIntent.ToggleMetrics(false))
        assertFalse(viewModel.state.showMetrics)
    }

    @Test
    fun testProcessIntent_ToggleReasoning_Show() {
        viewModel.processIntent(ChatIntent.ToggleReasoning(true))
        assertTrue(viewModel.state.showReasoning)
    }

    @Test
    fun testProcessIntent_ToggleReasoning_Hide() {
        viewModel.processIntent(ChatIntent.ToggleReasoning(true))
        viewModel.processIntent(ChatIntent.ToggleReasoning(false))
        assertFalse(viewModel.state.showReasoning)
    }

    @Test
    fun testProcessIntent_SetError() {
        viewModel.processIntent(ChatIntent.SetError("Test error"))
        assertEquals("Test error", viewModel.state.errorMessage)
    }

    @Test
    fun testProcessIntent_ClearError() {
        viewModel.processIntent(ChatIntent.SetError("Test error"))
        viewModel.processIntent(ChatIntent.ClearError)
        assertNull(viewModel.state.errorMessage)
    }

    @Test
    fun testProcessIntent_SetLoading_True() {
        viewModel.processIntent(ChatIntent.SetLoading(true))
        assertTrue(viewModel.state.isLoading)
    }

    @Test
    fun testProcessIntent_SetLoading_False() {
        viewModel.processIntent(ChatIntent.SetLoading(true))
        viewModel.processIntent(ChatIntent.SetLoading(false))
        assertFalse(viewModel.state.isLoading)
    }

    @Test
    fun testProcessIntent_SetReasoningLoading_True() {
        viewModel.processIntent(ChatIntent.SetReasoningLoading(true))
        assertTrue(viewModel.state.isReasoningLoading)
    }

    @Test
    fun testProcessIntent_SetReasoningLoading_False() {
        viewModel.processIntent(ChatIntent.SetReasoningLoading(true))
        viewModel.processIntent(ChatIntent.SetReasoningLoading(false))
        assertFalse(viewModel.state.isReasoningLoading)
    }

    @Test
    fun testProcessIntent_MessageSent() {
        val response = ChatMessage(role = "assistant", content = "Response")
        val metric = createTestMetric()
        viewModel.processIntent(ChatIntent.MessageSent(response, metric))
        assertEquals(1, viewModel.state.messages.size)
        assertEquals(response, viewModel.state.messages.first())
        assertEquals(1, viewModel.state.metrics.size)
        assertEquals(1, viewModel.state.metrics.first().id)
    }

    @Test
    fun testProcessIntent_MessageSendFailed() {
        viewModel.processIntent(ChatIntent.SetLoading(true))
        viewModel.processIntent(ChatIntent.MessageSendFailed)
        assertFalse(viewModel.state.isLoading)
    }

    @Test
    fun testProcessIntent_UpdateReasoningComparison() {
        val comparison = ReasoningComparison(
            task = "Test task",
            results = mapOf(
                ReasoningMode.DIRECT to ReasoningResult(
                    mode = ReasoningMode.DIRECT,
                    systemPrompt = "",
                    actualPrompt = "Test task",
                    response = "Test response",
                ),
            ),
        )
        viewModel.processIntent(ChatIntent.UpdateReasoningComparison(comparison))
        assertEquals(comparison, viewModel.state.reasoningComparison)
    }

    @Test
    fun testClearChat() {
        viewModel.inputText = "Test"
        viewModel.processIntent(ChatIntent.SetError("Error"))
        viewModel.processIntent(
            ChatIntent.MessageSent(
                ChatMessage(role = "user", content = "Test"),
                createTestMetric()
            )
        )
        viewModel.clearChat()
        assertEquals(emptyList<ChatMessage>(), viewModel.state.messages)
        assertNull(viewModel.state.errorMessage)
    }

    @Test
    fun testUpdateSettings() {
        val newSettings = ApiSettings(
            apiKey = "new-key",
            model = "new-model",
            maxTokens = 500,
            temperature = 0.7,
        )
        viewModel.processIntent(ChatIntent.ToggleSettings(true))
        viewModel.updateSettings(newSettings)
        assertEquals(newSettings, viewModel.state.settings)
        assertFalse(viewModel.state.showSettings)
    }

    @Test
    fun testUpdateSettingsViaProcessIntent() {
        val newSettings = ApiSettings(
            apiKey = "test-key",
            model = "test-model",
        )
        viewModel.processIntent(ChatIntent.UpdateSettings(newSettings))
        assertEquals(newSettings, viewModel.state.settings)
        assertFalse(viewModel.state.showSettings)
    }

    @Test
    fun testSendMessage_WithEmptyInput() {
        viewModel.inputText = ""
        viewModel.sendMessage()
        assertFalse(viewModel.state.isLoading)
        assertEquals(0, viewModel.state.messages.size)
    }

    @Test
    fun testSendMessage_WithBlankInput() {
        viewModel.inputText = "   "
        viewModel.sendMessage()
        assertFalse(viewModel.state.isLoading)
        assertEquals(0, viewModel.state.messages.size)
    }

    @Test
    fun testSendMessage_WhileLoading() {
        viewModel.processIntent(ChatIntent.SetLoading(true))
        viewModel.inputText = "Hello"
        val initialMessageCount = viewModel.state.messages.size
        viewModel.sendMessage()
        assertEquals(initialMessageCount, viewModel.state.messages.size)
    }

    @Test
    fun testSendMessage_ValidInput_StartsLoading() {
        fakeRepository.sendResult = SendMessageResult.Success(
            response = ChatMessage(role = "assistant", content = "Test response"),
            metric = createTestMetric(),
        )
        viewModel.inputText = "Hello"
        viewModel.sendMessage()
        assertTrue(viewModel.state.isLoading)
        assertEquals("", viewModel.state.inputText)
        assertNull(viewModel.state.errorMessage)
        assertEquals(1, viewModel.state.messages.size)
        assertEquals("user", viewModel.state.messages[0].role)
        assertEquals("Hello", viewModel.state.messages[0].content)
    }

    @Test
    fun testSendMessage_WithApiError_StartsLoading() {
        fakeRepository.sendResult = SendMessageResult.Error("API Error")
        viewModel.inputText = "Hello"
        viewModel.sendMessage()
        assertEquals("", viewModel.state.inputText)
        assertTrue(viewModel.state.isLoading)
    }

    @Test
    fun testSendMessage_ClearsInputText() {
        fakeRepository.sendResult = SendMessageResult.Success(
            response = ChatMessage(role = "assistant", content = "Response"),
            metric = createTestMetric(),
        )
        viewModel.inputText = "Hello"
        viewModel.sendMessage()
        assertEquals("", viewModel.state.inputText)
    }

    @Test
    fun testSendMessage_ClearsErrorMessage() {
        viewModel.processIntent(ChatIntent.SetError("Previous error"))
        fakeRepository.sendResult = SendMessageResult.Success(
            response = ChatMessage(role = "assistant", content = "Response"),
            metric = createTestMetric(),
        )
        viewModel.inputText = "Hello"
        viewModel.sendMessage()
        assertNull(viewModel.state.errorMessage)
    }

    @Test
    fun testRunReasoningComparison_SetsLoading() {
        fakeRepository.reasoningComparisonResult = ReasoningComparison(
            task = "Test task",
            results = emptyMap(),
        )
        viewModel.runReasoningComparison("Test task")
        assertTrue(viewModel.state.isReasoningLoading)
    }

    @Test
    fun testTypedProp_InputText() {
        viewModel.inputText = "Test input"
        assertEquals("Test input", viewModel.state.inputText)
        assertEquals("Test input", viewModel.uiState.value.inputText)
    }

    @Test
    fun testTypedProp_Messages() {
        val message = ChatMessage(role = "user", content = "Test")
        viewModel.messages = listOf(message)
        assertEquals(listOf(message), viewModel.state.messages)
        assertEquals(listOf(message), viewModel.uiState.value.messages)
    }

    @Test
    fun testTypedProp_IsLoading() {
        viewModel.isLoading = true
        assertTrue(viewModel.state.isLoading)
        assertTrue(viewModel.uiState.value.isLoading)
        viewModel.isLoading = false
        assertFalse(viewModel.state.isLoading)
    }

    @Test
    fun testTypedProp_ErrorMessage() {
        viewModel.errorMessage = "Error"
        assertEquals("Error", viewModel.state.errorMessage)
        assertEquals("Error", viewModel.uiState.value.errorMessage)
        viewModel.errorMessage = null
        assertNull(viewModel.state.errorMessage)
    }

    @Test
    fun testTypedProp_ShowSettings() {
        viewModel.showSettings = true
        assertTrue(viewModel.state.showSettings)
        viewModel.showSettings = false
        assertFalse(viewModel.state.showSettings)
    }

    @Test
    fun testTypedProp_ShowMetrics() {
        viewModel.showMetrics = true
        assertTrue(viewModel.state.showMetrics)
        viewModel.showMetrics = false
        assertFalse(viewModel.state.showMetrics)
    }

    @Test
    fun testTypedProp_ShowReasoning() {
        viewModel.showReasoning = true
        assertTrue(viewModel.state.showReasoning)
        viewModel.showReasoning = false
        assertFalse(viewModel.state.showReasoning)
    }

    @Test
    fun testTypedProp_Metrics() {
        val metric = createTestMetric()
        viewModel.metrics = listOf(metric)
        assertEquals(listOf(metric), viewModel.state.metrics)
    }

    @Test
    fun testTypedProp_MetricCounter() {
        viewModel.metricCounter = 5
        assertEquals(5, viewModel.state.metricCounter)
    }

    @Test
    fun testTypedProp_Settings() {
        val settings = ApiSettings(apiKey = "test", model = "test-model")
        viewModel.settings = settings
        assertEquals(settings, viewModel.state.settings)
    }

    @Test
    fun testTypedProp_ReasoningComparison() {
        val comparison = ReasoningComparison(
            task = "Task",
            results = emptyMap(),
        )
        viewModel.reasoningComparison = comparison
        assertEquals(comparison, viewModel.state.reasoningComparison)
    }

    @Test
    fun testTypedProp_IsReasoningLoading() {
        viewModel.isReasoningLoading = true
        assertTrue(viewModel.state.isReasoningLoading)
        viewModel.isReasoningLoading = false
        assertFalse(viewModel.state.isReasoningLoading)
    }

    @Test
    fun testState_ReflectsCurrentState() {
        viewModel.inputText = "Current text"
        assertEquals(viewModel.uiState.value.inputText, viewModel.state.inputText)
        viewModel.processIntent(ChatIntent.SetError("Error"))
        assertEquals(viewModel.uiState.value.errorMessage, viewModel.state.errorMessage)
    }

    @Test
    fun testClearChat_PreservesSettings() {
        val settings = ApiSettings(apiKey = "preserved-key", model = "preserved-model")
        viewModel.settings = settings
        viewModel.inputText = "Test"
        viewModel.processIntent(
            ChatIntent.MessageSent(
                ChatMessage(role = "user", content = "Test"),
                createTestMetric()
            )
        )
        viewModel.clearChat()
        assertEquals(settings, viewModel.state.settings)
    }

    @Test
    fun testMultipleMessageSent_UpdatesMetricCounter() {
        val response1 = ChatMessage(role = "assistant", content = "Response 1")
        val response2 = ChatMessage(role = "assistant", content = "Response 2")
        val metric1 = createTestMetric()
        val metric2 = createTestMetric()
        viewModel.processIntent(ChatIntent.MessageSent(response1, metric1))
        assertEquals(1, viewModel.state.metricCounter)
        assertEquals(1, viewModel.state.metrics.last().id)
        viewModel.processIntent(ChatIntent.MessageSent(response2, metric2))
        assertEquals(2, viewModel.state.metricCounter)
        assertEquals(2, viewModel.state.metrics.last().id)
    }

    @Test
    fun testProcessIntent_ClearChat() {
        viewModel.inputText = "Test"
        viewModel.processIntent(
            ChatIntent.MessageSent(
                ChatMessage(role = "user", content = "Test"),
                createTestMetric()
            )
        )
        viewModel.processIntent(ChatIntent.ClearChat)
        assertEquals(emptyList<ChatMessage>(), viewModel.state.messages)
    }

    @Test
    fun testProcessIntent_AllIntentTypes() {
        viewModel.processIntent(ChatIntent.UpdateInputText("test"))
        assertEquals("test", viewModel.state.inputText)
        viewModel.processIntent(ChatIntent.ToggleSettings(true))
        assertTrue(viewModel.state.showSettings)
        viewModel.processIntent(ChatIntent.ToggleMetrics(true))
        assertTrue(viewModel.state.showMetrics)
        viewModel.processIntent(ChatIntent.ToggleReasoning(true))
        assertTrue(viewModel.state.showReasoning)
        viewModel.processIntent(ChatIntent.SetError("error"))
        assertEquals("error", viewModel.state.errorMessage)
        viewModel.processIntent(ChatIntent.ClearError)
        assertNull(viewModel.state.errorMessage)
        viewModel.processIntent(ChatIntent.SetLoading(true))
        assertTrue(viewModel.state.isLoading)
        viewModel.processIntent(ChatIntent.SetReasoningLoading(true))
        assertTrue(viewModel.state.isReasoningLoading)
    }

    @Test
    fun testSettings_DefaultValues() {
        assertEquals("", viewModel.state.settings.apiKey)
        assertEquals("glm-5", viewModel.state.settings.model)
        assertEquals(1.0, viewModel.state.settings.temperature)
        assertNull(viewModel.state.settings.maxTokens)
    }

    @Test
    fun testUpdateSettings_WithCustomValues() {
        val customSettings = ApiSettings(
            apiKey = "custom-api-key",
            model = "custom-model",
            maxTokens = 2000,
            temperature = 0.5,
            stopSequences = "stop1,stop2",
            responseFormat = "json"
        )
        viewModel.updateSettings(customSettings)
        assertEquals("custom-api-key", viewModel.state.settings.apiKey)
        assertEquals("custom-model", viewModel.state.settings.model)
        assertEquals(2000, viewModel.state.settings.maxTokens)
        assertEquals(0.5, viewModel.state.settings.temperature)
        assertEquals("stop1,stop2", viewModel.state.settings.stopSequences)
        assertEquals("json", viewModel.state.settings.responseFormat)
    }

    private fun createTestMetric(): MetricRecord = MetricRecord(
        id = 0,
        prompt = "Test prompt",
        response = "Test response",
        mode = "free",
        responseLength = 13,
        tokensUsed = 10,
        maxTokens = 100,
        finishReason = "stop",
        responseTimeMs = 100,
        constraints = ConstraintsInfo(null, emptyList(), "text", 1.0),
    )
}

class FakeChatRepository : ChatRepository {
    var sendResult: SendMessageResult = SendMessageResult.Success(
        response = ChatMessage(role = "assistant", content = "Default response"),
        metric = MetricRecord(
            id = 0,
            prompt = "",
            response = "",
            mode = "free",
            responseLength = 0,
            tokensUsed = null,
            maxTokens = null,
            finishReason = null,
            responseTimeMs = 0,
            constraints = ConstraintsInfo(null, emptyList(), "text", 1.0),
        ),
    )
    var reasoningComparisonResult: ReasoningComparison = ReasoningComparison("", emptyMap())
    var sendMessageCalled: Boolean = false

    override suspend fun sendMessage(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings,
    ): SendMessageResult {
        sendMessageCalled = true
        return sendResult
    }

    override suspend fun runReasoningComparison(
        task: String,
        settings: ApiSettings,
        onProgress: (ReasoningComparison) -> Unit,
    ): ReasoningComparison {
        onProgress(reasoningComparisonResult)
        return reasoningComparisonResult
    }

    override fun sendMessageStreaming(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings,
    ): Flow<StreamChunk> = flow {
        emit(StreamChunk.Content("Default "))
        emit(StreamChunk.Content("response"))
        emit(StreamChunk.Done)
    }
}
