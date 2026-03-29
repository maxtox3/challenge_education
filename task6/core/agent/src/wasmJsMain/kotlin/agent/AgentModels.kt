package agent

import model.ChatMessage

data class AgentRequest(val prompt: String, val context: AgentContext?, val config: AgentConfig?,)

data class AgentContext(val messages: List<ChatMessage>,)

data class AgentConfig(val model: String, val temperature: Float?, val maxTokens: Int?,)

data class AgentMetrics(val responseTimeMs: Long, val inputTokens: Int?, val outputTokens: Int?,)

sealed class AgentResult {
    data class Success(val response: String, val metrics: AgentMetrics?,) : AgentResult()

    data object Loading : AgentResult()

    data class Error(val message: String, val cause: Throwable? = null,) : AgentResult()
}
