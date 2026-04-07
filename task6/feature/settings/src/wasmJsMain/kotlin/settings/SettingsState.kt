package settings

import kotlinx.serialization.Serializable
import model.ModelType
import model.ResponseFormat
import network.ResponseConstraints

@Serializable
data class ApiSettings(
    val apiKey: String = "9cccc72cda3c456c9263fe143dbae7b1.9ir3VQrPquSuSyvZ",
    val model: String = ModelType.PRO.id,
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

data class SettingsState(
    val settings: ApiSettings = ApiSettings(),
    val isLoading: Boolean = false,
    val validationError: String? = null,
    val isApiKeyVisible: Boolean = false,
)
