package chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import chat.ui.components.ChatInput
import chat.ui.components.MessageBubble
import chat.ui.components.TypingIndicator
import chat.ui.icons.Close
import chat.ui.icons.Delete
import chat.ui.icons.Settings
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import settings.ApiSettings
import settings.SettingsComponent
import settings.ui.SettingsDialog
import ui.theme.AppColors
import ui.theme.AppTheme

@Composable
fun ChatContent(component: ChatComponent, settingsComponent: SettingsComponent) {
    val state by component.state.subscribeAsState()

    val settings = remember(state.settings) {
        ApiSettings(
            model = state.settings.model,
            maxTokens = state.settings.maxTokens,
            temperature = state.settings.temperature
        )
    }

    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    AppTheme {
        DialogsOverlay(state, component, settingsComponent)
        ChatScreen(state, settings, component, listState)
    }
}

@Composable
private fun DialogsOverlay(state: ChatState, component: ChatComponent, settingsComponent: SettingsComponent) {
    if (state.showSettings) {
        SettingsDialog(
            component = settingsComponent,
            onDismiss = { component.accept(ChatIntent.ToggleSettings(false)) },
        )
    }
}

@Composable
private fun ChatScreen(
    state: ChatState,
    settings: ApiSettings,
    component: ChatComponent,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(16.dp)
            .testTag(ChatContentTags.ROOT),
    ) {
        ChatHeader(settings, component)
        MessageListArea(state, listState)
        state.errorMessage?.let { error ->
            ErrorBanner(error, component)
        }
        Spacer(modifier = Modifier.height(12.dp))
        ChatInput(
            value = state.inputText,
            onValueChange = { component.accept(ChatIntent.UpdateInputText(it)) },
            onSend = { component.accept(ChatIntent.SendMessage) },
            isLoading = state.isLoading,
        )
    }
}

@Composable
private fun ChatHeader(settings: ApiSettings, component: ChatComponent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag(ChatContentTags.HEADER),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Z.ai Chat",
                style = MaterialTheme.typography.h5,
                color = AppColors.TextPrimary,
                modifier = Modifier.testTag(ChatContentTags.TITLE),
            )
            Text(
                text = "Model: ${settings.model}",
                style = MaterialTheme.typography.caption,
                color = AppColors.TextMuted,
                modifier = Modifier.testTag(ChatContentTags.MODEL_TEXT),
            )
        }

        HeaderActionButtons(component)
    }
}

@Composable
private fun HeaderActionButtons(component: ChatComponent) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(
            onClick = { component.accept(ChatIntent.ClearChat) },
            modifier = Modifier
                .background(AppColors.SurfaceLight, CircleShape)
                .size(40.dp)
                .testTag(ChatContentTags.CLEAR_BUTTON),
        ) {
            Icon(
                imageVector = Delete,
                contentDescription = "Clear chat",
                tint = AppColors.TextSecondary,
            )
        }

        IconButton(
            onClick = { component.accept(ChatIntent.ToggleSettings(true)) },
            modifier = Modifier
                .background(AppColors.Primary, CircleShape)
                .size(40.dp)
                .testTag(ChatContentTags.SETTINGS_BUTTON),
        ) {
            Icon(
                imageVector = Settings,
                contentDescription = "Settings",
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun ColumnScope.MessageListArea(state: ChatState, listState: androidx.compose.foundation.lazy.LazyListState) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        if (state.messages.isEmpty() && !state.isLoading) {
            EmptyStateContent()
        } else {
            MessageList(state, listState)
        }
    }
}

@Composable
private fun BoxScope.EmptyStateContent() {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .testTag(ChatContentTags.EMPTY_STATE),
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
}

@Composable
private fun MessageList(state: ChatState, listState: androidx.compose.foundation.lazy.LazyListState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(ChatContentTags.MESSAGE_LIST),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        itemsIndexed(state.messages, key = { index, _ -> index }) { _, message ->
            MessageBubble(message = message)
        }

        if (state.isLoading) {
            item {
                Box(modifier = Modifier.testTag(ChatContentTags.LOADING_INDICATOR)) {
                    TypingIndicator()
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(error: String, component: ChatComponent) {
    Surface(
        color = AppColors.Error.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(vertical = 8.dp)
            .testTag(ChatContentTags.ERROR_SURFACE),
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
                    .testTag(ChatContentTags.ERROR_TEXT),
                style = MaterialTheme.typography.body2,
            )
            IconButton(
                onClick = { component.accept(ChatIntent.ClearError) },
                modifier = Modifier.testTag(ChatContentTags.ERROR_DISMISS_BUTTON),
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

object ChatContentTags {
    const val ROOT = "chat_content_root"
    const val HEADER = "chat_content_header"
    const val TITLE = "chat_content_title"
    const val MODEL_TEXT = "chat_content_model_text"
    const val CLEAR_BUTTON = "chat_content_clear_button"
    const val SETTINGS_BUTTON = "chat_content_settings_button"
    const val MESSAGE_LIST = "chat_content_message_list"
    const val EMPTY_STATE = "chat_content_empty_state"
    const val ERROR_SURFACE = "chat_content_error_surface"
    const val ERROR_TEXT = "chat_content_error_text"
    const val ERROR_DISMISS_BUTTON = "chat_content_error_dismiss_button"
    const val LOADING_INDICATOR = "chat_content_loading_indicator"
}
