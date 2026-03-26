@file:Suppress("MagicNumber")

package ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var localBrain: ImageVector? = null

val Brain: ImageVector
    get() {
        val current = localBrain
        if (current != null) return current

        return ImageVector.Builder(
            name = "Brain",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
            ) {
                moveTo(12f, 2f)
                curveTo(9.5f, 2f, 7.5f, 3.5f, 7f, 5.5f)
                curveTo(5f, 5.5f, 3.5f, 7f, 3.5f, 9f)
                curveTo(3.5f, 10.5f, 4.2f, 11.8f, 5.3f, 12.6f)
                curveTo(4.5f, 13.3f, 4f, 14.4f, 4f, 15.5f)
                curveTo(4f, 17.4f, 5.4f, 19f, 7.2f, 19.2f)
                curveTo(7.5f, 20.8f, 9f, 22f, 10.8f, 22f)
                curveTo(11.5f, 22f, 12.2f, 21.8f, 12.8f, 21.4f)
                curveTo(13.2f, 21.1f, 13.5f, 20.7f, 13.7f, 20.3f)
                curveTo(14f, 20.7f, 14.4f, 21f, 14.8f, 21.2f)
                curveTo(15.4f, 21.6f, 16f, 21.8f, 16.8f, 21.8f)
                curveTo(18.5f, 21.8f, 19.9f, 20.6f, 20.2f, 19f)
                curveTo(22f, 18.8f, 23.4f, 17.2f, 23.4f, 15.3f)
                curveTo(23.4f, 14.2f, 22.9f, 13.1f, 22.1f, 12.4f)
                curveTo(23.2f, 11.6f, 23.9f, 10.3f, 23.9f, 8.8f)
                curveTo(23.9f, 6.8f, 22.4f, 5.3f, 20.4f, 5.3f)
                curveTo(19.9f, 3.3f, 17.9f, 2f, 15.5f, 2f)
                curveTo(14.3f, 2f, 13.2f, 2.4f, 12.4f, 3.1f)
                curveTo(11.8f, 2.4f, 10.9f, 2f, 10f, 2f)
                curveTo(10f, 2f, 12f, 2f, 12f, 2f)
                close()
            }
            path(
                fill = SolidColor(Color.Black),
            ) {
                moveTo(12f, 3f)
                verticalLineTo(21f)
                moveTo(8f, 6f)
                curveTo(8f, 6f, 9f, 8f, 12f, 8f)
                curveTo(15f, 8f, 16f, 6f, 16f, 6f)
                moveTo(6f, 12f)
                curveTo(6f, 12f, 8f, 14f, 12f, 14f)
                curveTo(16f, 14f, 18f, 12f, 18f, 12f)
                moveTo(7f, 18f)
                curveTo(7f, 18f, 9f, 16f, 12f, 16f)
                curveTo(15f, 16f, 17f, 18f, 17f, 18f)
            }
        }.build()
    }
