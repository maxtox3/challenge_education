package ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ui.theme.AppColors

@Composable
fun DialogSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        color = AppColors.Surface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.testTag(DialogSurfaceTags.ROOT),
        content = content,
    )
}

@Composable
fun DialogHeader(title: String, onDismiss: () -> Unit, dismissText: String = "Close") {
    Row(
        modifier = Modifier.fillMaxWidth().testTag(DialogHeaderTags.ROOT),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.h6,
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag(DialogHeaderTags.TITLE),
        )
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag(DialogHeaderTags.CLOSE_BUTTON),
        ) {
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
fun LoadingButtonContent(isLoading: Boolean, loadingText: String = "Loading...", buttonText: String = "Submit") {
    Row(
        modifier = Modifier.testTag(LoadingButtonContentTags.ROOT),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp).testTag(LoadingButtonContentTags.LOADING_INDICATOR),
                color = Color.White,
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(loadingText, color = Color.White, modifier = Modifier.testTag(LoadingButtonContentTags.BUTTON_TEXT))
        } else {
            Text(buttonText, color = Color.White, modifier = Modifier.testTag(LoadingButtonContentTags.BUTTON_TEXT))
        }
    }
}

@Composable
fun EmptyStateBox(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().testTag(EmptyStateBoxTags.ROOT),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = AppColors.TextMuted,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.testTag(EmptyStateBoxTags.MESSAGE),
        )
    }
}

@Composable
fun SectionSpacer(height: Int = 12) {
    Spacer(modifier = Modifier.height(height.dp))
}

object DialogSurfaceTags {
    const val ROOT = "dialog_surface_root"
}

object DialogHeaderTags {
    const val ROOT = "dialog_header_root"
    const val TITLE = "dialog_header_title"
    const val CLOSE_BUTTON = "dialog_header_close_button"
}

object LoadingButtonContentTags {
    const val ROOT = "loading_button_content_root"
    const val LOADING_INDICATOR = "loading_button_loading_indicator"
    const val BUTTON_TEXT = "loading_button_text"
}

object EmptyStateBoxTags {
    const val ROOT = "empty_state_box_root"
    const val MESSAGE = "empty_state_box_message"
}
