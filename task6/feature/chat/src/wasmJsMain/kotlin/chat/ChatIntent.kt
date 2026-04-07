package chat

import model.ChatMessage
import settings.ApiSettings

sealed class ChatIntent {
    data class UpdateInputText(val text: String) : ChatIntent()
    data object SendMessage : ChatIntent()
    data object ClearChat : ChatIntent()
    data class UpdateSettings(val settings: ApiSettings) : ChatIntent()
    data class ToggleSettings(val show: Boolean) : ChatIntent()
    data class SetError(val message: String?) : ChatIntent()
    data object ClearError : ChatIntent()
    data class SetLoading(val loading: Boolean) : ChatIntent()
    data class UpdateMessages(val messages: List<ChatMessage>) : ChatIntent()
}
