package network

import kotlinx.coroutines.flow.Flow
import model.ChatMessage
import model.StreamChunk

interface ChatClient {
    suspend fun sendMessage(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        constraints: ResponseConstraints = ResponseConstraints(),
        systemPrompt: String? = null,
    ): Result<ChatMessage>

    fun sendMessageStreaming(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        constraints: ResponseConstraints = ResponseConstraints(),
        systemPrompt: String? = null,
    ): Flow<StreamChunk>
}
