package chat

import agent.AgentMsg
import agent.AgentState
import agent.AgentStatus
import agent.AgentStore
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.StateFlow
import settings.ApiSettings

class ChatViewModel(private val agentStore: AgentStore, val listState: LazyListState,) {
    val state: StateFlow<AgentState> = agentStore.stateFlow

    val inputText: String get() = state.value.inputText
    val messages get() = state.value.messages
    val isLoading: Boolean get() = state.value.status == AgentStatus.Loading
    val errorMessage: String? get() = (state.value.status as? AgentStatus.Error)?.message

    fun processIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.UpdateInputText -> handleUpdateInputText(intent.text)

            is ChatIntent.SendMessage -> handleSendMessage()

            is ChatIntent.ClearChat -> handleClearChat()

            is ChatIntent.UpdateSettings -> handleUpdateSettings(intent.settings)

            is ChatIntent.MessageSent -> {
                agentStore.dispatch(AgentMsg.Ui.AddMessage(intent.response))
            }

            is ChatIntent.SetError -> {
                agentStore.dispatch(AgentMsg.Ui.SetError(intent.message))
            }

            is ChatIntent.ClearError -> {
                agentStore.dispatch(AgentMsg.Ui.ClearError)
            }

            is ChatIntent.SetLoading -> {
                agentStore.dispatch(AgentMsg.Ui.SetLoading(intent.loading))
            }

            is ChatIntent.MessageSendFailed -> {
                agentStore.dispatch(AgentMsg.Ui.SetError("Failed to send message"))
                agentStore.dispatch(AgentMsg.Ui.SetLoading(false))
            }

            is ChatIntent.ToggleSettings,
            is ChatIntent.ToggleMetrics,
            is ChatIntent.ToggleReasoning,
            is ChatIntent.RunReasoningComparison,
            is ChatIntent.UpdateReasoningComparison,
            is ChatIntent.SetReasoningLoading -> Unit
        }
    }

    private fun handleUpdateInputText(text: String) {
        agentStore.dispatch(AgentMsg.Ui.UpdateInputText(text))
    }

    private fun handleSendMessage() {
        val currentInput = state.value.inputText
        if (currentInput.isNotBlank()) {
            agentStore.dispatch(AgentMsg.SendMessage(currentInput))
            agentStore.dispatch(AgentMsg.Ui.UpdateInputText(""))
        }
    }

    private fun handleClearChat() {
        agentStore.dispatch(AgentMsg.ClearContext)
    }

    private fun handleUpdateSettings(settings: ApiSettings) {
        val newConfig = state.value.config.copy(
            model = settings.model,
            temperature = settings.temperature.toFloat(),
            maxTokens = settings.maxTokens,
            enablePersistence = settings.apiKey.isNotEmpty()
        )
        agentStore.dispatch(AgentMsg.UpdateConfig(newConfig))
    }

    fun clearChat() {
        agentStore.dispatch(AgentMsg.ClearContext)
    }

    fun updateSettings(newSettings: ApiSettings) {
        val newConfig = state.value.config.copy(
            model = newSettings.model,
            temperature = newSettings.temperature.toFloat(),
            maxTokens = newSettings.maxTokens,
            enablePersistence = newSettings.apiKey.isNotEmpty()
        )
        agentStore.dispatch(AgentMsg.UpdateConfig(newConfig))
    }

    fun sendMessage(prompt: String) {
        if (prompt.isNotBlank()) {
            agentStore.dispatch(AgentMsg.SendMessage(prompt))
        }
    }
}

@Composable
fun rememberChatViewModel(agentStore: AgentStore): ChatViewModel {
    val listState = rememberLazyListState()

    return remember(agentStore, listState) {
        ChatViewModel(agentStore, listState)
    }
}
