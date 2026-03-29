package chat.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import model.ChatMessage
import ui.animations.slideInAnimation
import ui.theme.AppColors

@Composable
fun MessageBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val isUser = message.role == "user"
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(MessageBubbleConstants.ANIMATION_DURATION_MS),
        label = "alpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .testTag(MessageBubbleTags.ROOT)
            .then(if (!message.isStreaming) slideInAnimation() else Modifier),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        MessageSurface(message, isUser)
    }
}

@Composable
private fun MessageSurface(message: ChatMessage, isUser: Boolean) {
    Surface(
        color = getMessageColor(message.role, isUser),
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isUser) 16.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 16.dp,
        ),
        modifier = Modifier
            .fillMaxWidth(MessageBubbleConstants.MESSAGE_WIDTH_FRACTION)
            .testTag(MessageBubbleTags.SURFACE)
            .then(
                if (message.isReasoningContent) {
                    Modifier.border(2.dp, AppColors.Warning, RoundedCornerShape(16.dp))
                } else {
                    Modifier
                },
            ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            RoleLabel(message.role, isUser, message.model)
            Spacer(modifier = Modifier.height(6.dp))
            MessageContent(message)
            TokensInfo(message.tokensUsed, message.finishReason)
        }
    }
}

@Composable
private fun RoleLabel(role: String, isUser: Boolean, model: String?) {
    Text(
        text = getRoleText(role, isUser, model),
        color = if (isUser) AppColors.PrimaryContainer else AppColors.Secondary,
        style = MaterialTheme.typography.caption,
        modifier = Modifier.testTag(MessageBubbleTags.ROLE_TEXT),
    )
}

@Composable
private fun ReasoningContent(message: ChatMessage) {
    val scrollState = rememberScrollState()
    var userScrolledUp by remember { mutableStateOf(false) }

    LaunchedEffect(message.content) {
        userScrolledUp = false
    }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }
            .onEach { position ->
                if (position < scrollState.maxValue - MessageBubbleConstants.REASONING_SCROLL_THRESHOLD) {
                    userScrolledUp = true
                }
            }
            .collect()
    }

    LaunchedEffect(Unit) {
        snapshotFlow { scrollState.maxValue }
            .onEach { maxValue ->
                if (!userScrolledUp && maxValue > 0) {
                    scrollState.animateScrollTo(maxValue)
                }
            }
            .collect()
    }

    Column {
        ReasoningIndicator()
        Spacer(modifier = Modifier.height(8.dp))
        Markdown(
            content = message.content,
            colors = markdownColor(text = AppColors.TextMuted),
            typography = markdownTypography(),
            modifier = Modifier
                .heightIn(max = MessageBubbleConstants.REASONING_MAX_HEIGHT.dp)
                .verticalScroll(scrollState)
                .testTag(MessageBubbleTags.MARKDOWN_CONTENT)
                .alpha(MessageBubbleConstants.REASONING_ALPHA),
        )
    }
}

@Composable
private fun MessageContent(message: ChatMessage) {
    if (message.isReasoningContent) {
        ReasoningContent(message)
    } else {
        Markdown(
            content = message.content,
            colors = markdownColor(text = AppColors.TextPrimary),
            typography = markdownTypography(),
            modifier = Modifier.testTag(MessageBubbleTags.MARKDOWN_CONTENT),
        )
    }
}

@Composable
private fun ReasoningIndicator() {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        modifier = Modifier.testTag(MessageBubbleTags.REASONING_INDICATOR),
    ) {
        Text(
            text = "[!]",
            fontSize = 12.sp,
            color = AppColors.Warning,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Reasoning content",
            color = AppColors.Warning,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag(MessageBubbleTags.REASONING_TEXT),
        )
    }
}

@Composable
private fun TokensInfo(tokensUsed: Int?, finishReason: String?) {
    tokensUsed?.let { tokens ->
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.testTag(MessageBubbleTags.TOKENS_INFO),
        ) {
            Text(
                text = "$tokens tokens",
                color = AppColors.TextMuted,
                style = MaterialTheme.typography.caption,
            )
            finishReason?.let { reason ->
                Text(
                    text = " • ${if (reason == "stop") "complete" else "truncated"}",
                    color = if (reason == "stop") AppColors.Success else AppColors.Warning,
                    style = MaterialTheme.typography.caption,
                )
            }
        }
    }
}

private fun getMessageColor(role: String, isUser: Boolean) = when {
    isUser -> AppColors.UserBubble
    role == "system" -> AppColors.SystemBubble
    else -> AppColors.AssistantBubble
}

private fun getRoleText(role: String, isUser: Boolean, model: String?) = when {
    isUser -> "You"
    role == "system" -> "System"
    else -> model?.replaceFirstChar { it.uppercase() } ?: "AI"
}

@Composable
fun TypingIndicator() {
    val transition = rememberInfiniteTransition()

    Surface(
        color = AppColors.AssistantBubble,
        shape = RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp,
        ),
        modifier = Modifier
            .fillMaxWidth(MessageBubbleConstants.TYPING_INDICATOR_WIDTH_FRACTION)
            .testTag(MessageBubbleTags.TYPING_INDICATOR),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(
                0,
                MessageBubbleConstants.TYPING_DOT_DELAY_1_MS,
                MessageBubbleConstants.TYPING_DOT_DELAY_2_MS,
            ).forEach { delay ->
                val scale by transition.animateFloat(
                    initialValue = MessageBubbleConstants.TYPING_DOT_SCALE_MIN,
                    targetValue = MessageBubbleConstants.TYPING_DOT_SCALE_MAX,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = MessageBubbleConstants.TYPING_ANIMATION_DURATION_MS
                            MessageBubbleConstants.TYPING_DOT_SCALE_MIN at delay
                            MessageBubbleConstants.TYPING_DOT_SCALE_MAX at
                                delay + MessageBubbleConstants.TYPING_DOT_ANIMATION_OFFSET_MS
                            MessageBubbleConstants.TYPING_DOT_SCALE_MIN at
                                delay + MessageBubbleConstants.TYPING_DOT_ANIMATION_CYCLE_MS
                        },
                        repeatMode = RepeatMode.Restart,
                    ),
                )

                Box(
                    modifier = Modifier
                        .scale(scale)
                        .size(8.dp)
                        .background(
                            AppColors.TextSecondary,
                            RoundedCornerShape(MessageBubbleConstants.TYPING_DOT_CORNER_RADIUS_PERCENT),
                        ),
                )
            }
        }
    }
}

private object MessageBubbleConstants {
    const val ANIMATION_DURATION_MS = 300
    const val MESSAGE_WIDTH_FRACTION = 0.85f
    const val REASONING_MAX_HEIGHT = 120
    const val REASONING_ALPHA = 0.8f
    const val REASONING_SCROLL_THRESHOLD = 10
    const val TYPING_INDICATOR_WIDTH_FRACTION = 0.3f
    const val TYPING_ANIMATION_DURATION_MS = 1200
    const val TYPING_DOT_SCALE_MIN = 0.5f
    const val TYPING_DOT_SCALE_MAX = 1.2f
    const val TYPING_DOT_DELAY_1_MS = 100
    const val TYPING_DOT_DELAY_2_MS = 200
    const val TYPING_DOT_ANIMATION_OFFSET_MS = 300
    const val TYPING_DOT_ANIMATION_CYCLE_MS = 600
    const val TYPING_DOT_CORNER_RADIUS_PERCENT = 50
}
