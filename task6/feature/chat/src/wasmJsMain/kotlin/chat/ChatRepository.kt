package chat

import core.exception.ChatException
import kotlinx.coroutines.flow.Flow
import model.ChatMessage
import model.StreamChunk
import network.ChatClient
import settings.ApiSettings

interface ChatRepository {
    suspend fun sendMessage(prompt: String, messages: List<ChatMessage>, settings: ApiSettings): SendMessageResult

    fun sendMessageStreaming(prompt: String, messages: List<ChatMessage>, settings: ApiSettings): Flow<StreamChunk>
}

sealed class SendMessageResult {
    data class Success(val response: ChatMessage) : SendMessageResult()

    data class Error(val message: String) : SendMessageResult()
}

class ChatRepositoryImpl(private val client: ChatClient) : ChatRepository {

    override suspend fun sendMessage(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings,
    ): SendMessageResult {
        val constraints = settings.toResponseConstraints()

        return try {
            val result = client.sendMessage(
                apiKey = settings.apiKey,
                provider = settings.provider,
                model = settings.model,
                messages = messages,
                constraints = constraints,
            )

            result.fold(
                onSuccess = { response ->
                    SendMessageResult.Success(response)
                },
                onFailure = { error ->
                    SendMessageResult.Error(error.message ?: "Unknown error")
                },
            )
        } catch (e: ChatException) {
            SendMessageResult.Error(e.message ?: "Unknown error")
        }
    }

    override fun sendMessageStreaming(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings,
    ): Flow<StreamChunk> {
        val constraints = settings.toResponseConstraints()
        return client.sendMessageStreaming(
            apiKey = settings.apiKey,
            provider = settings.provider,
            model = settings.model,
            messages = messages,
            constraints = constraints,
        )
    }
}
