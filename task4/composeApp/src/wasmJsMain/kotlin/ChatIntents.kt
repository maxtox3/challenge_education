/**
 * Handles intent processing for ChatViewModel.
 * Extracted to reduce complexity in processIntent method.
 */
class ChatIntents(private val viewModel: ChatViewModel) {
    fun handleInputIntent(intent: ChatIntent.UpdateInputText) {
        viewModel.inputText = intent.text
    }

    fun handleMessageIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SendMessage -> viewModel.sendMessage()
            is ChatIntent.MessageSent -> viewModel.handleMessageSent(intent.response, intent.metric)
            is ChatIntent.MessageSendFailed -> viewModel.isLoading = false
            else -> {}
        }
    }

    fun handleSettingsIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.UpdateSettings -> viewModel.updateSettings(intent.settings)
            is ChatIntent.ToggleSettings -> viewModel.showSettings = intent.show
            else -> {}
        }
    }

    fun handleMetricsIntent(intent: ChatIntent.ToggleMetrics) {
        viewModel.showMetrics = intent.show
    }

    fun handleReasoningIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.ToggleReasoning -> viewModel.showReasoning = intent.show
            is ChatIntent.RunReasoningComparison -> viewModel.runReasoningComparison(intent.task)
            is ChatIntent.UpdateReasoningComparison -> viewModel.reasoningComparison = intent.comparison
            is ChatIntent.SetReasoningLoading -> viewModel.isReasoningLoading = intent.loading
            else -> {}
        }
    }

    fun handleErrorIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SetError -> viewModel.errorMessage = intent.message
            is ChatIntent.ClearError -> viewModel.errorMessage = null
            is ChatIntent.SetLoading -> viewModel.isLoading = intent.loading
            else -> {}
        }
    }

    fun handleClearChatIntent() {
        viewModel.clearChat()
    }
}
