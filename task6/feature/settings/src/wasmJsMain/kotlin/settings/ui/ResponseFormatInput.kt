package settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import settings.ApiSettings
import settings.SettingsIntent
import ui.theme.AppColors

@Composable
internal fun ResponseFormatInput(state: ApiSettings, onIntent: (SettingsIntent) -> Unit) {
    Text(
        text = "Response Format",
        color = AppColors.TextSecondary,
        style = MaterialTheme.typography.caption,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ResponseFormatChip(
            selected = state.responseFormat == "text",
            onClick = { onIntent(SettingsIntent.UpdateResponseFormat("text")) },
            label = "Text",
            modifier = Modifier.testTag(SettingsDialogTags.TEXT_FORMAT_CHIP),
        )
        ResponseFormatChip(
            selected = state.responseFormat == "json",
            onClick = { onIntent(SettingsIntent.UpdateResponseFormat("json")) },
            label = "JSON",
            modifier = Modifier.testTag(SettingsDialogTags.JSON_FORMAT_CHIP),
        )
    }
}
