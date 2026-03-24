package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ui.theme.AppColors

object ChatInputTags {
    const val ROOT = "chat_input_root"
    const val TEXT_FIELD = "chat_input_text_field"
    const val SEND_BUTTON = "chat_input_send_button"
}

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
