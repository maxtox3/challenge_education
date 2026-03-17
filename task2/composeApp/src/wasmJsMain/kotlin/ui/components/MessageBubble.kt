package ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
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
        animationSpec = tween(300),
        label = "alpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
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
            modifier = Modifier.fillMaxWidth(0.85f),
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
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = message.content,
                    color = AppColors.TextPrimary,
                    style = MaterialTheme.typography.body2,
                )

                message.tokensUsed?.let { tokens ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
    Surface(
        color = AppColors.AssistantBubble,
        shape = RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp,
        ),
        modifier = Modifier.fillMaxWidth(0.3f),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(AppColors.TextSecondary, shape = RoundedCornerShape(50)),
                )
            }
        }
    }
}
