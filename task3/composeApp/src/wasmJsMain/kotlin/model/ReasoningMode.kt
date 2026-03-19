package model

enum class ReasoningMode(val displayName: String, val description: String) {
    DIRECT("Прямой ответ", "Без дополнительных инструкций"),
    STEP_BY_STEP("Пошагово", "Инструкция: решай пошагово"),
    META_PROMPT("Мета-промпт", "Сначала составляет промпт, затем использует его"),
    EXPERT_PANEL("Эксперты", "Группа экспертов: аналитик, инженер, критик"),
}

data class ReasoningResult(
    val mode: ReasoningMode,
    val systemPrompt: String,
    val actualPrompt: String,
    val response: String = "",
    val responseTimeMs: Long = 0,
    val tokensUsed: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class ReasoningComparison(val task: String, val results: Map<ReasoningMode, ReasoningResult>,) {
    val isComplete: Boolean
        get() = results.values.all { !it.isLoading && it.error == null && it.response.isNotEmpty() }

    val hasAnyResult: Boolean
        get() = results.values.any { it.response.isNotEmpty() }
}
