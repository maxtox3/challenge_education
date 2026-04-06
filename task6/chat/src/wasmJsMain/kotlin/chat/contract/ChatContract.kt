package chat.contract

import model.ChatMessage
import settings.ApiSettings

object ChatContract {
    data class State(
        val messages: List<ChatMessage> = emptyList(),
        val inputText: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val settings: ApiSettings = ApiSettings(),
    )

    sealed class Intent {
        data class SendMessage(val content: String) : Intent()
        data class UpdateInput(val text: String) : Intent()
        data object ClearHistory : Intent()
        data class UpdateSettings(val settings: ApiSettings) : Intent()
        data object DismissError : Intent()
    }

    sealed class Label {
        data class ShowError(val message: String) : Label()
        data object ScrollToBottom : Label()
        data class ShowToast(val message: String) : Label()
    }
}
