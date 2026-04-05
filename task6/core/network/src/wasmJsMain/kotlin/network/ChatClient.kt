package network

import kotlinx.coroutines.flow.Flow
import model.ChatMessage
import model.StreamChunk

interface ChatClient {
    suspend fun sendMessage(
        model: String,
        messages: List<ChatMessage>,
        constraints: ResponseConstraints = ResponseConstraints(),
        systemPrompt: String? = null,
    ): Result<ChatMessage>

    fun sendMessageStreaming(
        model: String,
        messages: List<ChatMessage>,
        constraints: ResponseConstraints = ResponseConstraints(),
        systemPrompt: String? = null,
    ): Flow<StreamChunk>
}
