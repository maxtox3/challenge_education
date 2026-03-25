package ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import model.ChatMessage
import ui.animations.slideInAnimation
import ui.theme.AppColors

object MessageBubbleTags {
    const val ROOT = "message_bubble_root"
    const val SURFACE = "message_bubble_surface"
    const val ROLE_TEXT = "message_bubble_role_text"
    const val REASONING_INDICATOR = "message_bubble_reasoning_indicator"
    const val REASONING_TEXT = "message_bubble_reasoning_text"
    const val REASONING_EXPAND_ICON = "message_bubble_reasoning_expand_icon"
    const val MARKDOWN_CONTENT = "message_bubble_markdown_content"
    const val TOKENS_INFO = "message_bubble_tokens_info"
    const val TYPING_INDICATOR = "typing_indicator"
}

@Composable
fun MessageBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val isUser = message.role == "user"

    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .testTag(MessageBubbleTags.ROOT)
            .then(slideInAnimation()),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = when {
                isUser -> AppColors.UserBubble
                message.role == "system" -> AppColors.SystemBubble
                else -> AppColors.AssistantBubble
            },
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            modifier = Modifier
                .fillMaxWidth(0.85f)
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
                Text(
                    text = when {
                        isUser -> "You"
                        message.role == "system" -> "System"
                        else -> "GLM-5"
                    },
                    color = when {
                        isUser -> AppColors.PrimaryContainer
                        else -> AppColors.Secondary
                    },
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.testTag(MessageBubbleTags.ROLE_TEXT),
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (message.isReasoningContent) {
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Markdown(
                        content = message.content,
                        colors = markdownColor(text = AppColors.TextMuted),
                        typography = markdownTypography(),
                        modifier = Modifier
                            .testTag(MessageBubbleTags.MARKDOWN_CONTENT)
                            .alpha(0.8f),
                    )
                } else {
                    Markdown(
                        content = message.content,
                        colors = markdownColor(text = AppColors.TextPrimary),
                        typography = markdownTypography(),
                        modifier = Modifier.testTag(MessageBubbleTags.MARKDOWN_CONTENT),
                    )
                }

                message.tokensUsed?.let { tokens ->
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

                        message.finishReason?.let { reason ->
                            Text(
                                text = " • ${if (reason == "stop") "complete" else "truncated"}",
                                color = if (reason == "stop") AppColors.Success else AppColors.Warning,
                                style = MaterialTheme.typography.caption,
                            )
                        }
                    }
                }
            }
        }
    }
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
            .fillMaxWidth(0.3f)
            .testTag(MessageBubbleTags.TYPING_INDICATOR),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(0, 100, 200).forEach { delay ->
                val scale by transition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = 1200
                            0.5f at delay
                            1.2f at delay + 300
                            0.5f at delay + 600
                        },
                        repeatMode = RepeatMode.Restart,
                    ),
                )

                Box(
                    modifier = Modifier
                        .scale(scale)
                        .size(8.dp)
                        .background(AppColors.TextSecondary, RoundedCornerShape(50)),
                )
            }
        }
    }
}
