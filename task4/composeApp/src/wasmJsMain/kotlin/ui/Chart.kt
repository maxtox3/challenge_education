@file:Suppress("MagicNumber")

package ui

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var localChart: ImageVector? = null

val Chart: ImageVector
    get() {
        val current = localChart
        if (current != null) return current

        return ImageVector.Builder(
            name = "Chart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(androidx.compose.ui.graphics.Color.Black),
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(3f, 13f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(8f)
                horizontalLineToRelative(-2f)
                close()
                moveTo(9f, 9f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(12f)
                horizontalLineToRelative(-2f)
                close()
                moveTo(15f, 5f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(16f)
                horizontalLineToRelative(-2f)
                close()
                moveTo(21f, 3f)
                verticalLineToRelative(18f)
                horizontalLineToRelative(-2f)
                verticalLineTo(3f)
                close()
            }
        }.build()
    }
