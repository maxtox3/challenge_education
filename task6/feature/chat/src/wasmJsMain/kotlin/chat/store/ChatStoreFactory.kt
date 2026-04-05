package chat.store

import chat.ChatIntent
import chat.ChatLabel
import chat.ChatRepository
import chat.ChatState
import chat.SendMessageResult
import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.coroutineExecutorFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import model.ChatMessage
import settings.ApiSettings

class ChatStoreFactory(private val storeFactory: StoreFactory, private val repository: ChatRepository,) {

    private sealed interface Msg {
        data class InputTextChanged(val text: String) : Msg
        data class MessagesUpdated(val messages: List<ChatMessage>) : Msg
        data class LoadingChanged(val isLoading: Boolean) : Msg
        data class ErrorChanged(val error: String?) : Msg
        data class SettingsToggled(val show: Boolean) : Msg
        data class MetricsToggled(val show: Boolean) : Msg
        data class ReasoningToggled(val show: Boolean) : Msg
        data class MetricsUpdated(val metrics: List<model.MetricRecord>, val counter: Int) : Msg
        data class SettingsUpdated(val settings: ApiSettings) : Msg
        data class ReasoningComparisonUpdated(val comparison: model.ReasoningComparison?) : Msg
        data class ReasoningLoadingChanged(val isLoading: Boolean) : Msg
    }

    private class ExecutorContext(
        val dispatchFn: (Msg) -> Unit,
        val publishFn: (ChatLabel) -> Unit,
        val getStateFn: () -> ChatState
    )

    private fun handleSendMessage(
        currentInput: String,
        currentState: ChatState,
        ctx: ExecutorContext,
        launchBlock: (suspend () -> Unit) -> Unit
    ) {
        if (currentInput.isBlank()) return

        val userMessage = ChatMessage(role = "user", content = currentInput)
        val updatedMessages = currentState.messages + userMessage

        ctx.dispatchFn(Msg.InputTextChanged(""))
        ctx.dispatchFn(Msg.MessagesUpdated(updatedMessages))
        ctx.dispatchFn(Msg.LoadingChanged(true))
        ctx.dispatchFn(Msg.ErrorChanged(null))

        launchBlock {
            try {
                when (
                    val result = repository.sendMessage(
                        prompt = currentInput,
                        messages = currentState.messages,
                        settings = currentState.settings
                    )
                ) {
                    is SendMessageResult.Success -> {
                        val assistantMessage = result.response
                        val finalMessages = updatedMessages + assistantMessage
                        ctx.dispatchFn(Msg.MessagesUpdated(finalMessages))
                        val stateAfterUpdate = ctx.getStateFn()
                        ctx.dispatchFn(
                            Msg.MetricsUpdated(
                                stateAfterUpdate.metrics + result.metric,
                                stateAfterUpdate.metricCounter + 1
                            )
                        )
                        ctx.dispatchFn(Msg.LoadingChanged(false))
                        ctx.publishFn(ChatLabel.ScrollToBottom)
                    }

                    is SendMessageResult.Error -> {
                        ctx.dispatchFn(Msg.LoadingChanged(false))
                        ctx.dispatchFn(Msg.ErrorChanged(result.message))
                        ctx.publishFn(ChatLabel.ShowToast(result.message))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalStateException) {
                ctx.dispatchFn(Msg.LoadingChanged(false))
                ctx.dispatchFn(Msg.ErrorChanged(e.message ?: "Invalid state"))
                ctx.publishFn(ChatLabel.ShowToast(e.message ?: "Invalid state"))
            } catch (e: IllegalArgumentException) {
                ctx.dispatchFn(Msg.LoadingChanged(false))
                ctx.dispatchFn(Msg.ErrorChanged(e.message ?: "Invalid request"))
                ctx.publishFn(ChatLabel.ShowToast(e.message ?: "Invalid request"))
            }
        }
    }

    private suspend fun handleReasoningComparison(task: String, settings: ApiSettings, ctx: ExecutorContext) {
        try {
            val result = repository.runReasoningComparison(
                task = task,
                settings = settings,
                onProgress = { comparison ->
                    ctx.dispatchFn(Msg.ReasoningComparisonUpdated(comparison))
                }
            )
            ctx.dispatchFn(Msg.ReasoningComparisonUpdated(result))
            ctx.dispatchFn(Msg.ReasoningLoadingChanged(false))
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalStateException) {
            ctx.dispatchFn(Msg.ReasoningLoadingChanged(false))
            ctx.dispatchFn(Msg.ErrorChanged(e.message ?: "Reasoning comparison failed"))
            ctx.publishFn(ChatLabel.ShowToast(e.message ?: "Reasoning comparison failed"))
        } catch (e: IllegalArgumentException) {
            ctx.dispatchFn(Msg.ReasoningLoadingChanged(false))
            ctx.dispatchFn(Msg.ErrorChanged(e.message ?: "Invalid reasoning parameters"))
            ctx.publishFn(ChatLabel.ShowToast(e.message ?: "Invalid reasoning parameters"))
        }
    }

    private fun handleMessageSent(
        response: model.ChatMessage,
        metric: model.MetricRecord?,
        currentState: ChatState,
        ctx: ExecutorContext
    ) {
        val updatedMessages = currentState.messages + response
        ctx.dispatchFn(Msg.MessagesUpdated(updatedMessages))
        metric?.let { m ->
            ctx.dispatchFn(
                Msg.MetricsUpdated(
                    currentState.metrics + m,
                    currentState.metricCounter + 1
                )
            )
        }
        ctx.publishFn(ChatLabel.ScrollToBottom)
    }

    fun create(): ChatStore = object :
        ChatStore,
        Store<ChatIntent, ChatState, ChatLabel> by storeFactory.create(
            name = "ChatStore",
            initialState = ChatState(),
            executorFactory = coroutineExecutorFactory {
                onIntent<ChatIntent.UpdateInputText> { dispatch(Msg.InputTextChanged(it.text)) }
                onIntent<ChatIntent.SendMessage> {
                    val ctx = ExecutorContext({ dispatch(it) }, { publish(it) }) { state() }
                    handleSendMessage(state().inputText, state(), ctx) { block -> launch { block() } }
                }
                onIntent<ChatIntent.ClearChat> {
                    dispatch(Msg.MessagesUpdated(emptyList()))
                    dispatch(Msg.MetricsUpdated(emptyList(), 0))
                    dispatch(Msg.ErrorChanged(null))
                }
                onIntent<ChatIntent.UpdateSettings> { dispatch(Msg.SettingsUpdated(it.settings)) }
                onIntent<ChatIntent.ToggleSettings> { dispatch(Msg.SettingsToggled(it.show)) }
                onIntent<ChatIntent.ToggleMetrics> { dispatch(Msg.MetricsToggled(it.show)) }
                onIntent<ChatIntent.ToggleReasoning> { dispatch(Msg.ReasoningToggled(it.show)) }
                onIntent<ChatIntent.RunReasoningComparison> {
                    val ctx = ExecutorContext({ dispatch(it) }, { publish(it) }) { state() }
                    dispatch(Msg.ReasoningLoadingChanged(true))
                    launch { handleReasoningComparison(it.task, state().settings, ctx) }
                }
                onIntent<ChatIntent.MessageSent> {
                    val ctx = ExecutorContext({ dispatch(it) }, { publish(it) }) { state() }
                    handleMessageSent(it.response, it.metric, state(), ctx)
                }
                onIntent<ChatIntent.MessageSendFailed> {
                    dispatch(Msg.LoadingChanged(false))
                    dispatch(Msg.ErrorChanged("Failed to send message"))
                    publish(ChatLabel.ShowToast("Failed to send message"))
                }
                onIntent<ChatIntent.SetError> { dispatch(Msg.ErrorChanged(it.message)) }
                onIntent<ChatIntent.ClearError> { dispatch(Msg.ErrorChanged(null)) }
                onIntent<ChatIntent.SetLoading> { dispatch(Msg.LoadingChanged(it.loading)) }
                onIntent<ChatIntent.UpdateReasoningComparison> {
                    dispatch(Msg.ReasoningComparisonUpdated(it.comparison))
                }
                onIntent<ChatIntent.SetReasoningLoading> { dispatch(Msg.ReasoningLoadingChanged(it.loading)) }
            },
            reducer = MessageReducer
        ) {}

    private object MessageReducer : Reducer<ChatState, Msg> {
        override fun ChatState.reduce(message: Msg): ChatState = when (message) {
            is Msg.InputTextChanged -> copy(inputText = message.text)

            is Msg.MessagesUpdated -> copy(messages = message.messages)

            is Msg.LoadingChanged -> copy(isLoading = message.isLoading)

            is Msg.ErrorChanged -> copy(errorMessage = message.error)

            is Msg.SettingsToggled -> copy(showSettings = message.show)

            is Msg.MetricsToggled -> copy(showMetrics = message.show)

            is Msg.ReasoningToggled -> copy(showReasoning = message.show)

            is Msg.MetricsUpdated -> copy(
                metrics = message.metrics,
                metricCounter = message.counter
            )

            is Msg.SettingsUpdated -> copy(settings = message.settings)

            is Msg.ReasoningComparisonUpdated -> copy(
                reasoningComparison = message.comparison ?: reasoningComparison
            )

            is Msg.ReasoningLoadingChanged -> copy(isReasoningLoading = message.isLoading)
        }
    }
}
