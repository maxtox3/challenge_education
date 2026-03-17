import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.*

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
        messages: List<ChatMessage>
    ): Result<String> {
        return try {
            val request = ZAiRequest(
                model = "glm-5",
                messages = messages.map { Message(it.role, it.content) }
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
                return Result.failure(Exception("API Error ${errorResponse.error.code}: ${errorResponse.error.message ?: "Unknown error"}"))
            }
            
            val zaiResponse = json.decodeFromString<ZAiResponse>(responseBody)
            
            if (zaiResponse.choices.isEmpty()) {
                println("[ChatClient] Empty choices in response")
                return Result.failure(Exception("Empty response from API"))
            }
            
            val content = zaiResponse.choices.first().message.content
            println("[ChatClient] Success, response length: ${content.length}")
            
            Result.success(content)
        } catch (e: Exception) {
            println("[ChatClient] Exception: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
