package chat.domain

import model.ChatMessage

interface GetChatHistoryUseCase {
    suspend operator fun invoke(): List<ChatMessage>
}
