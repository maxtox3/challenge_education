package model

import kotlinx.serialization.Serializable

@Serializable
enum class ApiProvider(val id: String, val displayName: String) {
    ZAI("zai", "Z.ai"),
    OPENROUTER("openrouter", "OpenRouter"),
}
