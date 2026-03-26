import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import core.util.toggleBoolean
import core.util.typedProp
import core.util.updateNested
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import model.ResponseFormat
import network.ResponseConstraints

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

data class SettingsState(
    val settings: ApiSettings = ApiSettings(),
    val isLoading: Boolean = false,
    val validationError: String? = null,
    val isApiKeyVisible: Boolean = false,
)

sealed class SettingsIntent {
    data class UpdateApiKey(val apiKey: String) : SettingsIntent()
    data class UpdateModel(val model: String) : SettingsIntent()
    data class UpdateMaxTokens(val maxTokens: Int?) : SettingsIntent()
    data class UpdateTemperature(val temperature: Double) : SettingsIntent()
    data class UpdateStopSequences(val stopSequences: String) : SettingsIntent()
    data class UpdateResponseFormat(val responseFormat: String) : SettingsIntent()
    data class UpdateSettings(val settings: ApiSettings) : SettingsIntent()
    data object SaveSettings : SettingsIntent()
    data object ResetSettings : SettingsIntent()
    data object ClearValidationError : SettingsIntent()
    data object ToggleApiKeyVisibility : SettingsIntent()
    data class ValidateApiKey(val apiKey: String) : SettingsIntent()
}

sealed class SettingsSideEffect {
    data class ShowToast(val message: String) : SettingsSideEffect()
    data object SettingsSaved : SettingsSideEffect()
    data class ValidationError(val error: String) : SettingsSideEffect()
}

class SettingsViewModel(
    private val viewModelScope: CoroutineScope,
    private val onSettingsSaved: (ApiSettings) -> Unit = {},
) {
    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    private val _sideEffects = MutableSharedFlow<SettingsSideEffect>()
    val sideEffects: SharedFlow<SettingsSideEffect> = _sideEffects.asSharedFlow()

    val state: SettingsState get() = _uiState.value

    var settings: ApiSettings by _uiState.typedProp(
        getter = { it.settings },
        setter = { state, value -> state.copy(settings = value) }
    )

    var isLoading: Boolean by _uiState.typedProp(
        getter = { it.isLoading },
        setter = { state, value -> state.copy(isLoading = value) }
    )

    var validationError: String? by _uiState.typedProp(
        getter = { it.validationError },
        setter = { state, value -> state.copy(validationError = value) }
    )

    var isApiKeyVisible: Boolean by _uiState.typedProp(
        getter = { it.isApiKeyVisible },
        setter = { state, value -> state.copy(isApiKeyVisible = value) }
    )

    fun processIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.UpdateApiKey -> updateSettingsField { it.copy(apiKey = intent.apiKey) }

            is SettingsIntent.UpdateModel -> updateSettingsField { it.copy(model = intent.model) }

            is SettingsIntent.UpdateMaxTokens -> updateSettingsField { it.copy(maxTokens = intent.maxTokens) }

            is SettingsIntent.UpdateTemperature -> updateSettingsField { it.copy(temperature = intent.temperature) }

            is SettingsIntent.UpdateStopSequences -> updateSettingsField {
                it.copy(stopSequences = intent.stopSequences)
            }

            is SettingsIntent.UpdateResponseFormat -> updateSettingsField {
                it.copy(responseFormat = intent.responseFormat)
            }

            is SettingsIntent.UpdateSettings -> updateSettings(intent.settings)

            is SettingsIntent.SaveSettings -> saveSettings()

            is SettingsIntent.ResetSettings -> resetSettings()

            is SettingsIntent.ClearValidationError -> clearValidationError()

            is SettingsIntent.ToggleApiKeyVisibility -> toggleApiKeyVisibility()

            is SettingsIntent.ValidateApiKey -> validateApiKey(intent.apiKey)
        }
    }

    private inline fun updateSettingsField(noinline block: (ApiSettings) -> ApiSettings) {
        _uiState.updateNested(
            nestedGetter = { it.settings },
            nestedSetter = { state, settings -> state.copy(settings = settings) },
            block = block
        )
    }

    fun updateSettings(newSettings: ApiSettings) {
        _uiState.update { it.copy(settings = newSettings) }
    }

    private fun saveSettings() {
        val currentSettings = _uiState.value.settings
        if (validateSettings(currentSettings)) {
            onSettingsSaved(currentSettings)
            viewModelScope.launch {
                _sideEffects.emit(SettingsSideEffect.SettingsSaved)
            }
        }
    }

    private fun resetSettings() {
        _uiState.update { it.copy(settings = ApiSettings(), validationError = null) }
    }

    private fun clearValidationError() {
        _uiState.update { it.copy(validationError = null) }
    }

    private fun toggleApiKeyVisibility() {
        _uiState.toggleBoolean(
            getter = { it.isApiKeyVisible },
            setter = { state, value -> state.copy(isApiKeyVisible = value) }
        )
    }

    private fun validateApiKey(apiKey: String) {
        val error = if (apiKey.isBlank()) "API key cannot be empty" else null
        _uiState.update { it.copy(validationError = error) }
    }

    private fun validateSettings(settings: ApiSettings): Boolean {
        val error = when {
            settings.apiKey.isBlank() -> "API key is required"
            settings.temperature !in 0.0..2.0 -> "Temperature must be between 0.0 and 2.0"
            else -> null
        }
        _uiState.update { it.copy(validationError = error) }
        return error == null
    }

    fun loadSettings(settings: ApiSettings) {
        _uiState.update { it.copy(settings = settings) }
    }
}

@Composable
fun rememberSettingsViewModel(onSettingsSaved: (ApiSettings) -> Unit = {},): SettingsViewModel {
    val scope = rememberCoroutineScope()
    return remember(scope, onSettingsSaved) {
        SettingsViewModel(scope, onSettingsSaved)
    }
}
