package agent

import network.ChatClient

object AgentFactory {
    fun create(chatClient: ChatClient, apiKey: String): Agent = SimpleAgent(chatClient, apiKey)
}
