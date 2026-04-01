package agent

sealed class AgentStatus {
    data object Idle : AgentStatus()
    data object Loading : AgentStatus()
    data class Error(val message: String) : AgentStatus()
}
