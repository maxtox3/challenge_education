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
internal fun MaxTokensInput(state: ApiSettings, onIntent: (SettingsIntent) -> Unit) {
    OutlinedTextField(
        value = state.maxTokens?.toString() ?: "",
        onValueChange = {
            if (it.isEmpty() || it.all { c -> c.isDigit() }) {
                onIntent(SettingsIntent.UpdateMaxTokens(it.toIntOrNull()))
            }
        },
        label = {
            Text(
                "Max Tokens (empty = unlimited)",
                color = AppColors.TextSecondary,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SettingsDialogTags.MAX_TOKENS_FIELD),
        colors = primaryTextFieldColors(),
    )
    Spacer(modifier = Modifier.height(12.dp))
}
