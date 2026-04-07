package chat.store

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import chat.ChatRepository
import com.arkivanov.mvikotlin.core.store.StoreFactory
import storage.StorageService

object ChatStoreProvider {

    fun provideChatStore(repository: ChatRepository, storage: StorageService, storeFactory: StoreFactory): ChatStore =
        ChatStoreFactory(
            storeFactory = storeFactory,
            repository = repository,
            storage = storage
        ).create()
}

@Composable
fun rememberChatStore(repository: ChatRepository, storage: StorageService, storeFactory: StoreFactory): ChatStore =
    remember(repository, storage) {
        ChatStoreProvider.provideChatStore(
            repository = repository,
            storage = storage,
            storeFactory = storeFactory
        )
    }
