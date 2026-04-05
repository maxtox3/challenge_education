package agent

import model.ChatMessage
import model.MetricRecord
import model.ReasoningComparison
import model.StreamChunk

sealed class AgentMsg {
    data class SendMessage(val prompt: String) : AgentMsg()
    data object ClearContext : AgentMsg()
    data class HandleApiResponse(val response: String, val tokens: Int) : AgentMsg()
    data class HandleApiError(val error: String) : AgentMsg()
    data class HandleStreamChunk(val chunk: StreamChunk) : AgentMsg()
    data object LoadContextFromStorage : AgentMsg()
    data class ContextLoaded(val context: AgentContext) : AgentMsg()
    data class ContextSaved(val success: Boolean) : AgentMsg()
    data object CompressHistory : AgentMsg()
    data class HistoryCompressed(val summary: String) : AgentMsg()
    data class UpdateConfig(val config: AgentConfig) : AgentMsg()

    sealed class Ui : AgentMsg() {
        data class UpdateInputText(val text: String) : Ui()
        data class ToggleSettings(val show: Boolean) : Ui()
        data class ToggleMetrics(val show: Boolean) : Ui()
        data class ToggleReasoning(val show: Boolean) : Ui()
        data class UpdateReasoningComparison(val comparison: ReasoningComparison) : Ui()
        data class SetReasoningLoading(val loading: Boolean) : Ui()
        data class AddMetric(val metric: MetricRecord) : Ui()
        data class AddMessage(val message: ChatMessage) : Ui()
        data class SetError(val error: String?) : Ui()
        data object ClearError : Ui()
        data class SetLoading(val isLoading: Boolean) : Ui()
    }
}
