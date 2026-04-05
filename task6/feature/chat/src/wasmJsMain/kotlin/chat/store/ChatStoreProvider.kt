package chat.store

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import chat.ChatRepository
import com.arkivanov.mvikotlin.core.store.StoreFactory

object ChatStoreProvider {

    fun provideChatStore(repository: ChatRepository, storeFactory: StoreFactory): ChatStore =
        ChatStoreFactory(
            storeFactory = storeFactory,
            repository = repository
        ).create()
}

@Composable
fun rememberChatStore(repository: ChatRepository, storeFactory: StoreFactory): ChatStore =
    remember(repository) {
        ChatStoreProvider.provideChatStore(
            repository = repository,
            storeFactory = storeFactory
        )
    }
