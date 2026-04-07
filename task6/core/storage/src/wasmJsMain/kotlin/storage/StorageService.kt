package storage

import model.ChatMessage

interface StorageService {
    suspend fun getChatHistory(): List<ChatMessage>
    suspend fun setChatHistory(messages: List<ChatMessage>)
    suspend fun clearChatHistory()
    suspend fun getApiSettings(): String?
    suspend fun setApiSettings(settings: String)
}
