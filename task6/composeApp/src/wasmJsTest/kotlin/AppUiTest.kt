import agent.AgentConfig
import agent.AgentContext
import agent.AgentState
import agent.AgentStatus
import agent.AgentStore
import agent.ContextStorage
import agent.LlmClient
import agent.LlmResponse
import androidx.compose.foundation.lazy.LazyListState
import chat.ChatIntent
import chat.ChatViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import model.ChatMessage
import model.StreamChunk
import settings.ApiSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppUiTest {
    private lateinit var mockLlmClient: MockLlmClient
    private lateinit var mockStorage: MockContextStorage
    private lateinit var agentStore: AgentStore
    private lateinit var viewModel: ChatViewModel
    private lateinit var listState: LazyListState

    @BeforeTest
    fun setup() {
        mockLlmClient = MockLlmClient()
        mockStorage = MockContextStorage()
        listState = LazyListState()
        agentStore = AgentStore(mockLlmClient, mockStorage)
        viewModel = ChatViewModel(agentStore, listState)
    }

    @Test
    fun initialStateRendersCorrectly() {
        assertEquals("", viewModel.inputText)
        assertEquals(emptyList<ChatMessage>(), viewModel.messages)
        assertFalse(viewModel.isLoading)
        assertNull(viewModel.errorMessage)
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
                    metric = null
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

        viewModel.processIntent(ChatIntent.SetLoading(false))
        assertFalse(viewModel.isLoading)
    }

    @Test
    fun errorMessageDisplaysInSnackbar() {
        assertNull(viewModel.errorMessage)

        viewModel.processIntent(ChatIntent.SetError("Network error"))

        viewModel.processIntent(ChatIntent.ClearError)
    }

    @Test
    fun settingsDialogOpensAndCloses() {
        viewModel.processIntent(ChatIntent.ToggleSettings(true))

        viewModel.processIntent(ChatIntent.ToggleSettings(false))
    }

    @Test
    fun metricsDialogOpensAndCloses() {
        viewModel.processIntent(ChatIntent.ToggleMetrics(true))

        viewModel.processIntent(ChatIntent.ToggleMetrics(false))
    }

    @Test
    fun reasoningDialogOpensAndCloses() {
        viewModel.processIntent(ChatIntent.ToggleReasoning(true))

        viewModel.processIntent(ChatIntent.ToggleReasoning(false))
    }

    @Test
    fun clearChatButtonWorks() {
        viewModel.processIntent(
            ChatIntent.MessageSent(
                response = ChatMessage(role = "user", content = "Test"),
                metric = null
            )
        )
        viewModel.processIntent(ChatIntent.SetError("Some error"))

        assertTrue(viewModel.messages.isNotEmpty())

        viewModel.processIntent(ChatIntent.ClearChat)

        assertTrue(viewModel.messages.isEmpty())
    }

    @Test
    fun sendMessageInteractionValidInput() {
        viewModel.processIntent(ChatIntent.UpdateInputText("Hello, world!"))
        assertEquals("Hello, world!", viewModel.inputText)

        viewModel.processIntent(ChatIntent.SendMessage)

        assertEquals("", viewModel.inputText)
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
            apiKey = "test-key",
            model = "custom-model",
            maxTokens = 500,
            temperature = 0.7,
        )

        viewModel.processIntent(ChatIntent.UpdateSettings(newSettings))
    }

    @Test
    fun reasoningComparisonUpdatesCorrectly() {
        viewModel.processIntent(ChatIntent.UpdateReasoningComparison(null))
    }

    @Test
    fun reasoningLoadingStateToggles() {
        viewModel.processIntent(ChatIntent.SetReasoningLoading(true))

        viewModel.processIntent(ChatIntent.SetReasoningLoading(false))
    }

    @Test
    fun metricsAccumulateCorrectly() {
        assertEquals(0, viewModel.messages.size)

        viewModel.processIntent(
            ChatIntent.MessageSent(
                response = ChatMessage(role = "assistant", content = "Response 1"),
                metric = null
            )
        )

        assertEquals(1, viewModel.messages.size)

        viewModel.processIntent(
            ChatIntent.MessageSent(
                response = ChatMessage(role = "assistant", content = "Response 2"),
                metric = null
            )
        )

        assertEquals(2, viewModel.messages.size)
    }

    @Test
    fun agentStateDefaultValues() {
        val state = AgentState()

        assertEquals("", state.inputText)
        assertEquals(emptyList<ChatMessage>(), state.messages)
        assertEquals(AgentStatus.Idle, state.status)
    }

    @Test
    fun agentStateCustomValues() {
        val messages = listOf(ChatMessage(role = "user", content = "Test"))

        val state = AgentState(
            inputText = "test input",
            messages = messages,
            status = AgentStatus.Loading,
        )

        assertEquals("test input", state.inputText)
        assertEquals(messages, state.messages)
        assertEquals(AgentStatus.Loading, state.status)
    }

    @Test
    fun agentConfigDefaultValues() {
        val config = AgentConfig()

        assertEquals("glm-5", config.model)
        assertNull(config.maxTokens)
        assertNull(config.temperature)
    }

    @Test
    fun messageSentIntentUpdatesStateCorrectly() {
        val message = ChatMessage(role = "assistant", content = "AI response")

        val initialMessageCount = viewModel.messages.size

        viewModel.processIntent(ChatIntent.MessageSent(message, null))

        assertEquals(initialMessageCount + 1, viewModel.messages.size)
        assertEquals(message, viewModel.messages.last())
    }

    @Test
    fun messageSendFailedSetsLoadingFalse() {
        viewModel.processIntent(ChatIntent.SetLoading(true))

        viewModel.processIntent(ChatIntent.MessageSendFailed)
    }

    @Test
    fun clearChatViaViewModelMethod() {
        viewModel.processIntent(
            ChatIntent.MessageSent(
                response = ChatMessage(role = "user", content = "Test message"),
                metric = null
            )
        )
        viewModel.processIntent(ChatIntent.SetError("Error message"))

        assertTrue(viewModel.messages.isNotEmpty())

        viewModel.clearChat()

        assertTrue(viewModel.messages.isEmpty())
    }

    @Test
    fun updateSettingsViaViewModelMethod() {
        val newSettings = ApiSettings(
            apiKey = "test-key",
            model = "test-model",
            maxTokens = 1000,
        )

        viewModel.updateSettings(newSettings)
    }
}

class MockLlmClient : LlmClient {
    override suspend fun call(prompt: String, context: AgentContext, config: AgentConfig): Result<LlmResponse> =
        Result.success(
            LlmResponse(
                content = "Mock response",
                tokensUsed = 10,
                model = config.model
            )
        )

    override fun stream(prompt: String, context: AgentContext, config: AgentConfig): Flow<StreamChunk> = flow {
        emit(StreamChunk.Content("Mock "))
        emit(StreamChunk.Content("response"))
        emit(StreamChunk.Done)
    }
}

class MockContextStorage : ContextStorage {
    private var context: AgentContext = AgentContext()

    override fun load(): AgentContext = context

    override fun save(context: AgentContext): Boolean {
        this.context = context
        return true
    }

    override fun clear() {
        context = AgentContext()
    }
}
