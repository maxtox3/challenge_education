package agent

data class AgentConfig(
    val model: String = "glm-5",
    val temperature: Float? = null,
    val maxTokens: Int? = null,
    val enablePersistence: Boolean = false,
    val keepLastN: Int = 10,
    val compressThreshold: Int = 20,
    val enableCompression: Boolean = false
)
