import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.SSEBufferPolicy
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.Json
import model.ChatMessage
import model.Message
import model.ResponseFormat
import model.StreamChunk
import model.ZAiErrorResponse
import model.ZAiRequest
import model.ZAiResponse
import model.ZAiStreamChunk

data class ResponseConstraints(
    val maxTokens: Int? = null,
    val stop: List<String>? = null,
    val responseFormat: ResponseFormat? = null,
    val temperature: Double? = null,
)

interface ChatClient {
    suspend fun sendMessage(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        constraints: ResponseConstraints = ResponseConstraints(),
        systemPrompt: String? = null,
    ): Result<ChatMessage>

    fun sendMessageStreaming(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        constraints: ResponseConstraints = ResponseConstraints(),
        systemPrompt: String? = null,
    ): Flow<StreamChunk>
}

class ChatClientImpl : ChatClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(SSE) {
            showCommentEvents()
            showRetryEvents()
            bufferPolicy = SSEBufferPolicy.All
        }
    }

    private val baseUrl = "https://api.z.ai/api/coding/paas/v4/chat/completions"

    private fun buildAllMessages(messages: List<ChatMessage>, systemPrompt: String?,): List<Message> = buildList {
        systemPrompt?.let { prompt ->
            if (prompt.isNotBlank()) {
                add(Message("system", prompt))
            }
        }
        addAll(messages.map { Message(it.role, it.content) })
    }

    private fun buildRequest(
        model: String,
        messages: List<Message>,
        constraints: ResponseConstraints,
        stream: Boolean = false,
    ): ZAiRequest = ZAiRequest(
        model = model,
        messages = messages,
        stream = stream,
        maxTokens = constraints.maxTokens,
        stop = constraints.stop,
        responseFormat = constraints.responseFormat,
        temperature = constraints.temperature ?: 1.0,
    )

    private fun parseSSEEvent(data: String?): StreamChunk? {
        if (data == null || data == "[DONE]") {
            println("[ChatClient] Stream completed")
            return StreamChunk.Done
        }
        return try {
            val streamChunk = json.decodeFromString<ZAiStreamChunk>(data)
            val choice = streamChunk.choices.firstOrNull()
            when {
                choice == null -> null

                !choice.delta.reasoningContent.isNullOrEmpty() ->
                    StreamChunk.Reasoning(choice.delta.reasoningContent)

                !choice.delta.content.isNullOrEmpty() -> {
                    println("[ChatClient] Stream data: ${choice.delta.content}")
                    StreamChunk.Content(choice.delta.content)
                }

                choice.finishReason != null -> {
                    println("[ChatClient] Stream finished with reason: ${choice.finishReason}")
                    StreamChunk.Done
                }

                else -> null
            }
        } catch (e: Exception) {
            println("[ChatClient] Error parsing stream chunk: ${e.message}")
            null
        }
    }

    override suspend fun sendMessage(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        constraints: ResponseConstraints,
        systemPrompt: String?,
    ): Result<ChatMessage> = try {
        val allMessages = buildAllMessages(messages, systemPrompt)
        val request = buildRequest(model, allMessages, constraints)

        val requestBody = json.encodeToString(request)
        println("[ChatClient] Sending request to: $baseUrl")
        println("[ChatClient] Request body: $requestBody")

        val response: HttpResponse = client.post(baseUrl) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $apiKey")
                append(HttpHeaders.AcceptLanguage, "en-US,en")
            }
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        println("[ChatClient] Response status: ${response.status}")
        val responseBody = response.bodyAsText()
        println("[ChatClient] Response body: $responseBody")

        parseResponse(responseBody, constraints)
    } catch (e: Exception) {
        println("[ChatClient] Exception: ${e.message}")
        Result.failure(e)
    }

    private fun parseResponse(responseBody: String, constraints: ResponseConstraints,): Result<ChatMessage> {
        if (responseBody.contains("\"error\"")) {
            val errorResponse = json.decodeFromString<ZAiErrorResponse>(responseBody)
            println("[ChatClient] API Error: $errorResponse")
            val errorMsg = errorResponse.error.message ?: "Unknown error"
            return Result.failure(
                ChatClientException("API Error ${errorResponse.error.code}: $errorMsg"),
            )
        }

        val zaiResponse = json.decodeFromString<ZAiResponse>(responseBody)

        return if (zaiResponse.choices.isEmpty()) {
            println("[ChatClient] Empty choices in response")
            Result.failure(ChatClientException("Empty response from API"))
        } else {
            val choice = zaiResponse.choices.first()
            val isReasoning = choice.message.content.isEmpty() &&
                !choice.message.reasoningContent.isNullOrEmpty()
            val content = choice.message.content.ifEmpty { choice.message.reasoningContent ?: "" }
            val mode = if (constraints.maxTokens != null || constraints.stop != null) {
                "constrained"
            } else {
                "free"
            }

            println("[ChatClient] Success, response length: ${content.length}")

            Result.success(
                ChatMessage(
                    role = "assistant",
                    content = content,
                    mode = mode,
                    tokensUsed = zaiResponse.usage?.completionTokens,
                    maxTokens = constraints.maxTokens,
                    finishReason = choice.finishReason,
                    isReasoningContent = isReasoning,
                ),
            )
        }
    }

    override fun sendMessageStreaming(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        constraints: ResponseConstraints,
        systemPrompt: String?,
    ): Flow<StreamChunk> = channelFlow {
        val allMessages = buildAllMessages(messages, systemPrompt)
        val request = buildRequest(model, allMessages, constraints, stream = true)

        val requestBody = json.encodeToString(request)
        println("[ChatClient] Sending SSE request to: $baseUrl")

        client.sse(
            request = {
                url { takeFrom(baseUrl) }
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                header(HttpHeaders.AcceptLanguage, "en-US,en")
                setBody(requestBody)
            }
        ) {
            println("[ChatClient] SSE connection established")
            incoming.collect { event ->
                parseSSEEvent(event.data)?.let { chunk -> send(chunk) }
            }
        }
    }
}

class ChatClientException(message: String) : Exception(message)
