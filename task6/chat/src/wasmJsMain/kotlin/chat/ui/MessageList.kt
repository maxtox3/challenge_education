package chat.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import model.ChatMessage

@Composable
fun MessageList(messages: List<ChatMessage>, modifier: Modifier = Modifier,) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        items(messages) { message ->
            MessageBubble(message = message)
        }
    }
}
