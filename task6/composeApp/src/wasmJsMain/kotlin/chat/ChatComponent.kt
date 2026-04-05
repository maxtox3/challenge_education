package chat

import com.arkivanov.decompose.value.Value

interface ChatComponent {
    val state: Value<ChatState>

    fun accept(intent: ChatIntent)
}
