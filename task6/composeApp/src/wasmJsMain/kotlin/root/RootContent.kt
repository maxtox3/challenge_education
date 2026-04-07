package root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import chat.ChatContent
import chat.ChatIntent
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import settings.ui.SettingsDialog

@Composable
fun RootContent(component: RootComponent) {
    ChatContent(
        component = component.chatComponent,
        onOpenSettings = component::showSettings,
    )

    val settingsSlot by component.settingsSlot.subscribeAsState()

    settingsSlot.child?.instance?.let { instance ->
        when (instance) {
            is RootComponent.SettingsChild.Settings -> {
                val settingsState by instance.component.state.subscribeAsState()

                LaunchedEffect(settingsState.settings) {
                    component.chatComponent.accept(ChatIntent.UpdateSettings(settingsState.settings))
                }

                SettingsDialog(
                    component = instance.component,
                    onDismiss = component::dismissSettings,
                )
            }
        }
    }
}
