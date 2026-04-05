package settings

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value

class DefaultSettingsComponent(
    private val initialSettings: ApiSettings = ApiSettings(),
    private val onSettingsSaved: () -> Unit = {},
) : SettingsComponent {

    private val _state: MutableValue<SettingsState> = MutableValue(SettingsState(settings = initialSettings))

    override val state: Value<SettingsState> = _state

    override fun accept(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.UpdateApiKey -> {
                _state.value = _state.value.copy(
                    settings = _state.value.settings.copy(apiKey = intent.apiKey)
                )
            }

            is SettingsIntent.UpdateModel -> {
                _state.value = _state.value.copy(
                    settings = _state.value.settings.copy(model = intent.model)
                )
            }

            is SettingsIntent.UpdateMaxTokens -> {
                _state.value = _state.value.copy(
                    settings = _state.value.settings.copy(maxTokens = intent.maxTokens)
                )
            }

            is SettingsIntent.UpdateTemperature -> {
                _state.value = _state.value.copy(
                    settings = _state.value.settings.copy(temperature = intent.temperature)
                )
            }

            is SettingsIntent.UpdateStopSequences -> {
                _state.value = _state.value.copy(
                    settings = _state.value.settings.copy(stopSequences = intent.stopSequences)
                )
            }

            is SettingsIntent.UpdateResponseFormat -> {
                _state.value = _state.value.copy(
                    settings = _state.value.settings.copy(responseFormat = intent.responseFormat)
                )
            }

            is SettingsIntent.UpdateSettings -> {
                _state.value = _state.value.copy(settings = intent.settings)
            }

            is SettingsIntent.SaveSettings -> {
                onSettingsSaved()
            }

            is SettingsIntent.ResetSettings -> {
                _state.value = SettingsState(settings = ApiSettings())
            }

            is SettingsIntent.ClearValidationError -> {
                _state.value = _state.value.copy(validationError = null)
            }

            is SettingsIntent.ToggleApiKeyVisibility -> {
                _state.value = _state.value.copy(isApiKeyVisible = !_state.value.isApiKeyVisible)
            }

            is SettingsIntent.ValidateApiKey -> {
                val error = if (intent.apiKey.isBlank()) "API key cannot be empty" else null
                _state.value = _state.value.copy(validationError = error)
            }
        }
    }
}
