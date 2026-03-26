@file:Suppress("MagicNumber")

package ui

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var localSettings: ImageVector? = null

val Settings: ImageVector
    get() {
        val current = localSettings
        if (current != null) return current

        return ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(androidx.compose.ui.graphics.Color.Black),
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(12f, 15.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 8.5f, 12f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f, 8.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 15.5f, 12f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f, 15.5f)
                moveToRelative(7.43f, -2.53f)
                curveToRelative(0.04f, -0.32f, 0.07f, -0.64f, 0.07f, -0.97f)
                reflectiveCurveToRelative(-0.03f, -0.66f, -0.07f, -1f)
                lineToRelative(2.11f, -1.63f)
                curveToRelative(0.19f, -0.15f, 0.24f, -0.42f, 0.12f, -0.64f)
                lineToRelative(-2f, -3.46f)
                curveToRelative(-0.12f, -0.22f, -0.39f, -0.31f, -0.61f, -0.22f)
                lineToRelative(-2.49f, 1f)
                curveToRelative(-0.52f, -0.39f, -1.06f, -0.73f, -1.69f, -0.98f)
                lineToRelative(-0.37f, -2.65f)
                arcTo(0.506f, 0.506f, 0f, isMoreThanHalf = false, isPositiveArc = false, 14f, 2f)
                horizontalLineToRelative(-4f)
                curveToRelative(-0.25f, 0f, -0.46f, 0.18f, -0.5f, 0.42f)
                lineToRelative(-0.37f, 2.65f)
                curveToRelative(-0.63f, 0.25f, -1.17f, 0.59f, -1.69f, 0.98f)
                lineToRelative(-2.49f, -1f)
                curveToRelative(-0.22f, -0.09f, -0.49f, 0f, -0.61f, 0.22f)
                lineToRelative(-2f, 3.46f)
                curveToRelative(-0.13f, 0.22f, -0.07f, 0.49f, 0.12f, 0.64f)
                lineTo(4.57f, 11f)
                curveToRelative(-0.04f, 0.34f, -0.07f, 0.67f, -0.07f, 1f)
                reflectiveCurveToRelative(0.03f, 0.65f, 0.07f, 0.97f)
                lineToRelative(-2.11f, 1.66f)
                curveToRelative(-0.19f, 0.15f, -0.25f, 0.42f, -0.12f, 0.64f)
                lineToRelative(2f, 3.46f)
                curveToRelative(0.12f, 0.22f, 0.39f, 0.3f, 0.61f, 0.22f)
                lineToRelative(2.49f, -1.01f)
                curveToRelative(0.52f, 0.4f, 1.06f, 0.74f, 1.69f, 0.99f)
                lineToRelative(0.37f, 2.65f)
                curveToRelative(0.04f, 0.24f, 0.25f, 0.42f, 0.5f, 0.42f)
                horizontalLineToRelative(4f)
                curveToRelative(0.25f, 0f, 0.46f, -0.18f, 0.5f, -0.42f)
                lineToRelative(0.37f, -2.65f)
                curveToRelative(0.63f, -0.26f, 1.17f, -0.59f, 1.69f, -0.99f)
                lineToRelative(2.49f, 1.01f)
                curveToRelative(0.22f, 0.08f, 0.49f, 0f, 0.61f, -0.22f)
                lineToRelative(2f, -3.46f)
                curveToRelative(0.12f, -0.22f, 0.07f, -0.49f, -0.12f, -0.64f)
                close()
            }
        }.build()
    }
