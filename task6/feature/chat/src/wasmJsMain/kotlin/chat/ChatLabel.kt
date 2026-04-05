package chat

sealed class ChatLabel {
    data object ScrollToBottom : ChatLabel()
    data class ShowToast(val message: String) : ChatLabel()
    data object HideKeyboard : ChatLabel()
}
