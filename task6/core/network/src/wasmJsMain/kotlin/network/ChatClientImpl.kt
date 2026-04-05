package network

import core.exception.ChatClientException
import core.exception.ChatException
import core.exception.ChatNetworkException
import core.exception.ChatSerializationException
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import model.ChatMessage
import model.Message
import model.StreamChunk
import model.ZAiErrorResponse
import model.ZAiRequest
import model.ZAiResponse
import model.ZAiStreamChunk

class ChatClientImpl(
    val apiKeyProvider: () -> String
) : ChatClient {
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
            bufferPolicy = SSEBufferPolicy.All
        }
    }

    private val baseUrl = "https://api.z.ai/api/coding/paas/v4/chat/completions"

    private fun buildAllMessages(messages: List<ChatMessage>, systemPrompt: String?): List<Message> = buildList {
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

                !choice.delta.reasoningContent.isNullOrEmpty() -> {
                    val reasoning = choice.delta.reasoningContent
                    if (reasoning != null) StreamChunk.Reasoning(reasoning) else null
                }

                !choice.delta.content.isNullOrEmpty() -> {
                    val content = choice.delta.content
                    println("[ChatClient] Stream data: $content")
                    if (content != null) StreamChunk.Content(content) else null
                }

                choice.finishReason != null -> {
                    println("[ChatClient] Stream finished with reason: ${choice.finishReason}")
                    StreamChunk.Done
                }

                else -> null
            }
        } catch (e: SerializationException) {
            println("[ChatClient] Error parsing stream chunk: ${e.message}")
            null
        }
    }

    override suspend fun sendMessage(
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
                append(HttpHeaders.Authorization, "Bearer ${apiKeyProvider.invoke()}")
                append(HttpHeaders.AcceptLanguage, "en-US,en")
            }
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        println("[ChatClient] Response status: ${response.status}")
        val responseBody = response.bodyAsText()
        println("[ChatClient] Response body: $responseBody")

        parseResponse(responseBody, constraints, model)
    } catch (e: ClientRequestException) {
        println("[ChatClient] Client request error: ${e.message}")
        Result.failure(ChatNetworkException("Client error: ${e.message}", e))
    } catch (e: ServerResponseException) {
        println("[ChatClient] Server error: ${e.message}")
        Result.failure(ChatNetworkException("Server error: ${e.message}", e))
    } catch (e: SerializationException) {
        println("[ChatClient] Serialization error: ${e.message}")
        Result.failure(ChatSerializationException("Failed to parse response: ${e.message}", e))
    } catch (e: ChatException) {
        println("[ChatClient] Chat error: ${e.message}")
        Result.failure(e)
    } catch (e: IllegalStateException) {
        println("[ChatClient] Illegal state: ${e.message}")
        Result.failure(ChatNetworkException("Connection error: ${e.message}", e))
    } catch (e: IllegalArgumentException) {
        println("[ChatClient] Invalid argument: ${e.message}")
        Result.failure(ChatNetworkException("Invalid request: ${e.message}", e))
    }

    private fun parseResponse(
        responseBody: String,
        constraints: ResponseConstraints,
        model: String,
    ): Result<ChatMessage> {
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
                    model = model,
                ),
            )
        }
    }

    override fun sendMessageStreaming(
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
                header(HttpHeaders.Authorization, "Bearer ${apiKeyProvider.invoke()}")
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
