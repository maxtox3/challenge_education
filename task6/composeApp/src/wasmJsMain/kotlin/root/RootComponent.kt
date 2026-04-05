package root

import chat.ChatComponent
import com.arkivanov.decompose.value.Value
import settings.SettingsComponent

interface RootComponent {
    val chatComponent: ChatComponent
    val settingsComponent: SettingsComponent
    val activeChild: Value<ActiveChild>

    sealed class ActiveChild {
        data class Chat(val component: ChatComponent) : ActiveChild()
        data class Settings(val component: SettingsComponent) : ActiveChild()
    }

    fun showChat()
    fun showSettings()
}
