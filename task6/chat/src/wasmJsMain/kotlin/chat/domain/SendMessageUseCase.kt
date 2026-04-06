package chat.domain

import model.ChatMessage
import settings.ApiSettings

interface SendMessageUseCase {
    suspend operator fun invoke(prompt: String, messages: List<ChatMessage>, settings: ApiSettings,): SendMessageResult
}

sealed class SendMessageResult {
    data class Success(val response: ChatMessage,) : SendMessageResult()

    data class Error(val message: String) : SendMessageResult()
}
