package settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import model.ModelType
import ui.theme.AppColors

@Composable
fun ModelSelector(selectedModelId: String, onModelSelected: (String) -> Unit, modifier: Modifier = Modifier,) {
    var expanded by remember { mutableStateOf(false) }
    val selectedModel = remember(selectedModelId) { ModelType.fromId(selectedModelId) }

    Box(modifier = modifier) {
        TextField(
            value = "${selectedModel.displayName} (${selectedModel.level})",
            onValueChange = {},
            readOnly = true,
            label = { Text("Model", color = AppColors.TextSecondary) },
            trailingIcon = { Text("▼", color = AppColors.TextSecondary) },
            colors = TextFieldDefaults.textFieldColors(
                textColor = AppColors.TextPrimary,
                backgroundColor = AppColors.SurfaceLight,
                focusedIndicatorColor = AppColors.Primary,
                unfocusedIndicatorColor = AppColors.Border,
                cursorColor = AppColors.Primary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ModelSelectorTags.TEXT_FIELD)
                .clickable { expanded = true },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(AppColors.Surface)
                .testTag(ModelSelectorTags.DROPDOWN),
        ) {
            ModelType.entries.forEach { model ->
                DropdownMenuItem(
                    onClick = {
                        onModelSelected(model.id)
                        expanded = false
                    },
                    modifier = Modifier.testTag("${ModelSelectorTags.ITEM}_${model.id}"),
                ) {
                    Text(
                        text = "${model.displayName} (${model.level})",
                        style = MaterialTheme.typography.body2,
                        color = if (model.id == selectedModelId) AppColors.Primary else AppColors.TextPrimary,
                    )
                }
            }
        }
    }
}
