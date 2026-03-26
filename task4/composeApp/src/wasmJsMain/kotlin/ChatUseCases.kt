import core.exception.ChatException
import core.exception.ChatNetworkException
import core.exception.ChatStreamingException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import model.ChatMessage
import model.ReasoningComparison
import model.StreamChunk

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

    private suspend fun executeStreaming(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings,
        callbacks: MessageCallbacks,
    ) {
        val currentContent = StringBuilder()
        val currentReasoning = StringBuilder()
        var isReasoningContent: Boolean

        try {
            repository.sendMessageStreaming(
                prompt = prompt,
                messages = messages,
                settings = settings,
            ).collect { chunk ->
                when (chunk) {
                    is StreamChunk.Content -> {
                        currentContent.append(chunk.text)
                        isReasoningContent = false
                        updateStreamingMessage(
                            currentMessages = messages,
                            content = currentContent.toString(),
                            reasoning = currentReasoning.toString(),
                            isReasoning = isReasoningContent,
                            onMessagesUpdate = callbacks.onMessagesUpdate,
                        )
                    }

                    is StreamChunk.Reasoning -> {
                        currentReasoning.append(chunk.text)
                        isReasoningContent = true
                        updateStreamingMessage(
                            currentMessages = messages,
                            content = currentContent.toString(),
                            reasoning = currentReasoning.toString(),
                            isReasoning = isReasoningContent,
                            onMessagesUpdate = callbacks.onMessagesUpdate,
                        )
                    }

                    is StreamChunk.Done -> {
                        val finalContent = currentContent.toString()
                            .ifEmpty { currentReasoning.toString() }
                        finalizeStreamingMessage(
                            currentMessages = messages,
                            content = finalContent,
                            isReasoning = currentContent.isEmpty() && currentReasoning.isNotEmpty(),
                            onMessagesUpdate = callbacks.onMessagesUpdate,
                        )
                        callbacks.onLoadingUpdate(false)
                    }
                }
            }
        } catch (e: ChatStreamingException) {
            callbacks.onErrorUpdate(e.message ?: "Streaming error")
            callbacks.onLoadingUpdate(false)
        } catch (e: ChatNetworkException) {
            callbacks.onErrorUpdate(e.message ?: "Network error")
            callbacks.onLoadingUpdate(false)
        } catch (e: ChatException) {
            callbacks.onErrorUpdate(e.message ?: "Chat error")
            callbacks.onLoadingUpdate(false)
        }
    }

    private fun updateStreamingMessage(
        currentMessages: List<ChatMessage>,
        content: String,
        reasoning: String,
        isReasoning: Boolean,
        onMessagesUpdate: (List<ChatMessage>) -> Unit,
    ) {
        val streamingMessage = ChatMessage(
            role = "assistant",
            content = content.ifEmpty { reasoning },
            isReasoningContent = isReasoning,
            isStreaming = true,
        )
        val updatedMessages = if (currentMessages.isNotEmpty() && currentMessages.last().isStreaming) {
            currentMessages.dropLast(1) + streamingMessage
        } else {
            currentMessages + streamingMessage
        }
        onMessagesUpdate(updatedMessages)
    }

    private fun finalizeStreamingMessage(
        currentMessages: List<ChatMessage>,
        content: String,
        isReasoning: Boolean,
        onMessagesUpdate: (List<ChatMessage>) -> Unit,
    ) {
        val finalMessage = ChatMessage(
            role = "assistant",
            content = content,
            isReasoningContent = isReasoning,
            isStreaming = false,
        )
        val updatedMessages = if (currentMessages.isNotEmpty() && currentMessages.last().isStreaming) {
            currentMessages.dropLast(1) + finalMessage
        } else {
            currentMessages + finalMessage
        }
        onMessagesUpdate(updatedMessages)
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
