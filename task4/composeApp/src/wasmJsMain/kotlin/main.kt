@file:OptIn(ExperimentalComposeUiApi::class)

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import chat.ui.App

fun main() {
    ComposeViewport("ComposeTarget") {
        App()
    }
}
