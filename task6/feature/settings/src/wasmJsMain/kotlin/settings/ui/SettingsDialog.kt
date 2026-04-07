package settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import settings.SettingsComponent
import settings.SettingsIntent
import ui.components.DialogSurface
import ui.theme.AppColors

@Composable
fun SettingsDialog(component: SettingsComponent, onDismiss: () -> Unit) {
    val state by component.state.subscribeAsState()

    Dialog(onDismissRequest = onDismiss) {
        DialogSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag(SettingsDialogTags.DIALOG_SURFACE),
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag(SettingsDialogTags.ROOT),
            ) {
                SettingsDialogContent(
                    state = state.settings,
                    onIntent = component::accept,
                )

                Spacer(modifier = Modifier.height(24.dp))

                SettingsDialogButtons(
                    onDismiss = onDismiss,
                    onSave = {
                        component.accept(SettingsIntent.SaveSettings)
                    },
                )
            }
        }
    }
}

@Composable
internal fun SettingsDialogContent(state: settings.ApiSettings, onIntent: (SettingsIntent) -> Unit) {
    Text(
        text = "API Settings",
        style = MaterialTheme.typography.h6,
        color = AppColors.TextPrimary,
        modifier = Modifier.testTag(SettingsDialogTags.TITLE),
    )
    Spacer(modifier = Modifier.height(20.dp))
    ProviderInput(state, onIntent)
    ApiKeyInput(state, onIntent)
    ModelSelector(
        selectedModelId = state.model,
        provider = state.provider,
        onModelSelected = { onIntent(SettingsIntent.UpdateModel(it)) },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(12.dp))
    MaxTokensInput(state, onIntent)
    TemperatureInput(state, onIntent)
    StopSequencesInput(state, onIntent)
    ResponseFormatInput(state, onIntent)
}
