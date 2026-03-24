@file:OptIn(ExperimentalComposeUiApi::class)

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

fun main() {
    ComposeViewport("ComposeTarget") {
        App()
    }
}
