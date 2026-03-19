import model.ResponseFormat

data class ApiSettings(
    val apiKey: String = "",
    val model: String = "glm-5",
    val maxTokens: Int? = null,
    val temperature: Double = 1.0,
    val stopSequences: String = "",
    val responseFormat: String = "text",
) {
    fun toResponseConstraints(): ResponseConstraints {
        val stopList = stopSequences
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .takeIf { it.isNotEmpty() }

        val format = when (responseFormat) {
            "json" -> ResponseFormat("json_object")
            else -> null
        }

        return ResponseConstraints(
            maxTokens = maxTokens,
            stop = stopList,
            responseFormat = format,
            temperature = temperature,
        )
    }
}
