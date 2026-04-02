package agent

import model.ChatMessage
import model.MetricRecord
import model.ReasoningComparison

data class AgentState(
    val context: AgentContext = AgentContext(),
    val config: AgentConfig = AgentConfig(),
    val messages: List<ChatMessage> = emptyList(),
    val metrics: List<MetricRecord> = emptyList(),
    val metricCounter: Int = 0,
    val agentMetrics: AgentMetrics = AgentMetrics(),
    val status: AgentStatus = AgentStatus.Idle,
    val inputText: String = "",
    val showSettings: Boolean = false,
    val showMetrics: Boolean = false,
    val showReasoning: Boolean = false,
    val reasoningComparison: ReasoningComparison = ReasoningComparison(
        task = "",
        results = emptyMap()
    ),
    val isReasoningLoading: Boolean = false,
) {
    val isLoading: Boolean get() = status == AgentStatus.Loading
    val errorMessage: String? get() = (status as? AgentStatus.Error)?.message
}
