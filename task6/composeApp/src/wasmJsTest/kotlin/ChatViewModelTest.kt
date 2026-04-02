import agent.AgentConfig
import agent.AgentContext
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

class ChatViewModelTest {
    private lateinit var fakeLlmClient: FakeLlmClient
    private lateinit var fakeStorage: FakeContextStorage
    private lateinit var agentStore: AgentStore
    private lateinit var viewModel: ChatViewModel
    private lateinit var listState: LazyListState

    @BeforeTest
    fun setup() {
        fakeLlmClient = FakeLlmClient()
        fakeStorage = FakeContextStorage()
        listState = LazyListState()
        agentStore = AgentStore(fakeLlmClient, fakeStorage)
        viewModel = ChatViewModel(agentStore, listState)
    }

    @Test
    fun testInitialState() {
        assertEquals("", viewModel.inputText)
        assertEquals(emptyList(), viewModel.messages)
        assertFalse(viewModel.isLoading)
        assertNull(viewModel.errorMessage)
    }

    @Test
    fun testProcessIntent_UpdateInputText() {
        viewModel.processIntent(ChatIntent.UpdateInputText("Hello"))
        assertEquals("Hello", viewModel.inputText)
    }

    @Test
    fun testProcessIntent_ToggleSettings_Show() {
        viewModel.processIntent(ChatIntent.ToggleSettings(true))
    }

    @Test
    fun testProcessIntent_ToggleSettings_Hide() {
        viewModel.processIntent(ChatIntent.ToggleSettings(true))
        viewModel.processIntent(ChatIntent.ToggleSettings(false))
    }

    @Test
    fun testProcessIntent_ToggleMetrics_Show() {
        viewModel.processIntent(ChatIntent.ToggleMetrics(true))
    }

    @Test
    fun testProcessIntent_ToggleMetrics_Hide() {
        viewModel.processIntent(ChatIntent.ToggleMetrics(true))
        viewModel.processIntent(ChatIntent.ToggleMetrics(false))
    }

    @Test
    fun testProcessIntent_ToggleReasoning_Show() {
        viewModel.processIntent(ChatIntent.ToggleReasoning(true))
    }

    @Test
    fun testProcessIntent_ToggleReasoning_Hide() {
        viewModel.processIntent(ChatIntent.ToggleReasoning(true))
        viewModel.processIntent(ChatIntent.ToggleReasoning(false))
    }

    @Test
    fun testProcessIntent_SetError() {
        viewModel.processIntent(ChatIntent.SetError("Test error"))
    }

    @Test
    fun testProcessIntent_ClearError() {
        viewModel.processIntent(ChatIntent.SetError("Test error"))
        viewModel.processIntent(ChatIntent.ClearError)
    }

    @Test
    fun testProcessIntent_SetLoading_True() {
        viewModel.processIntent(ChatIntent.SetLoading(true))
    }

    @Test
    fun testProcessIntent_SetLoading_False() {
        viewModel.processIntent(ChatIntent.SetLoading(true))
        viewModel.processIntent(ChatIntent.SetLoading(false))
    }

    @Test
    fun testProcessIntent_SetReasoningLoading_True() {
        viewModel.processIntent(ChatIntent.SetReasoningLoading(true))
    }

    @Test
    fun testProcessIntent_SetReasoningLoading_False() {
        viewModel.processIntent(ChatIntent.SetReasoningLoading(true))
        viewModel.processIntent(ChatIntent.SetReasoningLoading(false))
    }

    @Test
    fun testProcessIntent_MessageSent() {
        val response = ChatMessage(role = "assistant", content = "Response")
        viewModel.processIntent(ChatIntent.MessageSent(response, null))
        assertEquals(1, viewModel.messages.size)
        assertEquals(response, viewModel.messages.first())
    }

    @Test
    fun testProcessIntent_MessageSendFailed() {
        viewModel.processIntent(ChatIntent.SetLoading(true))
        viewModel.processIntent(ChatIntent.MessageSendFailed)
    }

    @Test
    fun testProcessIntent_UpdateReasoningComparison() {
        viewModel.processIntent(ChatIntent.UpdateReasoningComparison(null))
    }

    @Test
    fun testClearChat() {
        viewModel.processIntent(ChatIntent.UpdateInputText("Test"))
        viewModel.processIntent(ChatIntent.SetError("Error"))
        viewModel.processIntent(
            ChatIntent.MessageSent(
                ChatMessage(role = "user", content = "Test"),
                null
            )
        )
        viewModel.clearChat()
        assertEquals(emptyList(), viewModel.messages)
    }

    @Test
    fun testUpdateSettings() {
        val newSettings = ApiSettings(
            apiKey = "test-key",
            model = "new-model",
            maxTokens = 500,
            temperature = 0.7,
        )
        viewModel.updateSettings(newSettings)
    }

    @Test
    fun testSendMessage_WithEmptyInput() {
        viewModel.processIntent(ChatIntent.UpdateInputText(""))
        viewModel.processIntent(ChatIntent.SendMessage)
        assertFalse(viewModel.isLoading)
        assertEquals(0, viewModel.messages.size)
    }

    @Test
    fun testSendMessage_WithBlankInput() {
        viewModel.processIntent(ChatIntent.UpdateInputText("   "))
        viewModel.processIntent(ChatIntent.SendMessage)
        assertFalse(viewModel.isLoading)
        assertEquals(0, viewModel.messages.size)
    }
}

class FakeLlmClient : LlmClient {
    override suspend fun call(prompt: String, context: AgentContext, config: AgentConfig): Result<LlmResponse> =
        Result.success(
            LlmResponse(
                content = "Fake response",
                tokensUsed = 10,
                model = config.model
            )
        )

    override fun stream(prompt: String, context: AgentContext, config: AgentConfig): Flow<StreamChunk> = flow {
        emit(StreamChunk.Content("Default "))
        emit(StreamChunk.Content("response"))
        emit(StreamChunk.Done)
    }
}

class FakeContextStorage : ContextStorage {
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
