package chat.store

import chat.ChatIntent
import chat.ChatLabel
import chat.ChatState
import com.arkivanov.mvikotlin.core.store.Store

interface ChatStore : Store<ChatIntent, ChatState, ChatLabel>
