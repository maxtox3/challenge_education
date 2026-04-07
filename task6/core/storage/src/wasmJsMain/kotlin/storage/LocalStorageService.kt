package storage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.ChatMessage

external interface Storage {
    fun getItem(key: String): String?
    fun setItem(key: String, value: String)
    fun removeItem(key: String)
}

external val localStorage: Storage

external object console {
    fun error(message: String)
}

class LocalStorageService(
    private val json: Json = Json { ignoreUnknownKeys = true }
) : StorageService {
    
    override suspend fun getChatHistory(): List<ChatMessage> {
        return try {
            val stored = localStorage.getItem(CHAT_HISTORY_KEY)
            if (stored != null) {
                json.decodeFromString(stored)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun setChatHistory(messages: List<ChatMessage>) {
        try {
            val encoded = json.encodeToString(messages)
            localStorage.setItem(CHAT_HISTORY_KEY, encoded)
        } catch (e: Exception) {
            console.error("Failed to save chat history: ${e.message}")
        }
    }
    
    override suspend fun clearChatHistory() {
        localStorage.removeItem(CHAT_HISTORY_KEY)
    }
    
    override suspend fun getApiSettings(): String? {
        return localStorage.getItem(API_SETTINGS_KEY)
    }
    
    override suspend fun setApiSettings(settings: String) {
        try {
            localStorage.setItem(API_SETTINGS_KEY, settings)
        } catch (e: Exception) {
            console.error("Failed to save API settings: ${e.message}")
        }
    }
    
    companion object {
        private const val CHAT_HISTORY_KEY = "chat_history"
        private const val API_SETTINGS_KEY = "api_settings"
    }
}
