package model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
    val timestamp: Long = 0L,
    val mode: String = "free",
    val tokensUsed: Int? = null,
    val maxTokens: Int? = null,
    val finishReason: String? = null,
    val isReasoningContent: Boolean = false,
    val isStreaming: Boolean = false,
    val model: String? = null,
    val reasoningContent: String? = null,
)
