package settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ui.components.primaryButtonColors
import ui.theme.AppColors

@Composable
internal fun SettingsDialogButtons(onDismiss: () -> Unit, onSave: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag(SettingsDialogTags.CANCEL_BUTTON),
        ) {
            Text("Cancel", color = AppColors.TextSecondary)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onSave,
            colors = primaryButtonColors(),
            modifier = Modifier.testTag(SettingsDialogTags.SAVE_BUTTON),
        ) {
            Text("Save", color = Color.White)
        }
    }
}
