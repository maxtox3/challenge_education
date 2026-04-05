package settings

import com.arkivanov.decompose.value.Value

interface SettingsComponent {
    val state: Value<SettingsState>

    fun accept(intent: SettingsIntent)
}
