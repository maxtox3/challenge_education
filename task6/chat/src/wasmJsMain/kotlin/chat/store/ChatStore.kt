package chat.store

import chat.contract.ChatContract
import com.arkivanov.mvikotlin.core.store.Store

interface ChatStore : Store<ChatContract.Intent, ChatContract.State, ChatContract.Label>
