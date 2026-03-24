import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import model.ChatMessage
import model.MetricRecord
import model.ReasoningComparison

class ChatStateHolder(
    private val repository: ChatRepository,
    val scope: CoroutineScope,
    val listState: LazyListState,
) {
    private val _state = mutableStateOf(ChatState())
    val state: ChatState get() = _state.value

    private val _sideEffects = MutableSharedFlow<ChatSideEffect>()
    val sideEffects: Flow<ChatSideEffect> = _sideEffects.asSharedFlow()

    var inputText by mutableStateOf("")

    var messages by mutableStateOf<List<ChatMessage>>(emptyList())

    var isLoading by mutableStateOf(false)

    var errorMessage by mutableStateOf<String?>(null)

    var showSettings by mutableStateOf(false)

    var showMetrics by mutableStateOf(false)

    var showReasoning by mutableStateOf(false)

    var metrics by mutableStateOf<List<MetricRecord>>(emptyList())

    var metricCounter by mutableStateOf(0)

    var settings by mutableStateOf(ApiSettings())

    var reasoningComparison by mutableStateOf(
        ReasoningComparison(
            task = "У тебя есть 12 монет, одна из которых фальшивая " +
                "(легче или тяжелее — неизвестно). У тебя есть чашечные весы. " +
                "Как найти фальшивую монету за минимальное количество взвешиваний?",
            results = emptyMap(),
        ),
    )

    var isReasoningLoading by mutableStateOf(false)

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

    private fun updateState(newState: ChatState) {
        _state.value = newState
        inputText = newState.inputText
        messages = newState.messages
        isLoading = newState.isLoading
        errorMessage = newState.errorMessage
        showSettings = newState.showSettings
        showMetrics = newState.showMetrics
        showReasoning = newState.showReasoning
        metrics = newState.metrics
        metricCounter = newState.metricCounter
        settings = newState.settings
        reasoningComparison = newState.reasoningComparison
        isReasoningLoading = newState.isReasoningLoading
    }

    fun clearChat() {
        messages = emptyList()
        errorMessage = null
        updateState(_state.value.copy(messages = emptyList(), errorMessage = null))
    }

    fun updateSettings(newSettings: ApiSettings) {
        settings = newSettings
        showSettings = false
        updateState(_state.value.copy(settings = newSettings, showSettings = false))
    }

    fun sendMessage() {
        when (val result = MessageHandler.validateAndPrepare(inputText, isLoading)) {
            is MessageHandler.ValidationResult.Valid -> {
                messages = messages + result.message
                inputText = ""
                isLoading = true
                errorMessage = null

                updateState(
                    _state.value.copy(
                        messages = messages,
                        inputText = "",
                        isLoading = true,
                        errorMessage = null,
                    ),
                )

                scope.launch {
                    val apiResult = repository.sendMessage(
                        prompt = result.prompt,
                        messages = messages,
                        settings = settings,
                    )

                    isLoading = false
                    updateState(_state.value.copy(isLoading = false))

                    when (apiResult) {
                        is SendMessageResult.Success -> {
                            messages = messages + apiResult.response
                            metricCounter++
                            val metric = apiResult.metric.copy(id = metricCounter)
                            metrics = metrics + metric

                            updateState(
                                _state.value.copy(
                                    messages = messages,
                                    metricCounter = metricCounter,
                                    metrics = metrics,
                                ),
                            )
                        }

                        is SendMessageResult.Error -> {
                            errorMessage = apiResult.message
                            updateState(_state.value.copy(errorMessage = apiResult.message))
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
        updateState(_state.value.copy(isReasoningLoading = true))

        scope.launch {
            repository.runReasoningComparison(
                task = task,
                settings = settings,
                onProgress = { comparison ->
                    reasoningComparison = comparison
                    isReasoningLoading = comparison.results.values.any { it.isLoading }
                    updateState(
                        _state.value.copy(
                            reasoningComparison = comparison,
                            isReasoningLoading = isReasoningLoading,
                        ),
                    )
                },
            )
        }
    }
}

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
