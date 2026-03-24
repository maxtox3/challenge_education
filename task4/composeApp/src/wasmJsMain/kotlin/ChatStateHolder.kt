import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import model.ChatMessage
import model.ConstraintsInfo
import model.MetricRecord
import model.ReasoningComparison
import model.ReasoningMode
import model.ReasoningResult

class ChatStateHolder(val client: ChatClient, val scope: CoroutineScope, val listState: LazyListState,) {
    var inputText by mutableStateOf("")
    var messages by mutableStateOf<List<ChatMessage>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var showSettings by mutableStateOf(false)
    var showMetrics by mutableStateOf(false)
    var showReasoning by mutableStateOf(false)
    var metrics by mutableStateOf<List<MetricRecord>>(emptyList())
    var metricCounter by mutableStateOf(0)
    var settings by mutableStateOf(ApiSettings())
    var reasoningComparison by mutableStateOf(
        ReasoningComparison(
            task = "У тебя есть 12 монет, одна из которых фальшивая " +
                "(легче или тяжелее — неизвестно). У тебя есть чашечные весы. " +
                "Как найти фальшивую монету за минимальное количество взвешиваний?",
            results = emptyMap(),
        ),
    )
    var isReasoningLoading by mutableStateOf(false)

    fun clearChat() {
        messages = emptyList()
        errorMessage = null
    }

    fun updateSettings(newSettings: ApiSettings) {
        settings = newSettings
        showSettings = false
    }

    fun sendMessage() {
        if (inputText.isNotBlank() && !isLoading) {
            val promptText = inputText
            val userMessage = ChatMessage(
                role = "user",
                content = inputText,
            )
            messages = messages + userMessage
            inputText = ""
            isLoading = true
            errorMessage = null

            val constraints = settings.toResponseConstraints()
            val startTime = getTimeMillis()

            scope.launch {
                val result = client.sendMessage(
                    apiKey = settings.apiKey,
                    model = settings.model,
                    messages = messages,
                    constraints = constraints,
                )

                val responseTime = getTimeMillis() - startTime
                isLoading = false
                result.fold(
                    onSuccess = { response ->
                        messages = messages + response
                        metricCounter++
                        val record = MetricRecord(
                            id = metricCounter,
                            prompt = promptText,
                            response = response.content,
                            mode = response.mode,
                            responseLength = response.content.length,
                            tokensUsed = response.tokensUsed,
                            maxTokens = response.maxTokens,
                            finishReason = response.finishReason,
                            responseTimeMs = responseTime,
                            constraints = ConstraintsInfo(
                                maxTokens = settings.maxTokens,
                                stopSequences = settings.stopSequences
                                    .split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() },
                                responseFormat = settings.responseFormat,
                                temperature = settings.temperature,
                            ),
                        )
                        metrics = metrics + record
                    },
                    onFailure = { error ->
                        errorMessage = error.message
                    },
                )
            }
        }
    }

    fun runReasoningComparison(task: String) {
        val currentResults = mutableMapOf<ReasoningMode, ReasoningResult>()
        val loadingResults = ReasoningMode.entries.associateWith { mode ->
            ReasoningResult(
                mode = mode,
                systemPrompt = getSystemPromptForMode(mode, task),
                actualPrompt = task,
                isLoading = true,
            )
        }
        currentResults.putAll(loadingResults)
        reasoningComparison = ReasoningComparison(task, currentResults.toMap())
        isReasoningLoading = true

        ReasoningMode.entries.forEach { mode ->
            scope.launch {
                val systemPrompt = getSystemPromptForMode(mode, task)
                val startTime = getTimeMillis()

                val result = client.sendMessage(
                    apiKey = settings.apiKey,
                    model = settings.model,
                    messages = listOf(ChatMessage(role = "user", content = task)),
                    systemPrompt = systemPrompt,
                )

                val responseTime = getTimeMillis() - startTime

                result.fold(
                    onSuccess = { response ->
                        currentResults[mode] = ReasoningResult(
                            mode = mode,
                            systemPrompt = systemPrompt,
                            actualPrompt = task,
                            response = response.content,
                            responseTimeMs = responseTime,
                            tokensUsed = response.tokensUsed,
                            isLoading = false,
                        )
                        reasoningComparison = ReasoningComparison(task, currentResults.toMap())
                        isReasoningLoading = currentResults.values.any { it.isLoading }
                    },
                    onFailure = { error ->
                        currentResults[mode] = ReasoningResult(
                            mode = mode,
                            systemPrompt = systemPrompt,
                            actualPrompt = task,
                            isLoading = false,
                            error = error.message,
                        )
                        reasoningComparison = ReasoningComparison(task, currentResults.toMap())
                        isReasoningLoading = currentResults.values.any { it.isLoading }
                    },
                )
            }
        }
    }

    private fun getSystemPromptForMode(mode: ReasoningMode, task: String): String = when (mode) {
        ReasoningMode.DIRECT -> ""

        ReasoningMode.STEP_BY_STEP -> "Решай пошагово. Объясняй каждый шаг рассуждения подробно."

        ReasoningMode.META_PROMPT ->
            "Перед тем как ответить на задачу, сначала составь " +
                "оптимальный промпт для её решения, а затем используй его для получения ответа."

        ReasoningMode.EXPERT_PANEL -> """
            Ты группа из трёх экспертов:
            1. Аналитик - анализирует условие задачи и выделяет ключевые моменты
            2. Инженер - предлагает конкретное решение
            3. Критик - проверяет решение на ошибки и предлагает улучшения
            
            Каждый эксперт должен дать своё мнение по очереди. В конце дай итоговое решение.
        """.trimIndent()
    }
}

@Composable
fun rememberChatStateHolder(): ChatStateHolder {
    val client = remember { ChatClient() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    return remember(client, scope, listState) {
        ChatStateHolder(client, scope, listState)
    }
}
