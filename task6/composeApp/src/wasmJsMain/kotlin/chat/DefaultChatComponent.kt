package chat

import chat.store.ChatStore
import chat.store.ChatStoreFactory
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.states
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import storage.StorageService

class DefaultChatComponent(
    private val repository: ChatRepository,
    private val storage: StorageService,
    private val storeFactory: StoreFactory,
    componentContext: ComponentContext,
) : ChatComponent,
    ComponentContext by componentContext {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val store: ChatStore = ChatStoreFactory(
        storeFactory = storeFactory,
        repository = repository,
        storage = storage
    ).create()

    private val _state: MutableValue<ChatState> = MutableValue(store.state)

    override val state: Value<ChatState> = _state

    init {
        lifecycle.doOnDestroy {
            scope.cancel()
        }

        store.states
            .onEach { newState ->
                println("handle state = $newState")
                _state.value = newState
            }
            .launchIn(scope)

        store.labels
            .onEach { label ->
                when (label) {
                    is ChatLabel.ScrollToBottom -> Unit
                    is ChatLabel.ShowToast -> Unit
                    is ChatLabel.HideKeyboard -> Unit
                }
            }
            .launchIn(scope)
    }

    override fun accept(intent: ChatIntent) {
        store.accept(intent)
    }
}

class DefaultChatComponentFactory(private val storeFactory: StoreFactory, private val storage: StorageService) {
    fun create(repository: ChatRepository, componentContext: ComponentContext): DefaultChatComponent =
        DefaultChatComponent(
            repository = repository,
            storage = storage,
            storeFactory = storeFactory,
            componentContext = componentContext,
        )
}
