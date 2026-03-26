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
import model.StreamChunk

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

    var inputText: String by _uiState.typedProp(
        getter = { it.inputText },
        setter = { state, value -> state.copy(inputText = value) }
    )

    var messages: List<ChatMessage> by _uiState.typedProp(
        getter = { it.messages },
        setter = { state, value -> state.copy(messages = value) }
    )

    var isLoading: Boolean by _uiState.typedProp(
        getter = { it.isLoading },
        setter = { state, value -> state.copy(isLoading = value) }
    )

    var errorMessage: String? by _uiState.typedProp(
        getter = { it.errorMessage },
        setter = { state, value -> state.copy(errorMessage = value) }
    )

    var showSettings: Boolean by _uiState.typedProp(
        getter = { it.showSettings },
        setter = { state, value -> state.copy(showSettings = value) }
    )

    var showMetrics: Boolean by _uiState.typedProp(
        getter = { it.showMetrics },
        setter = { state, value -> state.copy(showMetrics = value) }
    )

    var showReasoning: Boolean by _uiState.typedProp(
        getter = { it.showReasoning },
        setter = { state, value -> state.copy(showReasoning = value) }
    )

    var metrics: List<MetricRecord> by _uiState.typedProp(
        getter = { it.metrics },
        setter = { state, value -> state.copy(metrics = value) }
    )

    var metricCounter: Int by _uiState.typedProp(
        getter = { it.metricCounter },
        setter = { state, value -> state.copy(metricCounter = value) }
    )

    var settings: ApiSettings by _uiState.typedProp(
        getter = { it.settings },
        setter = { state, value -> state.copy(settings = value) }
    )

    var reasoningComparison: ReasoningComparison by _uiState.typedProp(
        getter = { it.reasoningComparison },
        setter = { state, value -> state.copy(reasoningComparison = value) }
    )

    var isReasoningLoading: Boolean by _uiState.typedProp(
        getter = { it.isReasoningLoading },
        setter = { state, value -> state.copy(isReasoningLoading = value) }
    )

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
                    val currentContent = StringBuilder()
                    val currentReasoning = StringBuilder()
                    var isReasoningContent = false

                    try {
                        repository.sendMessageStreaming(
                            prompt = result.prompt,
                            messages = messages,
                            settings = settings,
                        ).collect { chunk ->
                            when (chunk) {
                                is StreamChunk.Content -> {
                                    currentContent.append(chunk.text)
                                    isReasoningContent = false
                                    updateStreamingMessage(
                                        content = currentContent.toString(),
                                        reasoning = currentReasoning.toString(),
                                        isReasoning = isReasoningContent,
                                    )
                                }

                                is StreamChunk.Reasoning -> {
                                    currentReasoning.append(chunk.text)
                                    isReasoningContent = true
                                    updateStreamingMessage(
                                        content = currentContent.toString(),
                                        reasoning = currentReasoning.toString(),
                                        isReasoning = isReasoningContent,
                                    )
                                }

                                is StreamChunk.Done -> {
                                    // Finalize the message
                                    val finalContent = currentContent.toString()
                                        .ifEmpty { currentReasoning.toString() }
                                    finalizeStreamingMessage(
                                        content = finalContent,
                                        isReasoning = currentContent.isEmpty() && currentReasoning.isNotEmpty(),
                                    )
                                    isLoading = false
                                }
                            }
                        }
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Streaming error"
                        isLoading = false
                    }
                }
            }

            is MessageHandler.ValidationResult.Invalid -> {
            }
        }
    }

    private fun updateStreamingMessage(content: String, reasoning: String, isReasoning: Boolean) {
        val streamingMessage = ChatMessage(
            role = "assistant",
            content = content.ifEmpty { reasoning },
            isReasoningContent = isReasoning,
            isStreaming = true,
        )
        messages = if (messages.isNotEmpty() && messages.last().isStreaming) {
            messages.dropLast(1) + streamingMessage
        } else {
            messages + streamingMessage
        }
    }

    private fun finalizeStreamingMessage(content: String, isReasoning: Boolean) {
        val finalMessage = ChatMessage(
            role = "assistant",
            content = content,
            isReasoningContent = isReasoning,
            isStreaming = false,
        )
        messages = if (messages.isNotEmpty() && messages.last().isStreaming) {
            messages.dropLast(1) + finalMessage
        } else {
            messages + finalMessage
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
    val client = remember { ChatClientImpl() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val repository = remember(client) { ChatRepositoryImpl(client, scope) }

    return remember(repository, scope, listState) {
        ChatViewModel(repository, scope, listState)
    }
}
