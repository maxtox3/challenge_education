package network

import kotlinx.coroutines.flow.Flow
import model.ApiProvider
import model.ChatMessage
import model.StreamChunk

@Suppress("LongParameterList")
interface ChatClient {
    suspend fun sendMessage(
        apiKey: String,
        provider: ApiProvider,
        model: String,
        messages: List<ChatMessage>,
        constraints: ResponseConstraints = ResponseConstraints(),
        systemPrompt: String? = null,
    ): Result<ChatMessage>

    fun sendMessageStreaming(
        apiKey: String,
        provider: ApiProvider,
        model: String,
        messages: List<ChatMessage>,
        constraints: ResponseConstraints = ResponseConstraints(),
        systemPrompt: String? = null,
    ): Flow<StreamChunk>
}
