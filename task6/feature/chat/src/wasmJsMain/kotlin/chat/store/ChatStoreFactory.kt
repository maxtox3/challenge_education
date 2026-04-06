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

class ChatStoreFactory(private val storeFactory: StoreFactory, private val repository: ChatRepository) {

    private sealed interface Msg {
        data class InputTextChanged(val text: String) : Msg
        data class MessagesUpdated(val messages: List<ChatMessage>) : Msg
        data class LoadingChanged(val isLoading: Boolean) : Msg
        data class ErrorChanged(val error: String?) : Msg
        data class SettingsToggled(val show: Boolean) : Msg
        data class SettingsUpdated(val settings: ApiSettings) : Msg
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
        println(currentInput)

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
                        messages = updatedMessages,
                        settings = currentState.settings
                    )
                ) {
                    is SendMessageResult.Success -> {
                        val assistantMessage = result.response
                        val finalMessages = updatedMessages + assistantMessage
                        ctx.dispatchFn(Msg.MessagesUpdated(finalMessages))
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

    fun create(): ChatStore = object :
        ChatStore,
        Store<ChatIntent, ChatState, ChatLabel> by storeFactory.create(
            name = "ChatStore",
            initialState = ChatState(),
            executorFactory = coroutineExecutorFactory {
                onIntent<ChatIntent.UpdateInputText> {
                    println("handle intent UpdateInputText")
                    dispatch(Msg.InputTextChanged(it.text))
                }
                onIntent<ChatIntent.SendMessage> {
                    val ctx = ExecutorContext({ dispatch(it) }, { publish(it) }) { state() }
                    handleSendMessage(state().inputText, state(), ctx) { block -> launch { block() } }
                }
                onIntent<ChatIntent.ClearChat> {
                    dispatch(Msg.MessagesUpdated(emptyList()))
                    dispatch(Msg.ErrorChanged(null))
                }
                onIntent<ChatIntent.UpdateSettings> { dispatch(Msg.SettingsUpdated(it.settings)) }
                onIntent<ChatIntent.ToggleSettings> { dispatch(Msg.SettingsToggled(it.show)) }
                onIntent<ChatIntent.SetError> { dispatch(Msg.ErrorChanged(it.message)) }
                onIntent<ChatIntent.ClearError> { dispatch(Msg.ErrorChanged(null)) }
                onIntent<ChatIntent.SetLoading> { dispatch(Msg.LoadingChanged(it.loading)) }
            },
            reducer = MessageReducer
        ) {}

    private object MessageReducer : Reducer<ChatState, Msg> {
        override fun ChatState.reduce(message: Msg): ChatState = when (message) {
            is Msg.InputTextChanged -> {
                println("handle msg UpdateInputText")
                copy(inputText = message.text)
            }

            is Msg.MessagesUpdated -> copy(messages = message.messages)

            is Msg.LoadingChanged -> copy(isLoading = message.isLoading)

            is Msg.ErrorChanged -> copy(errorMessage = message.error)

            is Msg.SettingsToggled -> copy(showSettings = message.show)

            is Msg.SettingsUpdated -> copy(settings = message.settings)
        }
    }
}
