@file:OptIn(ExperimentalComposeUiApi::class)

import agent.AgentStore
import agent.KtorLlmClient
import agent.LocalStorageContextStorage
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import chat.ChatContent
import network.ChatClientImpl
import root.DefaultRootComponent

fun main() {
    val chatClient = ChatClientImpl(apiKeyProvider = { "9cccc72cda3c456c9263fe143dbae7b1.9ir3VQrPquSuSyvZ" })
    val llmClient = KtorLlmClient(chatClient)
    val storage = LocalStorageContextStorage()
    val agentStore = AgentStore(llmClient, storage)

    val rootComponent = DefaultRootComponent(agentStore = agentStore)

    ComposeViewport("ComposeTarget") {
        ChatContent(
            component = rootComponent.chatComponent,
            settingsComponent = rootComponent.settingsComponent
        )
    }
}
