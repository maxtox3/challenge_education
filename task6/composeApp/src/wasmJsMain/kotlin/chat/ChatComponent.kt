package chat

import agent.AgentState
import com.arkivanov.decompose.value.Value

interface ChatComponent {
    val state: Value<AgentState>

    fun accept(intent: ChatIntent)
}
