package settings

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
import kotlinx.serialization.json.Json
import settings.store.SettingsStore
import settings.store.SettingsStoreFactory
import storage.StorageService

class DefaultSettingsComponent(
    initialSettings: ApiSettings = ApiSettings(),
    private val storeFactory: StoreFactory,
    storage: StorageService,
    private val json: Json,
    private val onSettingsSaved: (ApiSettings) -> Unit = {},
    componentContext: ComponentContext,
) : SettingsComponent,
    ComponentContext by componentContext {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val store: SettingsStore = SettingsStoreFactory(
        storeFactory = storeFactory,
        storageService = storage,
        json = json,
        onSettingsSaved = onSettingsSaved
    ).create(initialState = SettingsState(settings = initialSettings))

    private val _state: MutableValue<SettingsState> = MutableValue(store.state)

    override val state: Value<SettingsState> = _state

    init {
        lifecycle.doOnDestroy {
            scope.cancel()
        }

        store.states
            .onEach { newState ->
                _state.value = newState
            }
            .launchIn(scope)

        store.labels
            .onEach { label ->
                when (label) {
                    is SettingsLabel.SettingsSaved -> Unit
                    is SettingsLabel.ValidationError -> Unit
                    is SettingsLabel.ShowToast -> Unit
                }
            }
            .launchIn(scope)
    }

    override fun accept(intent: SettingsIntent) {
        store.accept(intent)
    }
}
