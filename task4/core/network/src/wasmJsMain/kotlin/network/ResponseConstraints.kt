package network

import model.ResponseFormat

data class ResponseConstraints(
    val maxTokens: Int? = null,
    val stop: List<String>? = null,
    val responseFormat: ResponseFormat? = null,
    val temperature: Double? = null,
)
