package agent

import model.StreamChunk

sealed class AgentMsg {
    data class SendMessage(val prompt: String) : AgentMsg()
    data object ClearContext : AgentMsg()
    data class HandleApiResponse(val response: String, val tokens: Int) : AgentMsg()
    data class HandleApiError(val error: String) : AgentMsg()
    data class HandleStreamChunk(val chunk: StreamChunk) : AgentMsg()
    data object LoadContextFromStorage : AgentMsg()
    data class ContextLoaded(val context: AgentContext) : AgentMsg()
    data class ContextSaved(val success: Boolean) : AgentMsg()
    data object CompressHistory : AgentMsg()
    data class HistoryCompressed(val summary: String) : AgentMsg()
    data class UpdateConfig(val config: AgentConfig) : AgentMsg()
}
