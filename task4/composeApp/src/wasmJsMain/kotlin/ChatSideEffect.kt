sealed class ChatSideEffect {
    data object ScrollToBottom : ChatSideEffect()
    data class ShowToast(val message: String) : ChatSideEffect()
    data object HideKeyboard : ChatSideEffect()
}
