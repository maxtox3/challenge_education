@file:OptIn(ExperimentalComposeUiApi::class)

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import chat.ChatRepositoryImpl
import chat.DefaultChatComponentFactory
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import kotlinx.serialization.json.Json
import network.ChatClientImpl
import root.DefaultRootComponent
import root.RootContent
import storage.LocalStorageService

fun main() {
    val json = Json { ignoreUnknownKeys = true }
    val chatClient = ChatClientImpl()
    val repository = ChatRepositoryImpl(chatClient)
    val storeFactory = DefaultStoreFactory()
    val storage = LocalStorageService(json)
    val chatComponentFactory = DefaultChatComponentFactory(storeFactory, storage)

    val lifecycle = LifecycleRegistry()
    val componentContext = DefaultComponentContext(lifecycle = lifecycle)

    val rootComponent = DefaultRootComponent(
        repository = repository,
        chatComponentFactory = chatComponentFactory,
        storeFactory = storeFactory,
        storage = storage,
        json = json,
        componentContext = componentContext,
    )

    ComposeViewport("ComposeTarget") {
        RootContent(rootComponent)
    }
}
