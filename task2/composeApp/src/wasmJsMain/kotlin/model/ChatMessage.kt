package model

data class ChatMessage(
    val role: String,
    val content: String,
    val timestamp: Long = 0L,
    val mode: String = "free",
    val tokensUsed: Int? = null,
    val maxTokens: Int? = null,
    val finishReason: String? = null
)
