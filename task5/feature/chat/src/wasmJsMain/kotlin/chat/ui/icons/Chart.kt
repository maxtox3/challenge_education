@file:Suppress("MagicNumber")

package chat.ui.icons

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
                horizontalLineTo(2f)
                verticalLineTo(8f)
                horizontalLineTo(2f)
                close()
                moveTo(9f, 9f)
                horizontalLineTo(2f)
                verticalLineTo(12f)
                horizontalLineTo(2f)
                close()
                moveTo(15f, 5f)
                horizontalLineTo(2f)
                verticalLineTo(16f)
                horizontalLineTo(2f)
                close()
                moveTo(21f, 3f)
                verticalLineTo(18f)
                horizontalLineTo(2f)
                verticalLineTo(3f)
                close()
            }
        }.build()
    }
