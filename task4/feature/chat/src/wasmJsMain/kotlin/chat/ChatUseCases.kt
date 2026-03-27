package chat

import core.exception.ChatException
import core.exception.ChatNetworkException
import core.exception.ChatStreamingException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import model.ChatMessage
import model.ReasoningComparison
import model.StreamChunk
import settings.ApiSettings

/**
 * Encapsulates business logic for chat operations.
 * Extracted from ChatViewModel to reduce function count and complexity.
 */
class ChatUseCases(private val repository: ChatRepository, private val viewModelScope: CoroutineScope) {
    /**
     * Configuration for message sending operations.
     */
    data class SendMessageConfig(
        val inputText: String,
        val isLoading: Boolean,
        val currentMessages: List<ChatMessage>,
        val settings: ApiSettings,
    )

    /**
     * Callbacks for message operations.
     */
    data class MessageCallbacks(
        val onMessagesUpdate: (List<ChatMessage>) -> Unit,
        val onInputTextUpdate: (String) -> Unit,
        val onLoadingUpdate: (Boolean) -> Unit,
        val onErrorUpdate: (String?) -> Unit,
    )

    /**
     * Sends a message with streaming support.
     * Handles the complete flow: validation, streaming updates, and finalization.
     */
    fun sendMessage(config: SendMessageConfig, callbacks: MessageCallbacks) {
        when (val result = MessageHandler.validateAndPrepare(config.inputText, config.isLoading)) {
            is MessageHandler.ValidationResult.Valid -> {
                callbacks.onMessagesUpdate(config.currentMessages + result.message)
                callbacks.onInputTextUpdate("")
                callbacks.onLoadingUpdate(true)
                callbacks.onErrorUpdate(null)

                viewModelScope.launch {
                    executeStreaming(
                        prompt = result.prompt,
                        messages = config.currentMessages + result.message,
                        settings = config.settings,
                        callbacks = callbacks,
                    )
                }
            }

            is MessageHandler.ValidationResult.Invalid -> {
                // Do nothing for invalid input
            }
        }
    }

    private data class StreamingContext(
        val messages: List<ChatMessage>,
        val model: String,
        val onMessagesUpdate: (List<ChatMessage>) -> Unit,
    )

    private suspend fun executeStreaming(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings,
        callbacks: MessageCallbacks,
    ) {
        val context = StreamingContext(messages, settings.model, callbacks.onMessagesUpdate)
        val currentContent = StringBuilder()
        val currentReasoning = StringBuilder()

        try {
            repository.sendMessageStreaming(
                prompt = prompt,
                messages = messages,
                settings = settings,
            ).collect { chunk ->
                when (chunk) {
                    is StreamChunk.Content -> {
                        currentContent.append(chunk.text)
                        updateStreamingMessage(context, currentContent.toString(), false)
                    }

                    is StreamChunk.Reasoning -> {
                        currentReasoning.append(chunk.text)
                        updateStreamingMessage(context, currentReasoning.toString(), true)
                    }

                    is StreamChunk.Done -> {
                        val finalContent = currentContent.toString()
                            .ifEmpty { currentReasoning.toString() }
                        finalizeStreamingMessage(
                            context,
                            finalContent,
                            currentContent.isEmpty() && currentReasoning.isNotEmpty(),
                        )
                        callbacks.onLoadingUpdate(false)
                    }
                }
            }
        } catch (e: ChatException) {
            handleStreamingError(e, callbacks)
        }
    }

    private fun handleStreamingError(e: ChatException, callbacks: MessageCallbacks) {
        val message = when (e) {
            is ChatStreamingException -> "Streaming error"
            is ChatNetworkException -> "Network error"
            else -> "Chat error"
        }
        callbacks.onErrorUpdate(e.message ?: message)
        callbacks.onLoadingUpdate(false)
    }

    private fun updateStreamingMessage(context: StreamingContext, content: String, isReasoning: Boolean,) {
        val streamingMessage = ChatMessage(
            role = "assistant",
            content = content,
            isReasoningContent = isReasoning,
            isStreaming = true,
            model = context.model,
        )
        val updatedMessages = if (context.messages.isNotEmpty() && context.messages.last().isStreaming) {
            context.messages.dropLast(1) + streamingMessage
        } else {
            context.messages + streamingMessage
        }
        context.onMessagesUpdate(updatedMessages)
    }

    private fun finalizeStreamingMessage(context: StreamingContext, content: String, isReasoning: Boolean,) {
        val finalMessage = ChatMessage(
            role = "assistant",
            content = content,
            isReasoningContent = isReasoning,
            isStreaming = false,
            model = context.model,
        )
        val updatedMessages = if (context.messages.isNotEmpty() && context.messages.last().isStreaming) {
            context.messages.dropLast(1) + finalMessage
        } else {
            context.messages + finalMessage
        }
        context.onMessagesUpdate(updatedMessages)
    }

    /**
     * Runs reasoning comparison for different modes.
     */
    fun runReasoningComparison(
        task: String,
        settings: ApiSettings,
        onComparisonUpdate: (ReasoningComparison) -> Unit,
        onLoadingUpdate: (Boolean) -> Unit,
    ) {
        onLoadingUpdate(true)

        viewModelScope.launch {
            repository.runReasoningComparison(
                task = task,
                settings = settings,
                onProgress = { comparison ->
                    onComparisonUpdate(comparison)
                    onLoadingUpdate(comparison.results.values.any { it.isLoading })
                },
            )
        }
    }
}
