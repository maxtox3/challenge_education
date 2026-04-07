package chat

import model.ChatMessage
import settings.ApiSettings

data class ChatState(
    val inputText: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val streamingMessage: String? = null,
    val isStreaming: Boolean = false,
    val errorMessage: String? = null,
    val showSettings: Boolean = false,
    val settings: ApiSettings = ApiSettings(),
)
