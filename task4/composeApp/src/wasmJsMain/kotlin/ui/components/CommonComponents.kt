package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ui.theme.AppColors

@Composable
fun DialogSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        color = AppColors.Surface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
        content = content,
    )
}

@Composable
fun DialogHeader(title: String, onDismiss: () -> Unit, dismissText: String = "Close",) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.h6,
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.Bold,
        )
        TextButton(onClick = onDismiss) {
            Text(dismissText, color = AppColors.TextSecondary)
        }
    }
}

@Composable
fun primaryTextFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    textColor = AppColors.TextPrimary,
    backgroundColor = AppColors.SurfaceLight,
    focusedBorderColor = AppColors.Primary,
    unfocusedBorderColor = AppColors.Border,
    cursorColor = AppColors.Primary,
)

@Composable
fun primaryButtonColors() = ButtonDefaults.buttonColors(
    backgroundColor = AppColors.Primary,
    disabledBackgroundColor = AppColors.SurfaceLight,
)

@Composable
fun LoadingButtonContent(isLoading: Boolean, loadingText: String = "Loading...", buttonText: String = "Submit",) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = Color.White,
            strokeWidth = 2.dp,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(loadingText, color = Color.White)
    } else {
        Text(buttonText, color = Color.White)
    }
}

@Composable
fun EmptyStateBox(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = AppColors.TextMuted,
            style = MaterialTheme.typography.body1,
        )
    }
}

@Composable
fun SectionSpacer(height: Int = 12) {
    Spacer(modifier = Modifier.height(height.dp))
}
