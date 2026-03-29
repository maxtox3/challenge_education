package agent

import core.exception.ChatException
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import model.ChatMessage
import model.StreamChunk
import network.ChatClient
import network.ResponseConstraints

class SimpleAgent(private val chatClient: ChatClient, private val apiKey: String,) : Agent {

    override suspend fun process(request: AgentRequest, onChunk: (StreamChunk) -> Unit,): AgentResult {
        val responseBuilder = StringBuilder()
        var error: Throwable? = null
        val startTime = getTimeMillis()

        try {
            val messages = buildMessages(request)
            val constraints = buildConstraints(request.config)
            val model = request.config?.model ?: "glm-5"

            chatClient.sendMessageStreaming(
                apiKey = apiKey,
                model = model,
                messages = messages,
                constraints = constraints,
            ).collect { chunk ->
                onChunk(chunk)
                when (chunk) {
                    is StreamChunk.Content -> responseBuilder.append(chunk.text)
                    is StreamChunk.Reasoning -> Unit
                    StreamChunk.Done -> Unit
                }
            }
        } catch (e: ChatException) {
            error = e
        } catch (e: IllegalStateException) {
            error = e
        } catch (e: IllegalArgumentException) {
            error = e
        }

        val responseTimeMs = getTimeMillis() - startTime

        return if (error != null) {
            AgentResult.Error(
                message = error.message ?: "Unknown error",
                cause = error,
            )
        } else {
            AgentResult.Success(
                response = responseBuilder.toString(),
                metrics = AgentMetrics(
                    responseTimeMs = responseTimeMs,
                    inputTokens = null,
                    outputTokens = null,
                ),
            )
        }
    }

    override fun processStreaming(request: AgentRequest): Flow<StreamChunk> {
        val messages = buildMessages(request)
        val constraints = buildConstraints(request.config)
        val model = request.config?.model ?: "glm-5"

        return chatClient.sendMessageStreaming(
            apiKey = apiKey,
            model = model,
            messages = messages,
            constraints = constraints,
        ).catch { e ->
            emit(StreamChunk.Done)
        }
    }

    private fun buildMessages(request: AgentRequest): List<ChatMessage> {
        val contextMessages = request.context?.messages ?: emptyList()
        return contextMessages + ChatMessage(
            role = "user",
            content = request.prompt,
        )
    }

    private fun buildConstraints(config: AgentConfig?): ResponseConstraints = ResponseConstraints(
        temperature = config?.temperature?.toDouble(),
        maxTokens = config?.maxTokens,
    )
}
