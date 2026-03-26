package model

data class MetricRecord(
    val id: Int,
    val prompt: String,
    val response: String,
    val mode: String,
    val responseLength: Int,
    val tokensUsed: Int?,
    val maxTokens: Int?,
    val finishReason: String?,
    val responseTimeMs: Long,
    val constraints: ConstraintsInfo,
)

data class ConstraintsInfo(
    val maxTokens: Int?,
    val stopSequences: List<String>,
    val responseFormat: String,
    val temperature: Double,
) {
    fun toDisplayString(): String {
        val parts = mutableListOf<String>()
        if (maxTokens != null) parts.add("maxTokens=$maxTokens")
        if (stopSequences.isNotEmpty()) parts.add("stop=${stopSequences.joinToString(",")}")
        if (responseFormat != "text") parts.add("format=$responseFormat")
        if (temperature != 1.0) parts.add("temp=$temperature")
        return if (parts.isEmpty()) "Free" else parts.joinToString(", ")
    }
}
