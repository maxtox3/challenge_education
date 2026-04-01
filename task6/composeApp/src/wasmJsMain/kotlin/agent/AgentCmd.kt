package agent

import model.ChatMessage

sealed class AgentCmd {
    data class CallApi(val prompt: String, val context: AgentContext, val config: AgentConfig) : AgentCmd()
    data class StreamApi(val prompt: String, val context: AgentContext, val config: AgentConfig) : AgentCmd()
    data class SaveContext(val context: AgentContext) : AgentCmd()
    data object LoadContext : AgentCmd()
    data object ClearStorage : AgentCmd()
    data class CompressWithLlm(val messages: List<ChatMessage>, val keepLastN: Int) : AgentCmd()
}
