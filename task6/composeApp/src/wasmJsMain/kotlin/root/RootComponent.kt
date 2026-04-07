package root

import chat.ChatComponent
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import settings.SettingsComponent

interface RootComponent {
    val chatComponent: ChatComponent
    val settingsSlot: Value<ChildSlot<SlotConfig, SettingsChild>>

    @Serializable
    sealed class SlotConfig {
        data object Settings : SlotConfig()
    }

    sealed class SettingsChild {
        data class Settings(val component: SettingsComponent) : SettingsChild()
    }

    fun showSettings()
    fun dismissSettings()
}
