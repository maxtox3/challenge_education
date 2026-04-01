package agent

data class AgentMetrics(
    val responseTimeMs: Long = 0,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val contextTokens: Int = 0,
    val compressedTokens: Int? = null,
    val totalRequests: Int = 0
) {
    val totalTokens: Int get() = inputTokens + outputTokens
    val tokensSaved: Int? get() = compressedTokens?.let { contextTokens - it }
    val averageTokensPerRequest: Float get() =
        if (totalRequests > 0) totalTokens.toFloat() / totalRequests else 0f

    fun addResponse(outputTokens: Int, responseTimeMs: Long): AgentMetrics = copy(
        outputTokens = this.outputTokens + outputTokens,
        totalRequests = totalRequests + 1,
        responseTimeMs = responseTimeMs
    )
}
