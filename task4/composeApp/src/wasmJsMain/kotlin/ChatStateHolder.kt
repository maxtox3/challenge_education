import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import model.ChatMessage
import model.MetricRecord
import model.ReasoningComparison

@Deprecated(
    message = "Use ChatViewModel instead. ChatStateHolder is deprecated and will be removed in a future version.",
    replaceWith = ReplaceWith("ChatViewModel(repository, scope, listState)", ""),
)
class ChatStateHolder(
    private val repository: ChatRepository,
    val scope: CoroutineScope,
    val listState: LazyListState,
) {
    private val viewModel = ChatViewModel(repository, scope, listState)

    val state: ChatState get() = viewModel.state

    val sideEffects: Flow<ChatSideEffect> get() = viewModel.sideEffects

    var inputText: String
        get() = viewModel.inputText
        set(value) {
            viewModel.inputText = value
        }

    var messages: List<ChatMessage>
        get() = viewModel.messages
        set(value) {
            viewModel.messages = value
        }

    var isLoading: Boolean
        get() = viewModel.isLoading
        set(value) {
            viewModel.isLoading = value
        }

    var errorMessage: String?
        get() = viewModel.errorMessage
        set(value) {
            viewModel.errorMessage = value
        }

    var showSettings: Boolean
        get() = viewModel.showSettings
        set(value) {
            viewModel.showSettings = value
        }

    var showMetrics: Boolean
        get() = viewModel.showMetrics
        set(value) {
            viewModel.showMetrics = value
        }

    var showReasoning: Boolean
        get() = viewModel.showReasoning
        set(value) {
            viewModel.showReasoning = value
        }

    var metrics: List<MetricRecord>
        get() = viewModel.metrics
        set(value) {
            viewModel.metrics = value
        }

    var metricCounter: Int
        get() = viewModel.metricCounter
        set(value) {
            viewModel.metricCounter = value
        }

    var settings: ApiSettings
        get() = viewModel.settings
        set(value) {
            viewModel.settings = value
        }

    var reasoningComparison: ReasoningComparison
        get() = viewModel.reasoningComparison
        set(value) {
            viewModel.reasoningComparison = value
        }

    var isReasoningLoading: Boolean
        get() = viewModel.isReasoningLoading
        set(value) {
            viewModel.isReasoningLoading = value
        }

    fun processIntent(intent: ChatIntent) = viewModel.processIntent(intent)

    fun clearChat() = viewModel.clearChat()

    fun updateSettings(newSettings: ApiSettings) = viewModel.updateSettings(newSettings)

    fun sendMessage() = viewModel.sendMessage()

    fun runReasoningComparison(task: String) = viewModel.runReasoningComparison(task)
}

@Deprecated(
    message = "Use rememberChatViewModel instead.",
    replaceWith = ReplaceWith("rememberChatViewModel()", ""),
)
@Composable
fun rememberChatStateHolder(): ChatStateHolder {
    val client = remember { ChatClient() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val repository = remember(client) { ChatRepositoryImpl(client, scope) }

    return remember(repository, scope, listState) {
        ChatStateHolder(repository, scope, listState)
    }
}
