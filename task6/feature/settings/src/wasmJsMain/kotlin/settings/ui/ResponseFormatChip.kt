package settings.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ui.theme.AppColors

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ResponseFormatChip(selected: Boolean, onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
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
