package root

import agent.AgentMsg
import agent.AgentState
import agent.AgentStore
import chat.ChatComponent
import chat.ChatIntent
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import settings.ApiSettings
import settings.SettingsComponent
import settings.SettingsIntent
import settings.SettingsState

class DefaultRootComponent(
    private val agentStore: AgentStore,
) : RootComponent {

    private val scope = CoroutineScope(Dispatchers.Main)

    private val _activeChild: MutableValue<RootComponent.ActiveChild> = MutableValue(
        RootComponent.ActiveChild.Chat(createChatComponent())
    )

    override val activeChild: Value<RootComponent.ActiveChild> = _activeChild

    override val chatComponent: ChatComponent = createChatComponent()
    override val settingsComponent: SettingsComponent = createSettingsComponent()

    private fun createChatComponent(): ChatComponent {
        return object : ChatComponent {
            private val _state: MutableValue<AgentState> = MutableValue(agentStore.stateFlow.value)

            init {
                agentStore.stateFlow
                    .onEach { newState ->
                        _state.value = newState
                    }
                    .launchIn(scope)
            }

            override val state: Value<AgentState> = _state

            override fun accept(intent: ChatIntent) {
                when (intent) {
                    is ChatIntent.UpdateInputText -> {
                        agentStore.dispatch(AgentMsg.Ui.UpdateInputText(intent.text))
                    }
                    is ChatIntent.SendMessage -> {
                        val currentInput = state.value.inputText
                        if (currentInput.isNotBlank()) {
                            agentStore.dispatch(AgentMsg.SendMessage(currentInput))
                            agentStore.dispatch(AgentMsg.Ui.UpdateInputText(""))
                        }
                    }
                    is ChatIntent.ClearChat -> {
                        agentStore.dispatch(AgentMsg.ClearContext)
                    }
                    is ChatIntent.UpdateSettings -> {
                        val newConfig = state.value.config.copy(
                            model = intent.settings.model,
                            temperature = intent.settings.temperature.toFloat(),
                            maxTokens = intent.settings.maxTokens,
                            enablePersistence = intent.settings.apiKey.isNotEmpty()
                        )
                        agentStore.dispatch(AgentMsg.UpdateConfig(newConfig))
                    }
                    is ChatIntent.ToggleSettings -> {
                        if (intent.show) {
                            showSettings()
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun createSettingsComponent(): SettingsComponent {
        val currentSettings = ApiSettings(
            model = agentStore.stateFlow.value.config.model,
            temperature = agentStore.stateFlow.value.config.temperature?.toDouble() ?: 1.0,
            maxTokens = agentStore.stateFlow.value.config.maxTokens
        )

        return object : SettingsComponent {
            private val _state: MutableValue<SettingsState> = MutableValue(SettingsState(settings = currentSettings))

            override val state: Value<SettingsState> = _state

            override fun accept(intent: SettingsIntent) {
                when (intent) {
                    is SettingsIntent.UpdateApiKey -> {
                        _state.update { it.copy(settings = it.settings.copy(apiKey = intent.apiKey)) }
                    }
                    is SettingsIntent.UpdateModel -> {
                        _state.update { it.copy(settings = it.settings.copy(model = intent.model)) }
                    }
                    is SettingsIntent.UpdateMaxTokens -> {
                        _state.update { it.copy(settings = it.settings.copy(maxTokens = intent.maxTokens)) }
                    }
                    is SettingsIntent.UpdateTemperature -> {
                        _state.update { it.copy(settings = it.settings.copy(temperature = intent.temperature)) }
                    }
                    is SettingsIntent.UpdateStopSequences -> {
                        _state.update { it.copy(settings = it.settings.copy(stopSequences = intent.stopSequences)) }
                    }
                    is SettingsIntent.UpdateResponseFormat -> {
                        _state.update { it.copy(settings = it.settings.copy(responseFormat = intent.responseFormat)) }
                    }
                    is SettingsIntent.SaveSettings -> {
                        val currentSettings = _state.value.settings
                        agentStore.dispatch(
                            AgentMsg.UpdateConfig(
                                agentStore.stateFlow.value.config.copy(
                                    model = currentSettings.model,
                                    temperature = currentSettings.temperature.toFloat(),
                                    maxTokens = currentSettings.maxTokens
                                )
                            )
                        )
                        showChat()
                    }
                    is SettingsIntent.ResetSettings -> {
                        _state.update { it.copy(settings = ApiSettings()) }
                    }
                    is SettingsIntent.ClearValidationError -> {
                        _state.update { it.copy(validationError = null) }
                    }
                    is SettingsIntent.ToggleApiKeyVisibility -> {
                        _state.update { it.copy(isApiKeyVisible = !it.isApiKeyVisible) }
                    }
                    is SettingsIntent.ValidateApiKey -> {
                        val error = if (intent.apiKey.isBlank()) "API key cannot be empty" else null
                        _state.update { it.copy(validationError = error) }
                    }
                    is SettingsIntent.UpdateSettings -> {
                        _state.update { it.copy(settings = intent.settings) }
                    }
                }
            }
        }
    }

    override fun showChat() {
        _activeChild.value = RootComponent.ActiveChild.Chat(chatComponent)
    }

    override fun showSettings() {
        _activeChild.value = RootComponent.ActiveChild.Settings(settingsComponent)
    }
}
