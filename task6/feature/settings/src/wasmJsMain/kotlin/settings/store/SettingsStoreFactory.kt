package settings.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import settings.ApiSettings
import settings.SettingsIntent
import settings.SettingsLabel
import settings.SettingsState
import storage.StorageService

private sealed class Msg {
    data class UpdateSettings(val settings: ApiSettings) : Msg()
    data class UpdateLoading(val isLoading: Boolean) : Msg()
    data class UpdateValidationError(val error: String?) : Msg()
    data class UpdateApiKeyVisibility(val isVisible: Boolean) : Msg()
}

class SettingsStoreFactory(
    private val storeFactory: StoreFactory,
    private val storageService: StorageService,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val onSettingsSaved: (ApiSettings) -> Unit,
) {
    fun create(initialState: SettingsState = SettingsState()): SettingsStore = object :
        SettingsStore,
        Store<SettingsIntent, SettingsState, SettingsLabel> by storeFactory.create(
            name = "SettingsStore",
            initialState = initialState,
            bootstrapper = SimpleBootstrapper(Unit),
            executorFactory = { ExecutorImpl() },
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl : CoroutineExecutor<SettingsIntent, Unit, SettingsState, Msg, SettingsLabel>() {
        override fun executeAction(action: Unit) {
            scope.launch {
                loadSettings()
            }
        }

        private suspend fun loadSettings() {
            try {
                val storedSettings = storageService.getApiSettings()
                if (storedSettings != null) {
                    val settings = json.decodeFromString<ApiSettings>(storedSettings)
                    dispatch(Msg.UpdateSettings(settings))
                }
            } catch (e: SerializationException) {
                publish(SettingsLabel.ValidationError("Failed to load settings: ${e.message}"))
            } catch (e: IllegalArgumentException) {
                publish(SettingsLabel.ValidationError("Invalid settings format: ${e.message}"))
            }
        }

        override fun executeIntent(intent: SettingsIntent) {
            when (intent) {
                is SettingsIntent.UpdateApiKey -> {
                    dispatch(Msg.UpdateSettings(state().settings.copy(apiKey = intent.apiKey)))
                }

                is SettingsIntent.UpdateModel -> {
                    dispatch(Msg.UpdateSettings(state().settings.copy(model = intent.model)))
                }

                is SettingsIntent.UpdateMaxTokens -> {
                    dispatch(Msg.UpdateSettings(state().settings.copy(maxTokens = intent.maxTokens)))
                }

                is SettingsIntent.UpdateTemperature -> {
                    dispatch(Msg.UpdateSettings(state().settings.copy(temperature = intent.temperature)))
                }

                is SettingsIntent.UpdateStopSequences -> {
                    dispatch(Msg.UpdateSettings(state().settings.copy(stopSequences = intent.stopSequences)))
                }

                is SettingsIntent.UpdateResponseFormat -> {
                    dispatch(Msg.UpdateSettings(state().settings.copy(responseFormat = intent.responseFormat)))
                }

                is SettingsIntent.UpdateSettings -> {
                    dispatch(Msg.UpdateSettings(intent.settings))
                }

                is SettingsIntent.SaveSettings -> {
                    saveSettings()
                }

                is SettingsIntent.ResetSettings -> {
                    dispatch(Msg.UpdateSettings(ApiSettings()))
                    dispatch(Msg.UpdateValidationError(null))
                    scope.launch {
                        clearSettings()
                    }
                }

                is SettingsIntent.ClearValidationError -> {
                    dispatch(Msg.UpdateValidationError(null))
                }

                is SettingsIntent.ToggleApiKeyVisibility -> {
                    dispatch(Msg.UpdateApiKeyVisibility(!state().isApiKeyVisible))
                }

                is SettingsIntent.ValidateApiKey -> {
                    val error = if (intent.apiKey.isBlank()) "API key cannot be empty" else null
                    dispatch(Msg.UpdateValidationError(error))
                }
            }
        }

        private fun saveSettings() {
            val currentSettings = state().settings
            val error = when {
                currentSettings.apiKey.isBlank() -> "API key is required"
                currentSettings.temperature !in 0.0..2.0 -> "Temperature must be between 0.0 and 2.0"
                else -> null
            }
            dispatch(Msg.UpdateValidationError(error))
            if (error == null) {
                onSettingsSaved(currentSettings)
                scope.launch {
                    try {
                        val encoded = json.encodeToString(currentSettings)
                        storageService.setApiSettings(encoded)
                        publish(SettingsLabel.SettingsSaved)
                    } catch (e: SerializationException) {
                        publish(SettingsLabel.ValidationError("Failed to save settings: ${e.message}"))
                    }
                }
            }
        }

        private suspend fun clearSettings() {
            try {
                storageService.setApiSettings(json.encodeToString(ApiSettings()))
            } catch (e: SerializationException) {
                publish(SettingsLabel.ValidationError("Failed to clear settings: ${e.message}"))
            }
        }
    }

    private object ReducerImpl : Reducer<SettingsState, Msg> {
        override fun SettingsState.reduce(msg: Msg): SettingsState = when (msg) {
            is Msg.UpdateSettings -> copy(settings = msg.settings)
            is Msg.UpdateLoading -> copy(isLoading = msg.isLoading)
            is Msg.UpdateValidationError -> copy(validationError = msg.error)
            is Msg.UpdateApiKeyVisibility -> copy(isApiKeyVisible = msg.isVisible)
        }
    }
}
