package chat.store

import chat.ChatIntent
import chat.ChatLabel
import chat.ChatRepository
import chat.ChatState
import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.coroutineExecutorFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import model.ChatMessage
import model.StreamChunk
import settings.ApiSettings
import storage.StorageService

class ChatStoreFactory(
    private val storeFactory: StoreFactory,
    private val repository: ChatRepository,
    private val storage: StorageService
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private sealed interface Msg {
        data class InputTextChanged(val text: String) : Msg
        data class MessagesUpdated(val messages: List<ChatMessage>) : Msg
        data class LoadingChanged(val isLoading: Boolean) : Msg
        data class ErrorChanged(val error: String?) : Msg
        data class SettingsToggled(val show: Boolean) : Msg
        data class SettingsUpdated(val settings: ApiSettings) : Msg
        data class StreamingMessageUpdated(val content: String) : Msg
        data object StreamingStarted : Msg
        data object StreamingFinished : Msg
    }

    private class ExecutorContext(
        val dispatchFn: (Msg) -> Unit,
        val publishFn: (ChatLabel) -> Unit,
        val getStateFn: () -> ChatState,
        val launchBlock: (suspend () -> Unit) -> Unit
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
        ctx.dispatchFn(Msg.StreamingStarted)
        ctx.dispatchFn(Msg.ErrorChanged(null))

        launchBlock {
            try {
                storage.setChatHistory(updatedMessages)
            } catch (e: IllegalStateException) {
                println("Failed to save chat history: ${e.message}")
            } catch (e: IllegalArgumentException) {
                println("Failed to save chat history: ${e.message}")
            }
        }

        launchBlock {
            startStreaming(currentInput, updatedMessages, currentState.settings, ctx, updatedMessages)
        }
    }

    private suspend fun startStreaming(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings,
        ctx: ExecutorContext,
        updatedMessages: List<ChatMessage>
    ) {
        repository.sendMessageStreaming(
            prompt = prompt,
            messages = messages,
            settings = settings
        )
            .onEach { chunk ->
                handleStreamChunk(chunk, ctx, updatedMessages)
            }
            .catch { e ->
                if (e !is CancellationException) {
                    ctx.dispatchFn(Msg.StreamingFinished)
                    ctx.dispatchFn(Msg.ErrorChanged(e.message ?: "Streaming failed"))
                    ctx.publishFn(ChatLabel.ShowToast(e.message ?: "Streaming failed"))
                }
            }
            .collect {}
    }

    private fun handleStreamChunk(chunk: StreamChunk, ctx: ExecutorContext, updatedMessages: List<ChatMessage>) {
        when (chunk) {
            is StreamChunk.Content -> {
                val current = ctx.getStateFn().streamingMessage ?: ""
                ctx.dispatchFn(Msg.StreamingMessageUpdated(current + chunk.text))
            }

            is StreamChunk.Reasoning -> {
                val current = ctx.getStateFn().streamingMessage ?: ""
                ctx.dispatchFn(Msg.StreamingMessageUpdated(current + chunk.text))
            }

            is StreamChunk.Done -> {
                handleStreamingComplete(ctx, updatedMessages)
            }
        }
    }

    private fun handleStreamingComplete(ctx: ExecutorContext, updatedMessages: List<ChatMessage>) {
        val finalContent = ctx.getStateFn().streamingMessage ?: ""
        if (finalContent.isNotEmpty()) {
            val finalMessage = ChatMessage(
                role = "assistant",
                content = finalContent
            )
            val finalMessages = updatedMessages + finalMessage
            ctx.dispatchFn(Msg.MessagesUpdated(finalMessages))
            ctx.launchBlock {
                try {
                    storage.setChatHistory(finalMessages)
                } catch (e: IllegalStateException) {
                    println("Failed to save chat history: ${e.message}")
                } catch (e: IllegalArgumentException) {
                    println("Failed to save chat history: ${e.message}")
                }
            }
        }
        ctx.dispatchFn(Msg.StreamingFinished)
        ctx.publishFn(ChatLabel.ScrollToBottom)
    }

    fun create(): ChatStore {
        lateinit var store: ChatStore

        store = object :
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
                        val ctx = ExecutorContext(
                            { dispatch(it) },
                            { publish(it) },
                            { state() },
                            { block -> launch { block() } }
                        )
                        handleSendMessage(state().inputText, state(), ctx) { block -> launch { block() } }
                    }
                    onIntent<ChatIntent.ClearChat> {
                        dispatch(Msg.MessagesUpdated(emptyList()))
                        dispatch(Msg.ErrorChanged(null))
                        launch {
                            try {
                                storage.clearChatHistory()
                            } catch (e: IllegalStateException) {
                                println("Failed to clear chat history: ${e.message}")
                            } catch (e: IllegalArgumentException) {
                                println("Failed to clear chat history: ${e.message}")
                            }
                        }
                    }
                    onIntent<ChatIntent.UpdateMessages> {
                        dispatch(Msg.MessagesUpdated(it.messages))
                    }
                    onIntent<ChatIntent.UpdateSettings> { dispatch(Msg.SettingsUpdated(it.settings)) }
                    onIntent<ChatIntent.ToggleSettings> { dispatch(Msg.SettingsToggled(it.show)) }
                    onIntent<ChatIntent.SetError> { dispatch(Msg.ErrorChanged(it.message)) }
                    onIntent<ChatIntent.ClearError> { dispatch(Msg.ErrorChanged(null)) }
                    onIntent<ChatIntent.SetLoading> { dispatch(Msg.LoadingChanged(it.loading)) }
                },
                reducer = MessageReducer
            ) {}

        scope.launch {
            try {
                val history = storage.getChatHistory()
                store.accept(ChatIntent.UpdateMessages(history))
            } catch (e: IllegalStateException) {
                println("Failed to load chat history: ${e.message}")
            } catch (e: IllegalArgumentException) {
                println("Failed to load chat history: ${e.message}")
            }
        }

        return store
    }

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

            is Msg.StreamingStarted -> copy(
                isStreaming = true,
                streamingMessage = "",
                isLoading = true
            )

            is Msg.StreamingMessageUpdated -> copy(streamingMessage = message.content)

            is Msg.StreamingFinished -> copy(
                isStreaming = false,
                streamingMessage = null,
                isLoading = false
            )
        }
    }
}
