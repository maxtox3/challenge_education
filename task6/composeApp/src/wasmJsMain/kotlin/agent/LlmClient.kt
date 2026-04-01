package agent

import kotlinx.coroutines.flow.Flow
import model.StreamChunk

interface LlmClient {
    suspend fun call(prompt: String, context: AgentContext, config: AgentConfig): Result<LlmResponse>

    fun stream(prompt: String, context: AgentContext, config: AgentConfig): Flow<StreamChunk>
}

data class LlmResponse(val content: String, val tokensUsed: Int, val model: String)
