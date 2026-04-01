package agent

import kotlinx.serialization.Serializable
import model.ChatMessage

@Serializable
data class AgentContext(
    val messages: List<ChatMessage> = emptyList(),
    val summary: String? = null,
    val tokenCount: Int = 0
) {
    val messageCount: Int get() = messages.size
    val totalTokens: Int get() = tokenCount

    fun addExchange(user: ChatMessage, assistant: ChatMessage): AgentContext = copy(
        messages = messages + user + assistant,
        tokenCount = tokenCount + estimateTokens(user.content) + estimateTokens(assistant.content)
    )

    private fun estimateTokens(text: String): Int = text.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
}
