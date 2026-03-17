package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZAiRequest(
    val model: String = "glm-5",
    val messages: List<Message>,
    val stream: Boolean = false,
    val temperature: Double = 1.0,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    val stop: List<String>? = null,
    @SerialName("response_format")
    val responseFormat: ResponseFormat? = null,
)

@Serializable
data class ResponseFormat(val type: String)

@Serializable
data class Message(val role: String, val content: String)

@Serializable
data class ZAiResponse(val id: String? = null, val choices: List<Choice>, val usage: Usage? = null)

@Serializable
data class Choice(
    val message: Message,
    @SerialName("finish_reason")
    val finishReason: String? = null,
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int,
)

@Serializable
data class ZAiErrorResponse(val error: ZAiError)

@Serializable
data class ZAiError(val code: String, val message: String? = null)
