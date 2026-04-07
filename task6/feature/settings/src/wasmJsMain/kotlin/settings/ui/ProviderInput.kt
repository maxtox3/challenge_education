package settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier.Companion
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import model.ApiProvider
import settings.ApiSettings
import settings.SettingsIntent
import ui.theme.AppColors

@Composable
internal fun ProviderInput(state: ApiSettings, onIntent: (SettingsIntent) -> Unit) {
    Text(
        text = "Provider",
        color = AppColors.TextSecondary,
        style = MaterialTheme.typography.caption,
    )
    Row(
        modifier = Companion.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProviderChip(
            selected = state.provider == ApiProvider.ZAI,
            onClick = {
                onIntent(SettingsIntent.UpdateProvider(ApiProvider.ZAI))
            },
            label = ApiProvider.ZAI.displayName,
            modifier = Companion.testTag(SettingsDialogTags.PROVIDER_ZAI_CHIP),
        )
        ProviderChip(
            selected = state.provider == ApiProvider.OPENROUTER,
            onClick = {
                onIntent(SettingsIntent.UpdateProvider(ApiProvider.OPENROUTER))
            },
            label = ApiProvider.OPENROUTER.displayName,
            modifier = Companion.testTag(SettingsDialogTags.PROVIDER_OPENROUTER_CHIP),
        )
    }
    Spacer(modifier = Companion.height(12.dp))
}
