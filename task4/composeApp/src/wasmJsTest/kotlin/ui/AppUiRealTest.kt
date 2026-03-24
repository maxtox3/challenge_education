@file:OptIn(ExperimentalTestApi::class)

package ui

import ApiSettings
import AppTags
import AppWithState
import ChatIntent
import ChatRepository
import ChatViewModel
import SendMessageResult
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.test.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import model.ChatMessage
import model.ConstraintsInfo
import model.MetricRecord
import model.ReasoningComparison
import model.ReasoningMode
import model.ReasoningResult
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppUiRealTest {
    private lateinit var mockRepository: MockChatRepositoryForUi
    private lateinit var viewModel: ChatViewModel
    private lateinit var listState: LazyListState

    @BeforeTest
    fun setup() {
        mockRepository = MockChatRepositoryForUi()
        listState = LazyListState()
        viewModel = ChatViewModel(
            repository = mockRepository,
            viewModelScope = CoroutineScope(Dispatchers.Default),
            listState = listState,
        )
    }

    @Test
    fun app_displaysEmptyState_whenNoMessages() = runComposeUiTest {
        setContent {
            AppWithState(state = viewModel)
        }

        onNodeWithTag(AppTags.ROOT).assertExists()
        onNodeWithTag(AppTags.EMPTY_STATE).assertExists()
        onNodeWithText("Start a conversation").assertExists()
        onNodeWithText("Press Enter to send").assertExists()
    }

    @Test
    fun app_messageListExists_whenHasMessages() = runComposeUiTest {
        viewModel.processIntent(
            ChatIntent.MessageSent(
                response = ChatMessage(role = "user", content = "Hello"),
                metric = MetricRecord(
                    id = 0,
                    prompt = "Hello",
                    response = "Hello",
                    mode = "free",
                    responseLength = 5,
                    tokensUsed = 5,
                    maxTokens = null,
                    finishReason = "stop",
                    responseTimeMs = 100,
                    constraints = ConstraintsInfo(null, emptyList(), "text", 1.0),
                ),
            )
        )

        setContent {
            AppWithState(state = viewModel)
        }

        onNodeWithTag(AppTags.MESSAGE_LIST).assertExists()
        onNodeWithTag(AppTags.EMPTY_STATE).assertDoesNotExist()
    }

    @Test
    fun app_clearButton_exists() = runComposeUiTest {
        setContent {
            AppWithState(state = viewModel)
        }

        onNodeWithTag(AppTags.CLEAR_BUTTON).assertExists()
    }

    @Test
    fun app_metricsButton_exists() = runComposeUiTest {
        setContent {
            AppWithState(state = viewModel)
        }

        onNodeWithTag(AppTags.METRICS_BUTTON).assertExists()
    }

    @Test
    fun app_reasoningButton_exists() = runComposeUiTest {
        setContent {
            AppWithState(state = viewModel)
        }

        onNodeWithTag(AppTags.REASONING_BUTTON).assertExists()
    }

    @Test
    fun app_settingsButton_exists() = runComposeUiTest {
        setContent {
            AppWithState(state = viewModel)
        }

        onNodeWithTag(AppTags.SETTINGS_BUTTON).assertExists()
    }

    @Test
    fun app_clearButton_clearsChat() = runComposeUiTest {
        viewModel.processIntent(
            ChatIntent.MessageSent(
                response = ChatMessage(role = "user", content = "Test message"),
                metric = MetricRecord(
                    id = 0,
                    prompt = "Test",
                    response = "Test",
                    mode = "free",
                    responseLength = 12,
                    tokensUsed = 5,
                    maxTokens = null,
                    finishReason = "stop",
                    responseTimeMs = 100,
                    constraints = ConstraintsInfo(null, emptyList(), "text", 1.0),
                ),
            )
        )

        setContent {
            AppWithState(state = viewModel)
        }

        onNodeWithTag(AppTags.MESSAGE_LIST).assertExists()

        onNodeWithTag(AppTags.CLEAR_BUTTON).performClick()

        assertTrue(viewModel.messages.isEmpty())
    }

    @Test
    fun app_metricsButton_opensMetricsDialog() = runComposeUiTest {
        setContent {
            AppWithState(state = viewModel)
        }

        assertFalse(viewModel.showMetrics)

        onNodeWithTag(AppTags.METRICS_BUTTON).performClick()

        assertTrue(viewModel.showMetrics)
    }

    @Test
    fun app_reasoningButton_opensReasoningDialog() = runComposeUiTest {
        setContent {
            AppWithState(state = viewModel)
        }

        assertFalse(viewModel.showReasoning)

        onNodeWithTag(AppTags.REASONING_BUTTON).performClick()

        assertTrue(viewModel.showReasoning)
    }

    @Test
    fun app_settingsButton_opensSettingsDialog() = runComposeUiTest {
        setContent {
            AppWithState(state = viewModel)
        }

        assertFalse(viewModel.showSettings)

        onNodeWithTag(AppTags.SETTINGS_BUTTON).performClick()

        assertTrue(viewModel.showSettings)
    }

    @Test
    fun app_displaysError_whenErrorSet() = runComposeUiTest {
        viewModel.processIntent(ChatIntent.SetError("Test error message"))

        setContent {
            AppWithState(state = viewModel)
        }

        onNodeWithTag(AppTags.ERROR_SURFACE).assertExists()
        onNodeWithTag(AppTags.ERROR_TEXT).assertExists()
        onNodeWithText("Test error message").assertExists()
    }

    @Test
    fun app_dismissErrorButton_exists() = runComposeUiTest {
        viewModel.processIntent(ChatIntent.SetError("Test error"))

        setContent {
            AppWithState(state = viewModel)
        }

        onNodeWithTag(AppTags.ERROR_DISMISS_BUTTON).assertExists()
    }

    @Test
    fun app_dismissErrorButton_clearsError() = runComposeUiTest {
        viewModel.processIntent(ChatIntent.SetError("Test error to dismiss"))

        setContent {
            AppWithState(state = viewModel)
        }

        onNodeWithTag(AppTags.ERROR_SURFACE).assertExists()

        onNodeWithTag(AppTags.ERROR_DISMISS_BUTTON).performClick()

        assertEquals(null, viewModel.errorMessage)
    }

    @Test
    fun app_noErrorDisplayed_whenNoError() = runComposeUiTest {
        setContent {
            AppWithState(state = viewModel)
        }

        onNodeWithTag(AppTags.ERROR_SURFACE).assertDoesNotExist()
    }

    @Test
    fun app_loadingIndicator_notShown_whenNotLoading() = runComposeUiTest {
        setContent {
            AppWithState(state = viewModel)
        }

        assertFalse(viewModel.isLoading)
        onNodeWithTag(AppTags.LOADING_INDICATOR).assertDoesNotExist()
    }

    @Test
    fun app_loadingIndicator_shown_whenLoading() = runComposeUiTest {
        viewModel.processIntent(ChatIntent.SetLoading(true))

        setContent {
            AppWithState(state = viewModel)
        }

        onNodeWithTag(AppTags.LOADING_INDICATOR).assertExists()
    }

    @Test
    fun app_header_displaysTitle() = runComposeUiTest {
        setContent {
            AppWithState(state = viewModel)
        }

        onNodeWithTag(AppTags.HEADER).assertExists()
        onNodeWithTag(AppTags.TITLE).assertExists()
        onNodeWithText("Z.ai Chat").assertExists()
    }

    @Test
    fun app_header_displaysModelName() = runComposeUiTest {
        setContent {
            AppWithState(state = viewModel)
        }

        onNodeWithTag(AppTags.MODEL_TEXT).assertExists()
        onNodeWithText("Model: glm-5").assertExists()
    }

    @Test
    fun app_errorSurface_disappearsAfterDismiss() = runComposeUiTest {
        viewModel.processIntent(ChatIntent.SetError("Error to dismiss"))

        setContent {
            AppWithState(state = viewModel)
        }

        onNodeWithTag(AppTags.ERROR_SURFACE).assertExists()

        onNodeWithTag(AppTags.ERROR_DISMISS_BUTTON).performClick()

        assertEquals(null, viewModel.errorMessage)
    }

    @Test
    fun app_multipleErrors_showsLatestError() = runComposeUiTest {
        viewModel.processIntent(ChatIntent.SetError("First error"))
        viewModel.processIntent(ChatIntent.SetError("Second error"))

        setContent {
            AppWithState(state = viewModel)
        }

        onNodeWithText("Second error").assertExists()
        onNodeWithText("First error").assertDoesNotExist()
    }

    @Test
    fun app_clearChat_clearsError() = runComposeUiTest {
        viewModel.processIntent(
            ChatIntent.MessageSent(
                response = ChatMessage(role = "user", content = "Test"),
                metric = MetricRecord(
                    id = 0,
                    prompt = "Test",
                    response = "Test",
                    mode = "free",
                    responseLength = 4,
                    tokensUsed = 5,
                    maxTokens = null,
                    finishReason = "stop",
                    responseTimeMs = 100,
                    constraints = ConstraintsInfo(null, emptyList(), "text", 1.0),
                ),
            )
        )
        viewModel.processIntent(ChatIntent.SetError("Error"))

        setContent {
            AppWithState(state = viewModel)
        }

        onNodeWithTag(AppTags.ERROR_SURFACE).assertExists()

        onNodeWithTag(AppTags.CLEAR_BUTTON).performClick()

        assertEquals(null, viewModel.errorMessage)
    }
}

class MockChatRepositoryForUi : ChatRepository {
    override suspend fun sendMessage(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings,
    ): SendMessageResult = SendMessageResult.Success(
        response = ChatMessage(
            role = "assistant",
            content = "Mock response",
            tokensUsed = 10,
            maxTokens = settings.maxTokens,
            finishReason = "stop",
        ),
        metric = MetricRecord(
            id = 0,
            prompt = prompt,
            response = "Mock response",
            mode = "free",
            responseLength = 13,
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
                systemPrompt = "Mock prompt",
                actualPrompt = task,
                response = "Mock response",
                responseTimeMs = 100,
                tokensUsed = 50,
            )
        }
        return ReasoningComparison(task = task, results = results)
    }
}
