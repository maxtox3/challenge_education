@file:OptIn(ExperimentalTestApi::class)

package ui

import agent.AgentConfig
import agent.AgentContext
import agent.AgentStore
import agent.ContextStorage
import agent.LlmClient
import agent.LlmResponse
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import chat.ChatIntent
import chat.ChatViewModel
import chat.ui.AppTags
import chat.ui.AppWithState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import model.ChatMessage
import model.StreamChunk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class AppUiRealTest {
    private lateinit var mockLlmClient: MockLlmClientForUi
    private lateinit var mockStorage: MockContextStorageForUi
    private lateinit var agentStore: AgentStore
    private lateinit var viewModel: ChatViewModel
    private lateinit var listState: LazyListState

    @BeforeTest
    fun setup() {
        mockLlmClient = MockLlmClientForUi()
        mockStorage = MockContextStorageForUi()
        listState = LazyListState()
        agentStore = AgentStore(mockLlmClient, mockStorage)
        viewModel = ChatViewModel(agentStore, listState)
    }

    @Test
    fun appDisplaysEmptyStateWhenNoMessages() = runComposeUiTest {
        setContent {
            AppWithState(viewModel = viewModel, apiKey = "test-api-key")
        }

        onNodeWithTag(AppTags.ROOT).assertExists()
        onNodeWithTag(AppTags.EMPTY_STATE).assertExists()
        onNodeWithText("Start a conversation").assertExists()
        onNodeWithText("Press Enter to send").assertExists()
    }

    @Test
    fun appMessageListExistsWhenHasMessages() = runComposeUiTest {
        viewModel.processIntent(
            ChatIntent.MessageSent(
                response = ChatMessage(role = "user", content = "Hello"),
                metric = null
            )
        )

        setContent {
            AppWithState(viewModel = viewModel, apiKey = "test-api-key")
        }

        onNodeWithTag(AppTags.MESSAGE_LIST).assertExists()
        onNodeWithTag(AppTags.EMPTY_STATE).assertDoesNotExist()
    }

    @Test
    fun appClearButtonExists() = runComposeUiTest {
        setContent {
            AppWithState(viewModel = viewModel, apiKey = "test-api-key")
        }

        onNodeWithTag(AppTags.CLEAR_BUTTON).assertExists()
    }

    @Test
    fun appMetricsButtonExists() = runComposeUiTest {
        setContent {
            AppWithState(viewModel = viewModel, apiKey = "test-api-key")
        }

        onNodeWithTag(AppTags.METRICS_BUTTON).assertExists()
    }

    @Test
    fun appReasoningButtonExists() = runComposeUiTest {
        setContent {
            AppWithState(viewModel = viewModel, apiKey = "test-api-key")
        }

        onNodeWithTag(AppTags.REASONING_BUTTON).assertExists()
    }

    @Test
    fun appSettingsButtonExists() = runComposeUiTest {
        setContent {
            AppWithState(viewModel = viewModel, apiKey = "test-api-key")
        }

        onNodeWithTag(AppTags.SETTINGS_BUTTON).assertExists()
    }

    @Test
    fun appClearButtonClearsChat() = runComposeUiTest {
        viewModel.processIntent(
            ChatIntent.MessageSent(
                response = ChatMessage(role = "user", content = "Test message"),
                metric = null
            )
        )

        setContent {
            AppWithState(viewModel = viewModel, apiKey = "test-api-key")
        }

        onNodeWithTag(AppTags.MESSAGE_LIST).assertExists()

        onNodeWithTag(AppTags.CLEAR_BUTTON).performClick()

        assertTrue(viewModel.messages.isEmpty())
    }

    @Test
    fun appMetricsButtonOpensMetricsDialog() = runComposeUiTest {
        setContent {
            AppWithState(viewModel = viewModel, apiKey = "test-api-key")
        }

        onNodeWithTag(AppTags.METRICS_BUTTON).performClick()
    }

    @Test
    fun appReasoningButtonOpensReasoningDialog() = runComposeUiTest {
        setContent {
            AppWithState(viewModel = viewModel, apiKey = "test-api-key")
        }

        onNodeWithTag(AppTags.REASONING_BUTTON).performClick()
    }

    @Test
    fun appSettingsButtonOpensSettingsDialog() = runComposeUiTest {
        setContent {
            AppWithState(viewModel = viewModel, apiKey = "test-api-key")
        }

        onNodeWithTag(AppTags.SETTINGS_BUTTON).performClick()
    }

    @Test
    fun appDisplaysErrorWhenErrorSet() = runComposeUiTest {
        viewModel.processIntent(ChatIntent.SetError("Test error message"))

        setContent {
            AppWithState(viewModel = viewModel, apiKey = "test-api-key")
        }

        onNodeWithTag(AppTags.ERROR_SURFACE).assertExists()
        onNodeWithTag(AppTags.ERROR_TEXT).assertExists()
        onNodeWithText("Test error message").assertExists()
    }

    @Test
    fun appDismissErrorButtonExists() = runComposeUiTest {
        viewModel.processIntent(ChatIntent.SetError("Test error"))

        setContent {
            AppWithState(viewModel = viewModel, apiKey = "test-api-key")
        }

        onNodeWithTag(AppTags.ERROR_DISMISS_BUTTON).assertExists()
    }

    @Test
    fun appDismissErrorButtonClearsError() = runComposeUiTest {
        viewModel.processIntent(ChatIntent.SetError("Test error to dismiss"))

        setContent {
            AppWithState(viewModel = viewModel, apiKey = "test-api-key")
        }

        onNodeWithTag(AppTags.ERROR_SURFACE).assertExists()

        onNodeWithTag(AppTags.ERROR_DISMISS_BUTTON).performClick()
    }

    @Test
    fun appNoErrorDisplayedWhenNoError() = runComposeUiTest {
        setContent {
            AppWithState(viewModel = viewModel, apiKey = "test-api-key")
        }

        onNodeWithTag(AppTags.ERROR_SURFACE).assertDoesNotExist()
    }

    @Test
    fun appLoadingIndicatorShownWhenLoading() = runComposeUiTest {
        viewModel.processIntent(ChatIntent.SetLoading(true))

        setContent {
            AppWithState(viewModel = viewModel, apiKey = "test-api-key")
        }

        onNodeWithTag(AppTags.LOADING_INDICATOR).assertExists()
    }

    @Test
    fun appHeaderDisplaysTitle() = runComposeUiTest {
        setContent {
            AppWithState(viewModel = viewModel, apiKey = "test-api-key")
        }

        onNodeWithTag(AppTags.HEADER).assertExists()
        onNodeWithTag(AppTags.TITLE).assertExists()
        onNodeWithText("Z.ai Chat").assertExists()
    }

    @Test
    fun appHeaderDisplaysModelName() = runComposeUiTest {
        setContent {
            AppWithState(viewModel = viewModel, apiKey = "test-api-key")
        }

        onNodeWithTag(AppTags.MODEL_TEXT).assertExists()
        onNodeWithText("Model: glm-5").assertExists()
    }

    @Test
    fun appErrorSurfaceDisappearsAfterDismiss() = runComposeUiTest {
        viewModel.processIntent(ChatIntent.SetError("Error to dismiss"))

        setContent {
            AppWithState(viewModel = viewModel, apiKey = "test-api-key")
        }

        onNodeWithTag(AppTags.ERROR_SURFACE).assertExists()

        onNodeWithTag(AppTags.ERROR_DISMISS_BUTTON).performClick()
    }

    @Test
    fun appMultipleErrorsShowsLatestError() = runComposeUiTest {
        viewModel.processIntent(ChatIntent.SetError("First error"))
        viewModel.processIntent(ChatIntent.SetError("Second error"))

        setContent {
            AppWithState(viewModel = viewModel, apiKey = "test-api-key")
        }

        onNodeWithText("Second error").assertExists()
        onNodeWithText("First error").assertDoesNotExist()
    }

    @Test
    fun appClearChatClearsError() = runComposeUiTest {
        viewModel.processIntent(
            ChatIntent.MessageSent(
                response = ChatMessage(role = "user", content = "Test"),
                metric = null
            )
        )
        viewModel.processIntent(ChatIntent.SetError("Error"))

        setContent {
            AppWithState(viewModel = viewModel, apiKey = "test-api-key")
        }

        onNodeWithTag(AppTags.ERROR_SURFACE).assertExists()

        onNodeWithTag(AppTags.CLEAR_BUTTON).performClick()
    }
}

class MockLlmClientForUi : LlmClient {
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

class MockContextStorageForUi : ContextStorage {
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
