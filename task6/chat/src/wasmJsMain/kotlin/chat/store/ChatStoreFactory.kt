package chat.store

import chat.contract.ChatContract
import chat.domain.GetChatHistoryUseCase
import chat.domain.SendMessageResult
import chat.domain.SendMessageUseCase
import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.arkivanov.mvikotlin.extensions.coroutines.coroutineExecutorFactory
import kotlinx.coroutines.launch
import model.ChatMessage
import settings.ApiSettings

class ChatStoreFactory(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val storeFactory: StoreFactory,
) {
    fun create(): ChatStore = object :
        ChatStore,
        Store<ChatContract.Intent, ChatContract.State, ChatContract.Label> by storeFactory.create(
            name = "ChatStore",
            initialState = ChatContract.State(),
            executorFactory = coroutineExecutorFactory {
                ChatExecutor(sendMessageUseCase, getChatHistoryUseCase)
            },
            reducer = ChatReducer,
        ) {}
}

private sealed class Message {
    data class SendMessage(val content: String) : Message()
    data class UpdateInput(val text: String) : Message()
    data object ClearHistory : Message()
    data class UpdateSettings(val settings: ApiSettings) : Message()
    data object DismissError : Message()
    data class MessageSent(val response: ChatMessage) : Message()
    data class MessageSendFailed(val error: String) : Message()
    data object SetLoading : Message()
    data object ClearLoading : Message()
}

private class ChatExecutor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
) : CoroutineExecutor<ChatContract.Intent, Message, ChatContract.State, Message, ChatContract.Label>() {
    override fun executeIntent(intent: ChatContract.Intent): Unit = when (intent) {
        is ChatContract.Intent.SendMessage -> {
            dispatch(Message.SetLoading)
            scope.launch {
                val result = sendMessageUseCase(
                    prompt = intent.content,
                    messages = state().messages,
                    settings = state().settings,
                )
                when (result) {
                    is SendMessageResult.Success -> {
                        dispatch(Message.MessageSent(result.response))
                        dispatch(Message.ClearLoading)
                        publish(ChatContract.Label.ScrollToBottom)
                    }

                    is SendMessageResult.Error -> {
                        dispatch(Message.MessageSendFailed(result.message))
                        dispatch(Message.ClearLoading)
                        publish(ChatContract.Label.ShowError(result.message))
                    }
                }
            }
            Unit
        }

        is ChatContract.Intent.UpdateInput -> {
            dispatch(Message.UpdateInput(intent.text))
        }

        is ChatContract.Intent.ClearHistory -> {
            dispatch(Message.ClearHistory)
        }

        is ChatContract.Intent.UpdateSettings -> {
            dispatch(Message.UpdateSettings(intent.settings))
        }

        is ChatContract.Intent.DismissError -> {
            dispatch(Message.DismissError)
        }
    }
}

private object ChatReducer : Reducer<ChatContract.State, Message> {
    override fun ChatContract.State.reduce(message: Message): ChatContract.State = when (message) {
        is Message.SendMessage -> this
        is Message.UpdateInput -> copy(inputText = message.text)
        is Message.ClearHistory -> copy(messages = emptyList())
        is Message.UpdateSettings -> copy(settings = message.settings)
        is Message.DismissError -> copy(errorMessage = null)
        is Message.MessageSent -> copy(messages = messages + message.response)
        is Message.MessageSendFailed -> copy(errorMessage = message.error)
        is Message.SetLoading -> copy(isLoading = true)
        is Message.ClearLoading -> copy(isLoading = false)
    }
}
