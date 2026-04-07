package settings.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import settings.ApiSettings
import settings.SettingsIntent
import ui.components.primaryTextFieldColors
import ui.theme.AppColors

@Composable
internal fun StopSequencesInput(state: ApiSettings, onIntent: (SettingsIntent) -> Unit) {
    OutlinedTextField(
        value = state.stopSequences,
        onValueChange = { onIntent(SettingsIntent.UpdateStopSequences(it)) },
        label = {
            Text(
                "Stop Sequences (comma-separated)",
                color = AppColors.TextSecondary,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SettingsDialogTags.STOP_SEQUENCES_FIELD),
        colors = primaryTextFieldColors(),
    )
    Spacer(modifier = Modifier.height(12.dp))
}
