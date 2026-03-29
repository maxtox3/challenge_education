package chat

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import core.util.typedProp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import model.ChatMessage
import model.MetricRecord
import model.ReasoningComparison
import network.ChatClientImpl
import settings.ApiSettings

class ChatViewModel(repository: ChatRepository, viewModelScope: CoroutineScope, val listState: LazyListState,) {

    private val _uiState = MutableStateFlow(ChatState())
    val uiState: StateFlow<ChatState> = _uiState.asStateFlow()

    private val _sideEffects = MutableSharedFlow<ChatSideEffect>()
    val sideEffects: SharedFlow<ChatSideEffect> = _sideEffects.asSharedFlow()

    val state: ChatState get() = _uiState.value

    private val useCases = ChatUseCases(repository, viewModelScope)
    private val intentHandlers = ChatIntents(this)

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
            is ChatIntent.UpdateInputText -> intentHandlers.handleInputIntent(intent)

            is ChatIntent.SendMessage, is ChatIntent.MessageSent, is ChatIntent.MessageSendFailed ->
                intentHandlers.handleMessageIntent(intent)

            is ChatIntent.ClearChat -> intentHandlers.handleClearChatIntent()

            is ChatIntent.UpdateSettings, is ChatIntent.ToggleSettings ->
                intentHandlers.handleSettingsIntent(intent)

            is ChatIntent.ToggleMetrics -> intentHandlers.handleMetricsIntent(intent)

            is ChatIntent.ToggleReasoning, is ChatIntent.RunReasoningComparison,
            is ChatIntent.UpdateReasoningComparison, is ChatIntent.SetReasoningLoading ->
                intentHandlers.handleReasoningIntent(intent)

            is ChatIntent.SetError, is ChatIntent.ClearError, is ChatIntent.SetLoading ->
                intentHandlers.handleErrorIntent(intent)
        }
    }

    internal fun handleMessageSent(response: ChatMessage, metric: MetricRecord) {
        messages = messages + response
        metricCounter++
        metrics = metrics + metric.copy(id = metricCounter)
    }

    fun clearChat() {
        _uiState.update { it.copy(messages = emptyList(), errorMessage = null) }
    }

    fun updateSettings(newSettings: ApiSettings) {
        _uiState.update { it.copy(settings = newSettings, showSettings = false) }
    }

    fun sendMessage() {
        useCases.sendMessage(
            config = ChatUseCases.SendMessageConfig(
                inputText = inputText,
                isLoading = isLoading,
                currentMessages = messages,
                settings = settings,
            ),
            callbacks = ChatUseCases.MessageCallbacks(
                onMessagesUpdate = { messages = it },
                onInputTextUpdate = { inputText = it },
                onLoadingUpdate = { isLoading = it },
                onErrorUpdate = { errorMessage = it },
            ),
        )
    }

    fun runReasoningComparison(task: String) {
        useCases.runReasoningComparison(
            task = task,
            settings = settings,
            onComparisonUpdate = { reasoningComparison = it },
            onLoadingUpdate = { isReasoningLoading = it },
        )
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
