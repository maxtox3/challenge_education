import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import model.ChatMessage
import model.Message
import model.ResponseFormat
import model.ZAiErrorResponse
import model.ZAiRequest
import model.ZAiResponse

data class ResponseConstraints(
    val maxTokens: Int? = null,
    val stop: List<String>? = null,
    val responseFormat: ResponseFormat? = null,
    val temperature: Double? = null,
)

class ChatClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val baseUrl = "https://api.z.ai/api/coding/paas/v4/chat/completions"

    suspend fun sendMessage(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        constraints: ResponseConstraints = ResponseConstraints(),
        systemPrompt: String? = null,
    ): Result<ChatMessage> {
        return try {
            val allMessages = buildList {
                systemPrompt?.let { prompt ->
                    if (prompt.isNotBlank()) {
                        add(Message("system", prompt))
                    }
                }
                addAll(messages.map { Message(it.role, it.content) })
            }
            val request = ZAiRequest(
                model = model,
                messages = allMessages,
                maxTokens = constraints.maxTokens,
                stop = constraints.stop,
                responseFormat = constraints.responseFormat,
                temperature = constraints.temperature ?: 1.0,
            )

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

            if (responseBody.contains("\"error\"")) {
                val errorResponse = json.decodeFromString<ZAiErrorResponse>(responseBody)
                println("[ChatClient] API Error: $errorResponse")
                return Result.failure(
                    Exception(
                        "API Error ${errorResponse.error.code}: ${errorResponse.error.message ?: "Unknown error"}",
                    ),
                )
            }

            val zaiResponse = json.decodeFromString<ZAiResponse>(responseBody)

            if (zaiResponse.choices.isEmpty()) {
                println("[ChatClient] Empty choices in response")
                return Result.failure(Exception("Empty response from API"))
            }

            val choice = zaiResponse.choices.first()
            val isReasoning = choice.message.content.isEmpty() && !choice.message.reasoningContent.isNullOrEmpty()
            val content = choice.message.content.ifEmpty { choice.message.reasoningContent ?: "" }
            val finishReason = choice.finishReason
            val tokensUsed = zaiResponse.usage?.completionTokens

            val mode = if (constraints.maxTokens != null || constraints.stop != null) "constrained" else "free"

            println("[ChatClient] Success, response length: ${content.length}")

            Result.success(
                ChatMessage(
                    role = "assistant",
                    content = content,
                    mode = mode,
                    tokensUsed = tokensUsed,
                    maxTokens = constraints.maxTokens,
                    finishReason = finishReason,
                    isReasoningContent = isReasoning,
                ),
            )
        } catch (e: Exception) {
            println("[ChatClient] Exception: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
