import ChatIntent
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ui.Brain
import ui.Chart
import ui.Close
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

object AppTags {
    const val ROOT = "app_root"
    const val HEADER = "app_header"
    const val TITLE = "app_title"
    const val MODEL_TEXT = "app_model_text"
    const val CLEAR_BUTTON = "app_clear_button"
    const val METRICS_BUTTON = "app_metrics_button"
    const val REASONING_BUTTON = "app_reasoning_button"
    const val SETTINGS_BUTTON = "app_settings_button"
    const val MESSAGE_LIST = "app_message_list"
    const val EMPTY_STATE = "app_empty_state"
    const val ERROR_SURFACE = "app_error_surface"
    const val ERROR_TEXT = "app_error_text"
    const val ERROR_DISMISS_BUTTON = "app_error_dismiss_button"
    const val LOADING_INDICATOR = "app_loading_indicator"
}

@Composable
fun App() {
    val state = rememberChatViewModel()

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
                .padding(16.dp)
                .testTag(AppTags.ROOT),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag(AppTags.HEADER),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Z.ai Chat",
                        style = MaterialTheme.typography.h5,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.testTag(AppTags.TITLE),
                    )
                    Text(
                        text = "Model: ${state.settings.model}",
                        style = MaterialTheme.typography.caption,
                        color = AppColors.TextMuted,
                        modifier = Modifier.testTag(AppTags.MODEL_TEXT),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { state.clearChat() },
                        modifier = Modifier
                            .background(AppColors.SurfaceLight, CircleShape)
                            .size(40.dp)
                            .testTag(AppTags.CLEAR_BUTTON),
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
                            .size(40.dp)
                            .testTag(AppTags.METRICS_BUTTON),
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
                            .size(40.dp)
                            .testTag(AppTags.REASONING_BUTTON),
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
                            .size(40.dp)
                            .testTag(AppTags.SETTINGS_BUTTON),
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
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag(AppTags.EMPTY_STATE),
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
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(AppTags.MESSAGE_LIST),
                        state = state.listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(state.messages) { message ->
                            MessageBubble(message = message)
                        }

                        if (state.isLoading) {
                            item {
                                Box(modifier = Modifier.testTag(AppTags.LOADING_INDICATOR)) {
                                    TypingIndicator()
                                }
                            }
                        }
                    }
                }
            }

            state.errorMessage?.let { error ->
                Surface(
                    color = AppColors.Error.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .testTag(AppTags.ERROR_SURFACE),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = error,
                            color = AppColors.Error,
                            modifier = Modifier
                                .weight(1f)
                                .testTag(AppTags.ERROR_TEXT),
                            style = MaterialTheme.typography.body2,
                        )
                        IconButton(
                            onClick = { state.processIntent(ChatIntent.ClearError) },
                            modifier = Modifier.testTag(AppTags.ERROR_DISMISS_BUTTON),
                        ) {
                            Icon(
                                imageVector = Close,
                                contentDescription = "Dismiss error",
                                tint = AppColors.Error,
                            )
                        }
                    }
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

@Composable
fun AppWithState(state: ChatViewModel) {
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
                .padding(16.dp)
                .testTag(AppTags.ROOT),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag(AppTags.HEADER),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Z.ai Chat",
                        style = MaterialTheme.typography.h5,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.testTag(AppTags.TITLE),
                    )
                    Text(
                        text = "Model: ${state.settings.model}",
                        style = MaterialTheme.typography.caption,
                        color = AppColors.TextMuted,
                        modifier = Modifier.testTag(AppTags.MODEL_TEXT),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { state.clearChat() },
                        modifier = Modifier
                            .background(AppColors.SurfaceLight, CircleShape)
                            .size(40.dp)
                            .testTag(AppTags.CLEAR_BUTTON),
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
                            .size(40.dp)
                            .testTag(AppTags.METRICS_BUTTON),
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
                            .size(40.dp)
                            .testTag(AppTags.REASONING_BUTTON),
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
                            .size(40.dp)
                            .testTag(AppTags.SETTINGS_BUTTON),
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
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag(AppTags.EMPTY_STATE),
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
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(AppTags.MESSAGE_LIST),
                        state = state.listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(state.messages) { message ->
                            MessageBubble(message = message)
                        }

                        if (state.isLoading) {
                            item {
                                Box(modifier = Modifier.testTag(AppTags.LOADING_INDICATOR)) {
                                    TypingIndicator()
                                }
                            }
                        }
                    }
                }
            }

            state.errorMessage?.let { error ->
                Surface(
                    color = AppColors.Error.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .testTag(AppTags.ERROR_SURFACE),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = error,
                            color = AppColors.Error,
                            modifier = Modifier
                                .weight(1f)
                                .testTag(AppTags.ERROR_TEXT),
                            style = MaterialTheme.typography.body2,
                        )
                        IconButton(
                            onClick = { state.processIntent(ChatIntent.ClearError) },
                            modifier = Modifier.testTag(AppTags.ERROR_DISMISS_BUTTON),
                        ) {
                            Icon(
                                imageVector = Close,
                                contentDescription = "Dismiss error",
                                tint = AppColors.Error,
                            )
                        }
                    }
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
