package ui.animations

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

private const val SLIDE_IN_INITIAL_OFFSET = 20f
private const val SLIDE_IN_ANIMATION_DURATION_MS = 300

@Composable
fun slideInAnimation(): Modifier {
    var animated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animated = true }

    val offsetY by animateFloatAsState(
        targetValue = if (animated) 0f else SLIDE_IN_INITIAL_OFFSET,
        animationSpec = tween(SLIDE_IN_ANIMATION_DURATION_MS, easing = FastOutSlowInEasing),
        label = "offsetY",
    )
    return Modifier.offset { IntOffset(0, offsetY.roundToInt()) }
}
