@file:OptIn(ExperimentalComposeUiApi::class)

import agent.AgentStore
import agent.KtorLlmClient
import agent.LocalStorageContextStorage
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import chat.ui.App
import network.ChatClientImpl
import root.DefaultRootComponent

fun main() {
    val chatClient = ChatClientImpl(apiKeyProvider = { "9cccc72cda3c456c9263fe143dbae7b1.9ir3VQrPquSuSyvZ" })
    val llmClient = KtorLlmClient(chatClient)
    val storage = LocalStorageContextStorage()
    val agentStore = AgentStore(llmClient, storage)

    DefaultRootComponent(agentStore = agentStore)

    ComposeViewport("ComposeTarget") {
        App()
    }
}
