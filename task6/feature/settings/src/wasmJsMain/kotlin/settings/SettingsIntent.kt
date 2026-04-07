package settings

sealed class SettingsIntent {
    data class UpdateApiKey(val apiKey: String) : SettingsIntent()
    data class UpdateProvider(val provider: model.ApiProvider) : SettingsIntent()
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
