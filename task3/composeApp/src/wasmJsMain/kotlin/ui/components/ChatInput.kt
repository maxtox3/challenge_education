package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
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
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
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
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = AppColors.TextPrimary,
                backgroundColor = AppColors.SurfaceLight,
                focusedBorderColor = AppColors.Primary,
                unfocusedBorderColor = AppColors.Border,
                cursorColor = AppColors.Primary,
            ),
            shape = RoundedCornerShape(24.dp),
            maxLines = 4,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Button(
            onClick = onSend,
            enabled = !isLoading && value.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = AppColors.Primary,
                disabledBackgroundColor = AppColors.SurfaceLight,
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.height(52.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Send", color = Color.White)
            }
        }
    }
}
