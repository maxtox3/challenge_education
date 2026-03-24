import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import model.ChatMessage
import model.MetricRecord
import model.ReasoningComparison

class ChatViewModel(
    private val repository: ChatRepository,
    private val viewModelScope: CoroutineScope,
    val listState: LazyListState,
) {
    private val _uiState = MutableStateFlow(ChatState())
    val uiState: StateFlow<ChatState> = _uiState.asStateFlow()

    private val _sideEffects = MutableSharedFlow<ChatSideEffect>()
    val sideEffects: SharedFlow<ChatSideEffect> = _sideEffects.asSharedFlow()

    val state: ChatState get() = _uiState.value

    var inputText: String
        get() = _uiState.value.inputText
        set(value) = _uiState.update { it.copy(inputText = value) }

    var messages: List<ChatMessage>
        get() = _uiState.value.messages
        set(value) = _uiState.update { it.copy(messages = value) }

    var isLoading: Boolean
        get() = _uiState.value.isLoading
        set(value) = _uiState.update { it.copy(isLoading = value) }

    var errorMessage: String?
        get() = _uiState.value.errorMessage
        set(value) = _uiState.update { it.copy(errorMessage = value) }

    var showSettings: Boolean
        get() = _uiState.value.showSettings
        set(value) = _uiState.update { it.copy(showSettings = value) }

    var showMetrics: Boolean
        get() = _uiState.value.showMetrics
        set(value) = _uiState.update { it.copy(showMetrics = value) }

    var showReasoning: Boolean
        get() = _uiState.value.showReasoning
        set(value) = _uiState.update { it.copy(showReasoning = value) }

    var metrics: List<MetricRecord>
        get() = _uiState.value.metrics
        set(value) = _uiState.update { it.copy(metrics = value) }

    var metricCounter: Int
        get() = _uiState.value.metricCounter
        set(value) = _uiState.update { it.copy(metricCounter = value) }

    var settings: ApiSettings
        get() = _uiState.value.settings
        set(value) = _uiState.update { it.copy(settings = value) }

    var reasoningComparison: ReasoningComparison
        get() = _uiState.value.reasoningComparison
        set(value) = _uiState.update { it.copy(reasoningComparison = value) }

    var isReasoningLoading: Boolean
        get() = _uiState.value.isReasoningLoading
        set(value) = _uiState.update { it.copy(isReasoningLoading = value) }

    fun processIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.UpdateInputText -> updateInputText(intent.text)
            is ChatIntent.SendMessage -> sendMessage()
            is ChatIntent.ClearChat -> clearChat()
            is ChatIntent.UpdateSettings -> updateSettings(intent.settings)
            is ChatIntent.ToggleSettings -> toggleSettings(intent.show)
            is ChatIntent.ToggleMetrics -> toggleMetrics(intent.show)
            is ChatIntent.ToggleReasoning -> toggleReasoning(intent.show)
            is ChatIntent.RunReasoningComparison -> runReasoningComparison(intent.task)
            is ChatIntent.MessageSent -> onMessageSent(intent.response, intent.metric)
            is ChatIntent.MessageSendFailed -> onMessageSendFailed()
            is ChatIntent.SetError -> setError(intent.message)
            is ChatIntent.ClearError -> clearError()
            is ChatIntent.SetLoading -> setLoading(intent.loading)
            is ChatIntent.UpdateReasoningComparison -> updateReasoningComparison(intent.comparison)
            is ChatIntent.SetReasoningLoading -> setReasoningLoading(intent.loading)
        }
    }

    private fun updateInputText(text: String) {
        inputText = text
    }

    private fun toggleSettings(show: Boolean) {
        showSettings = show
    }

    private fun toggleMetrics(show: Boolean) {
        showMetrics = show
    }

    private fun toggleReasoning(show: Boolean) {
        showReasoning = show
    }

    private fun setError(message: String?) {
        errorMessage = message
    }

    private fun clearError() {
        errorMessage = null
    }

    private fun setLoading(loading: Boolean) {
        isLoading = loading
    }

    private fun setReasoningLoading(loading: Boolean) {
        isReasoningLoading = loading
    }

    private fun updateReasoningComparison(comparison: ReasoningComparison) {
        reasoningComparison = comparison
    }

    private fun onMessageSent(response: ChatMessage, metric: MetricRecord) {
        messages = messages + response
        metricCounter++
        metrics = metrics + metric.copy(id = metricCounter)
    }

    private fun onMessageSendFailed() {
        isLoading = false
    }

    fun clearChat() {
        _uiState.update { it.copy(messages = emptyList(), errorMessage = null) }
    }

    fun updateSettings(newSettings: ApiSettings) {
        _uiState.update { it.copy(settings = newSettings, showSettings = false) }
    }

    fun sendMessage() {
        when (val result = MessageHandler.validateAndPrepare(inputText, isLoading)) {
            is MessageHandler.ValidationResult.Valid -> {
                messages = messages + result.message
                inputText = ""
                isLoading = true
                errorMessage = null

                viewModelScope.launch {
                    val apiResult = repository.sendMessage(
                        prompt = result.prompt,
                        messages = messages,
                        settings = settings,
                    )

                    isLoading = false

                    when (apiResult) {
                        is SendMessageResult.Success -> {
                            messages = messages + apiResult.response
                            metricCounter++
                            val metric = apiResult.metric.copy(id = metricCounter)
                            metrics = metrics + metric
                        }

                        is SendMessageResult.Error -> {
                            errorMessage = apiResult.message
                        }
                    }
                }
            }

            is MessageHandler.ValidationResult.Invalid -> {
            }
        }
    }

    fun runReasoningComparison(task: String) {
        isReasoningLoading = true

        viewModelScope.launch {
            repository.runReasoningComparison(
                task = task,
                settings = settings,
                onProgress = { comparison ->
                    reasoningComparison = comparison
                    isReasoningLoading = comparison.results.values.any { it.isLoading }
                },
            )
        }
    }
}

@Composable
fun rememberChatViewModel(): ChatViewModel {
    val client = remember { ChatClient() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val repository = remember(client) { ChatRepositoryImpl(client, scope) }

    return remember(repository, scope, listState) {
        ChatViewModel(repository, scope, listState)
    }
}
