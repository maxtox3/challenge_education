package root

import chat.ChatComponent
import chat.ChatRepository
import chat.DefaultChatComponentFactory
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.mvikotlin.core.store.StoreFactory
import kotlinx.serialization.json.Json
import settings.ApiSettings
import settings.DefaultSettingsComponent
import settings.SettingsComponent
import storage.StorageService

class DefaultRootComponent(
    private val repository: ChatRepository,
    private val chatComponentFactory: DefaultChatComponentFactory,
    private val storeFactory: StoreFactory,
    private val storage: StorageService,
    private val json: Json,
) : RootComponent {

    override val chatComponent: ChatComponent = chatComponentFactory.create(
        repository = repository
    )

    override val settingsComponent: SettingsComponent = DefaultSettingsComponent(
        initialSettings = ApiSettings(),
        storeFactory = storeFactory,
        storage = storage,
        json = json
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
