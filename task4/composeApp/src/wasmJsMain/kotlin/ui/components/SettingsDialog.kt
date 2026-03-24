package ui.components

import ApiSettings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ui.theme.AppColors
import kotlin.math.roundToInt

object SettingsDialogTags {
    const val ROOT = "settings_dialog_root"
    const val DIALOG_SURFACE = "settings_dialog_surface"
    const val TITLE = "settings_dialog_title"
    const val API_KEY_FIELD = "settings_dialog_api_key_field"
    const val MODEL_FIELD = "settings_dialog_model_field"
    const val MAX_TOKENS_FIELD = "settings_dialog_max_tokens_field"
    const val TEMPERATURE_SLIDER = "settings_dialog_temperature_slider"
    const val STOP_SEQUENCES_FIELD = "settings_dialog_stop_sequences_field"
    const val TEXT_FORMAT_CHIP = "settings_dialog_text_format_chip"
    const val JSON_FORMAT_CHIP = "settings_dialog_json_format_chip"
    const val CANCEL_BUTTON = "settings_dialog_cancel_button"
    const val SAVE_BUTTON = "settings_dialog_save_button"
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SettingsDialog(currentSettings: ApiSettings, onDismiss: () -> Unit, onSave: (ApiSettings) -> Unit) {
    var apiKey by remember { mutableStateOf(currentSettings.apiKey) }
    var model by remember { mutableStateOf(currentSettings.model) }
    var maxTokensText by remember { mutableStateOf(currentSettings.maxTokens?.toString() ?: "") }
    var temperature by remember { mutableStateOf(currentSettings.temperature) }
    var stopSequences by remember { mutableStateOf(currentSettings.stopSequences) }
    var responseFormat by remember { mutableStateOf(currentSettings.responseFormat) }

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
                Text(
                    text = "API Settings",
                    style = MaterialTheme.typography.h6,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.testTag(SettingsDialogTags.TITLE),
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key", color = AppColors.TextSecondary) },
                    modifier = Modifier.fillMaxWidth().testTag(SettingsDialogTags.API_KEY_FIELD),
                    colors = primaryTextFieldColors(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model", color = AppColors.TextSecondary) },
                    modifier = Modifier.fillMaxWidth().testTag(SettingsDialogTags.MODEL_FIELD),
                    colors = primaryTextFieldColors(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = maxTokensText,
                    onValueChange = {
                        if (it.isEmpty() || it.all { c -> c.isDigit() }) {
                            maxTokensText = it
                        }
                    },
                    label = { Text("Max Tokens (empty = unlimited)", color = AppColors.TextSecondary) },
                    modifier = Modifier.fillMaxWidth().testTag(SettingsDialogTags.MAX_TOKENS_FIELD),
                    colors = primaryTextFieldColors(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Temperature: ${(temperature * 100).roundToInt() / 100.0}",
                    color = AppColors.TextSecondary,
                    style = MaterialTheme.typography.caption,
                )

                Slider(
                    value = temperature.toFloat(),
                    onValueChange = { temperature = it.toDouble() },
                    valueRange = 0f..2f,
                    modifier = Modifier.fillMaxWidth().testTag(SettingsDialogTags.TEMPERATURE_SLIDER),
                    colors = SliderDefaults.colors(
                        thumbColor = AppColors.Primary,
                        activeTrackColor = AppColors.Primary,
                    ),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = stopSequences,
                    onValueChange = { stopSequences = it },
                    label = { Text("Stop Sequences (comma-separated)", color = AppColors.TextSecondary) },
                    modifier = Modifier.fillMaxWidth().testTag(SettingsDialogTags.STOP_SEQUENCES_FIELD),
                    colors = primaryTextFieldColors(),
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                        selected = responseFormat == "text",
                        onClick = { responseFormat = "text" },
                        label = "Text",
                        modifier = Modifier.testTag(SettingsDialogTags.TEXT_FORMAT_CHIP),
                    )
                    ResponseFormatChip(
                        selected = responseFormat == "json",
                        onClick = { responseFormat = "json" },
                        label = "JSON",
                        modifier = Modifier.testTag(SettingsDialogTags.JSON_FORMAT_CHIP),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                        onClick = {
                            val maxTokens = maxTokensText.toIntOrNull()
                            onSave(
                                ApiSettings(
                                    apiKey = apiKey,
                                    model = model,
                                    maxTokens = maxTokens,
                                    temperature = temperature,
                                    stopSequences = stopSequences,
                                    responseFormat = responseFormat,
                                ),
                            )
                        },
                        colors = primaryButtonColors(),
                        modifier = Modifier.testTag(SettingsDialogTags.SAVE_BUTTON),
                    ) {
                        Text("Save", color = Color.White)
                    }
                }
            }
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
