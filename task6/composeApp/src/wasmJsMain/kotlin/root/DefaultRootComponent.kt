package root

import chat.ChatComponent
import chat.ChatRepository
import chat.DefaultChatComponentFactory
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import settings.ApiSettings
import settings.DefaultSettingsComponent
import settings.SettingsComponent

class DefaultRootComponent(
    private val repository: ChatRepository,
    private val chatComponentFactory: DefaultChatComponentFactory,
) : RootComponent {

    override val chatComponent: ChatComponent = chatComponentFactory.create(
        repository = repository
    )
    
    override val settingsComponent: SettingsComponent = DefaultSettingsComponent(
        initialSettings = ApiSettings()
    )

    private val _activeChild: MutableValue<RootComponent.ActiveChild> = MutableValue(
        RootComponent.ActiveChild.Chat(chatComponent)
    )
    override val activeChild: Value<RootComponent.ActiveChild> = _activeChild

    override fun showChat() {
        _activeChild.value = RootComponent.ActiveChild.Chat(chatComponent)
    }

    override fun showSettings() {
        _activeChild.value = RootComponent.ActiveChild.Settings(settingsComponent)
    }
}
