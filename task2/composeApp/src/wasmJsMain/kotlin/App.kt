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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import model.ChatMessage
import ui.components.ChatInput
import ui.components.MessageBubble
import ui.components.SettingsDialog
import ui.components.TypingIndicator
import ui.theme.AppColors
import ui.theme.AppTheme

@Composable
fun App() {
    var inputText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(ApiSettings()) }

    val client = remember { ChatClient() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    AppTheme {
        if (showSettings) {
            SettingsDialog(
                currentSettings = settings,
                onDismiss = { showSettings = false },
                onSave = { newSettings ->
                    settings = newSettings
                    showSettings = false
                },
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
                        text = "Model: ${settings.model}",
                        style = MaterialTheme.typography.caption,
                        color = AppColors.TextMuted,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            messages = emptyList()
                            errorMessage = null
                        },
                        modifier = Modifier
                            .background(AppColors.SurfaceLight, CircleShape)
                            .size(40.dp),
                    ) {
                        androidx.compose.material.Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear chat",
                            tint = AppColors.TextSecondary,
                        )
                    }

                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier
                            .background(AppColors.Primary, CircleShape)
                            .size(40.dp),
                    ) {
                        androidx.compose.material.Icon(
                            imageVector = Icons.Default.Settings,
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
                if (messages.isEmpty() && !isLoading) {
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
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(messages) { message ->
                            MessageBubble(message = message)
                        }

                        if (isLoading) {
                            item {
                                TypingIndicator()
                            }
                        }
                    }
                }
            }

            errorMessage?.let { error ->
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
                value = inputText,
                onValueChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank() && !isLoading) {
                        val userMessage = ChatMessage(
                            role = "user",
                            content = inputText,
                        )
                        messages = messages + userMessage
                        inputText = ""
                        isLoading = true
                        errorMessage = null

                        val constraints = settings.toResponseConstraints()

                        scope.launch {
                            val result = client.sendMessage(
                                apiKey = settings.apiKey,
                                model = settings.model,
                                messages = messages,
                                constraints = constraints,
                            )

                            isLoading = false
                            result.fold(
                                onSuccess = { response ->
                                    messages = messages + response
                                },
                                onFailure = { error ->
                                    errorMessage = error.message
                                },
                            )
                        }
                    }
                },
                isLoading = isLoading,
            )
        }
    }
}
