package agent

import kotlinx.coroutines.flow.Flow
import model.StreamChunk

interface Agent {
    suspend fun process(request: AgentRequest, onChunk: (StreamChunk) -> Unit = {},): AgentResult

    fun processStreaming(request: AgentRequest): Flow<StreamChunk>
}
