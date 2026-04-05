package root

import agent.AgentMsg
import agent.AgentState
import agent.AgentStore
import chat.ChatComponent
import chat.ChatIntent
import chat.ChatState
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import settings.ApiSettings
import settings.DefaultSettingsComponent
import settings.SettingsComponent

class DefaultRootComponent(private val agentStore: AgentStore,) : RootComponent {

    private val scope = CoroutineScope(Dispatchers.Main)

    override val chatComponent: ChatComponent = createChatComponent()
    override val settingsComponent: SettingsComponent = DefaultSettingsComponent(
        initialSettings = ApiSettings(
            model = agentStore.stateFlow.value.config.model,
            temperature = agentStore.stateFlow.value.config.temperature?.toDouble() ?: 1.0,
            maxTokens = agentStore.stateFlow.value.config.maxTokens
        )
    )

    private val _activeChild: MutableValue<RootComponent.ActiveChild> = MutableValue(
        RootComponent.ActiveChild.Chat(chatComponent)
    )
    override val activeChild: Value<RootComponent.ActiveChild> = _activeChild

    private fun createChatComponent(): ChatComponent = object : ChatComponent {
        private val _state: MutableValue<ChatState> = MutableValue(agentStore.stateFlow.value.toChatState())

        init {
            agentStore.stateFlow
                .onEach { newState ->
                    _state.value = newState.toChatState()
                }
                .launchIn(scope)
        }
        override val state: Value<ChatState> = _state
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
                    val newConfig = agentStore.stateFlow.value.config.copy(
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

    override fun showChat() {
        _activeChild.value = RootComponent.ActiveChild.Chat(chatComponent)
    }

    override fun showSettings() {
        _activeChild.value = RootComponent.ActiveChild.Settings(settingsComponent)
    }

    private fun AgentState.toChatState(): ChatState = ChatState(
        inputText = inputText,
        messages = messages,
        isLoading = isLoading,
        errorMessage = errorMessage,
        showSettings = showSettings,
        showMetrics = showMetrics,
        showReasoning = showReasoning,
        metrics = metrics,
        metricCounter = metricCounter,
        settings = ApiSettings(
            model = config.model,
            temperature = config.temperature?.toDouble() ?: 1.0,
            maxTokens = config.maxTokens
        ),
        reasoningComparison = reasoningComparison,
        isReasoningLoading = isReasoningLoading
    )
}
