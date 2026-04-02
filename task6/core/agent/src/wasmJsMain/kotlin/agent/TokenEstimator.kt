package agent

import model.ChatMessage

interface TokenEstimator {
    fun estimate(text: String): Int
    fun estimate(messages: List<ChatMessage>): Int
}
