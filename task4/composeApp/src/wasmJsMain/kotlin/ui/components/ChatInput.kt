package ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ui.theme.AppColors

@Composable
fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ChatInputTags.ROOT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .testTag(ChatInputTags.TEXT_FIELD)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown &&
                        keyEvent.key == Key.Enter &&
                        !keyEvent.isShiftPressed
                    ) {
                        if (value.isNotBlank() && !isLoading) {
                            onSend()
                        }
                        true
                    } else {
                        false
                    }
                },
            placeholder = {
                Text(
                    "Type a message... (Enter to send)",
                    color = AppColors.TextMuted,
                )
            },
            colors = primaryTextFieldColors(),
            shape = RoundedCornerShape(24.dp),
            maxLines = 4,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Button(
            onClick = onSend,
            enabled = !isLoading && value.isNotBlank(),
            colors = primaryButtonColors(),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .height(52.dp)
                .testTag(ChatInputTags.SEND_BUTTON),
        ) {
            LoadingButtonContent(
                isLoading = isLoading,
                loadingText = "Sending...",
                buttonText = "Send",
            )
        }
    }
}
