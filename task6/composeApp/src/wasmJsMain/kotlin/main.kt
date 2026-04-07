@file:OptIn(ExperimentalComposeUiApi::class)

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import chat.ChatContent
import chat.ChatRepositoryImpl
import chat.DefaultChatComponentFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import kotlinx.serialization.json.Json
import network.ChatClientImpl
import root.DefaultRootComponent
import storage.LocalStorageService

fun main() {
    val json = Json { ignoreUnknownKeys = true }
    val chatClient = ChatClientImpl(apiKeyProvider = { "9cccc72cda3c456c9263fe143dbae7b1.9ir3VQrPquSuSyvZ" })
    val repository = ChatRepositoryImpl(chatClient)
    val storeFactory = DefaultStoreFactory()
    val storage = LocalStorageService(json)
    val chatComponentFactory = DefaultChatComponentFactory(storeFactory, storage)

    val rootComponent = DefaultRootComponent(
        repository = repository,
        chatComponentFactory = chatComponentFactory,
        storeFactory = storeFactory,
        storage = storage,
        json = json
    )

    ComposeViewport("ComposeTarget") {
        ChatContent(
            component = rootComponent.chatComponent,
            settingsComponent = rootComponent.settingsComponent
        )
    }
}
