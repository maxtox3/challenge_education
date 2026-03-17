package model

data class ChatMessage(
    val role: String,
    val content: String,
    val timestamp: Long = 0L
)
