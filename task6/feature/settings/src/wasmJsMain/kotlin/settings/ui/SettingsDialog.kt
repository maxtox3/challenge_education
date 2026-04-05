package settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import settings.SettingsComponent
import settings.SettingsIntent
import ui.components.DialogSurface
import ui.components.primaryButtonColors
import ui.components.primaryTextFieldColors
import ui.theme.AppColors
import kotlin.math.roundToInt

@Composable
fun SettingsDialog(component: SettingsComponent, onDismiss: () -> Unit,) {
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
private fun SettingsDialogContent(state: settings.ApiSettings, onIntent: (SettingsIntent) -> Unit,) {
    Text(
        text = "API Settings",
        style = MaterialTheme.typography.h6,
        color = AppColors.TextPrimary,
        modifier = Modifier.testTag(SettingsDialogTags.TITLE),
    )
    Spacer(modifier = Modifier.height(20.dp))
    ApiKeyInput(state, onIntent)
    ModelSelector(
        selectedModelId = state.model,
        onModelSelected = { onIntent(SettingsIntent.UpdateModel(it)) },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(12.dp))
    MaxTokensInput(state, onIntent)
    TemperatureInput(state, onIntent)
    StopSequencesInput(state, onIntent)
    ResponseFormatInput(state, onIntent)
}

@Composable
private fun ApiKeyInput(state: settings.ApiSettings, onIntent: (SettingsIntent) -> Unit,) {
    OutlinedTextField(
        value = state.apiKey,
        onValueChange = { onIntent(SettingsIntent.UpdateApiKey(it)) },
        label = { Text("API Key", color = AppColors.TextSecondary) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SettingsDialogTags.API_KEY_FIELD),
        colors = primaryTextFieldColors(),
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun MaxTokensInput(state: settings.ApiSettings, onIntent: (SettingsIntent) -> Unit,) {
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

@Composable
private fun TemperatureInput(state: settings.ApiSettings, onIntent: (SettingsIntent) -> Unit,) {
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

@Composable
private fun StopSequencesInput(state: settings.ApiSettings, onIntent: (SettingsIntent) -> Unit,) {
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

@Composable
private fun ResponseFormatInput(state: settings.ApiSettings, onIntent: (SettingsIntent) -> Unit,) {
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

@Composable
private fun SettingsDialogButtons(onDismiss: () -> Unit, onSave: () -> Unit,) {
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ResponseFormatChip(selected: Boolean, onClick: () -> Unit, label: String, modifier: Modifier = Modifier,) {
    Surface(
        color = if (selected) AppColors.Primary else AppColors.SurfaceLight,
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else AppColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
