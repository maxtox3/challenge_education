import model.ChatMessage
import model.MetricRecord
import model.ReasoningComparison

sealed class ChatIntent {
    data class UpdateInputText(val text: String) : ChatIntent()
    data object SendMessage : ChatIntent()
    data object ClearChat : ChatIntent()
    data class UpdateSettings(val settings: ApiSettings) : ChatIntent()
    data class ToggleSettings(val show: Boolean) : ChatIntent()
    data class ToggleMetrics(val show: Boolean) : ChatIntent()
    data class ToggleReasoning(val show: Boolean) : ChatIntent()
    data class RunReasoningComparison(val task: String) : ChatIntent()
    data class MessageSent(val response: ChatMessage, val metric: MetricRecord) : ChatIntent()
    data object MessageSendFailed : ChatIntent()
    data class SetError(val message: String?) : ChatIntent()
    data object ClearError : ChatIntent()
    data class SetLoading(val loading: Boolean) : ChatIntent()
    data class UpdateReasoningComparison(val comparison: ReasoningComparison) : ChatIntent()
    data class SetReasoningLoading(val loading: Boolean) : ChatIntent()
}
