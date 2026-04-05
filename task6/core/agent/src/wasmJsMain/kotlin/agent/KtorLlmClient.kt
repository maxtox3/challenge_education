package agent

import kotlinx.coroutines.flow.Flow
import model.ChatMessage
import model.StreamChunk
import network.ChatClient
import network.ResponseConstraints

class KtorLlmClient(private val chatClient: ChatClient) : LlmClient {

    override suspend fun call(
        prompt: String,
        context: AgentContext,
        config: AgentConfig
    ): Result<LlmResponse> {
        val messages = context.messages + ChatMessage(role = "user", content = prompt)
        val constraints = config.toResponseConstraints()

        return chatClient.sendMessage(
            model = config.model,
            messages = messages,
            constraints = constraints
        ).mapCatching { response ->
            LlmResponse(
                content = response.content,
                tokensUsed = response.tokensUsed ?: 0,
                model = response.model ?: config.model
            )
        }
    }

    override fun stream(prompt: String, context: AgentContext, config: AgentConfig): Flow<StreamChunk> {
        val messages = context.messages + ChatMessage(role = "user", content = prompt)
        val constraints = config.toResponseConstraints()

        return chatClient.sendMessageStreaming(
            model = config.model,
            messages = messages,
            constraints = constraints
        )
    }

    private fun AgentConfig.toResponseConstraints(): ResponseConstraints = ResponseConstraints(
        maxTokens = maxTokens,
        temperature = temperature?.toDouble()
    )
}
