package agent

import model.ChatMessage

data class AgentState(
    val context: AgentContext = AgentContext(),
    val config: AgentConfig = AgentConfig(),
    val messages: List<ChatMessage> = emptyList(),
    val metrics: AgentMetrics = AgentMetrics(),
    val status: AgentStatus = AgentStatus.Idle,
    val inputText: String = ""
)
