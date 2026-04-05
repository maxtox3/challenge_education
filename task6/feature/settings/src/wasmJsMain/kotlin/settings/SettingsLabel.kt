package settings

sealed class SettingsLabel {
    data class ShowToast(val message: String) : SettingsLabel()
    data object SettingsSaved : SettingsLabel()
    data class ValidationError(val error: String) : SettingsLabel()
}
