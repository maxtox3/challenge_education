@file:OptIn(ExperimentalComposeUiApi::class)

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow

fun main() {
    CanvasBasedWindow("Z.ai Chat") {
        App()
    }
}
