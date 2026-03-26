package chat

import model.ChatMessage
import model.MetricRecord
import model.ReasoningComparison
import settings.ApiSettings

data class ChatState(
    val inputText: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showSettings: Boolean = false,
    val showMetrics: Boolean = false,
    val showReasoning: Boolean = false,
    val metrics: List<MetricRecord> = emptyList(),
    val metricCounter: Int = 0,
    val settings: ApiSettings = ApiSettings(),
    val reasoningComparison: ReasoningComparison = ReasoningComparison(
        task = "У тебя есть 12 монет, одна из которых фальшивая " +
            "(легче или тяжелее — неизвестно). У тебя есть чашечные весы. " +
            "Как найти фальшивую монету за минимальное количество взвешиваний?",
        results = emptyMap(),
    ),
    val isReasoningLoading: Boolean = false,
)
