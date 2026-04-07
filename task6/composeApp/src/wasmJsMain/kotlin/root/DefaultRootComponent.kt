package root

import chat.ChatComponent
import chat.ChatRepository
import chat.DefaultChatComponentFactory
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import com.arkivanov.mvikotlin.core.store.StoreFactory
import kotlinx.serialization.json.Json
import settings.ApiSettings
import settings.DefaultSettingsComponent
import storage.StorageService

class DefaultRootComponent(
    private val repository: ChatRepository,
    private val chatComponentFactory: DefaultChatComponentFactory,
    private val storeFactory: StoreFactory,
    private val storage: StorageService,
    private val json: Json,
    componentContext: ComponentContext,
) : RootComponent, ComponentContext by componentContext {

    override val chatComponent: ChatComponent = chatComponentFactory.create(
        repository = repository,
        componentContext = this,
    )

    private val settingsNavigation = SlotNavigation<RootComponent.SlotConfig>()

    override val settingsSlot: Value<ChildSlot<RootComponent.SlotConfig, RootComponent.SettingsChild>> = childSlot(
        source = settingsNavigation,
        serializer = null,
        initialConfiguration = { null },
        handleBackButton = false,
        childFactory = ::createSettingsChild,
    )

    override fun showSettings() {
        settingsNavigation.activate(RootComponent.SlotConfig.Settings)
    }

    override fun dismissSettings() {
        settingsNavigation.dismiss()
    }

    private fun createSettingsChild(
        config: RootComponent.SlotConfig,
        childContext: ComponentContext,
    ): RootComponent.SettingsChild = when (config) {
        RootComponent.SlotConfig.Settings -> RootComponent.SettingsChild.Settings(
            DefaultSettingsComponent(
                initialSettings = ApiSettings(),
                storeFactory = storeFactory,
                storage = storage,
                json = json,
                componentContext = childContext,
            )
        )
    }
}
