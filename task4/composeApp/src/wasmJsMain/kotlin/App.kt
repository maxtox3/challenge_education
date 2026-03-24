import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ui.Brain
import ui.Chart
import ui.Delete
import ui.Settings
import ui.components.ChatInput
import ui.components.MessageBubble
import ui.components.MetricsDialog
import ui.components.ReasoningDialog
import ui.components.SettingsDialog
import ui.components.TypingIndicator
import ui.theme.AppColors
import ui.theme.AppTheme

@Composable
fun App() {
    val state = rememberChatStateHolder()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            state.listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    AppTheme {
        if (state.showSettings) {
            SettingsDialog(
                currentSettings = state.settings,
                onDismiss = { state.showSettings = false },
                onSave = { newSettings -> state.updateSettings(newSettings) },
            )
        }

        if (state.showMetrics) {
            MetricsDialog(
                metrics = state.metrics,
                onDismiss = { state.showMetrics = false },
            )
        }

        if (state.showReasoning) {
            ReasoningDialog(
                comparison = state.reasoningComparison,
                isLoading = state.isReasoningLoading,
                onDismiss = { state.showReasoning = false },
                onRunComparison = { task -> state.runReasoningComparison(task) },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Z.ai Chat",
                        style = MaterialTheme.typography.h5,
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        text = "Model: ${state.settings.model}",
                        style = MaterialTheme.typography.caption,
                        color = AppColors.TextMuted,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { state.clearChat() },
                        modifier = Modifier
                            .background(AppColors.SurfaceLight, CircleShape)
                            .size(40.dp),
                    ) {
                        Icon(
                            imageVector = Delete,
                            contentDescription = "Clear chat",
                            tint = AppColors.TextSecondary,
                        )
                    }

                    IconButton(
                        onClick = { state.showMetrics = true },
                        modifier = Modifier
                            .background(
                                if (state.metrics.isNotEmpty()) AppColors.Primary else AppColors.SurfaceLight,
                                CircleShape,
                            )
                            .size(40.dp),
                    ) {
                        Icon(
                            imageVector = Chart,
                            contentDescription = "Metrics",
                            tint = if (state.metrics.isNotEmpty()) Color.White else AppColors.TextSecondary,
                        )
                    }

                    IconButton(
                        onClick = { state.showReasoning = true },
                        modifier = Modifier
                            .background(AppColors.SurfaceLight, CircleShape)
                            .size(40.dp),
                    ) {
                        Icon(
                            imageVector = Brain,
                            contentDescription = "Reasoning comparison",
                            tint = AppColors.TextSecondary,
                        )
                    }

                    IconButton(
                        onClick = { state.showSettings = true },
                        modifier = Modifier
                            .background(AppColors.Primary, CircleShape)
                            .size(40.dp),
                    ) {
                        Icon(
                            imageVector = Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(AppColors.Surface, RoundedCornerShape(16.dp))
                    .padding(12.dp),
            ) {
                if (state.messages.isEmpty() && !state.isLoading) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Start a conversation",
                            color = AppColors.TextSecondary,
                            style = MaterialTheme.typography.h6,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Press Enter to send",
                            color = AppColors.TextMuted,
                            style = MaterialTheme.typography.caption,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = state.listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(state.messages) { message ->
                            MessageBubble(message = message)
                        }

                        if (state.isLoading) {
                            item {
                                TypingIndicator()
                            }
                        }
                    }
                }
            }

            state.errorMessage?.let { error ->
                Surface(
                    color = AppColors.Error.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    Text(
                        text = error,
                        color = AppColors.Error,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.body2,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ChatInput(
                value = state.inputText,
                onValueChange = { state.inputText = it },
                onSend = { state.sendMessage() },
                isLoading = state.isLoading,
            )
        }
    }
}
