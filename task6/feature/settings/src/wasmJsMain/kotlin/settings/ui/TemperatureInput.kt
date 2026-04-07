package settings.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import settings.ApiSettings
import settings.SettingsIntent
import ui.theme.AppColors
import kotlin.math.roundToInt

@Composable
internal fun TemperatureInput(state: ApiSettings, onIntent: (SettingsIntent) -> Unit) {
    Text(
        text = "Temperature: ${(state.temperature * 100).roundToInt() / 100.0}",
        color = AppColors.TextSecondary,
        style = MaterialTheme.typography.caption,
    )
    Slider(
        value = state.temperature.toFloat(),
        onValueChange = { onIntent(SettingsIntent.UpdateTemperature(it.toDouble())) },
        valueRange = 0f..2f,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SettingsDialogTags.TEMPERATURE_SLIDER),
        colors = SliderDefaults.colors(
            thumbColor = AppColors.Primary,
            activeTrackColor = AppColors.Primary,
        ),
    )
    Spacer(modifier = Modifier.height(12.dp))
}
