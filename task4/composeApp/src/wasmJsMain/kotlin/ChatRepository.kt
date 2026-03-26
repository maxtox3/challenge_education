import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import model.ChatMessage
import model.ConstraintsInfo
import model.MetricRecord
import model.ReasoningComparison
import model.ReasoningMode
import model.ReasoningResult
import model.StreamChunk

interface ChatRepository {
    suspend fun sendMessage(prompt: String, messages: List<ChatMessage>, settings: ApiSettings): SendMessageResult

    suspend fun runReasoningComparison(
        task: String,
        settings: ApiSettings,
        onProgress: (ReasoningComparison) -> Unit,
    ): ReasoningComparison

    fun sendMessageStreaming(prompt: String, messages: List<ChatMessage>, settings: ApiSettings): Flow<StreamChunk>
}

sealed class SendMessageResult {
    data class Success(val response: ChatMessage, val metric: MetricRecord) : SendMessageResult()

    data class Error(val message: String) : SendMessageResult()
}

class ChatRepositoryImpl(private val client: ChatClient, private val scope: CoroutineScope) : ChatRepository {

    override suspend fun sendMessage(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings,
    ): SendMessageResult {
        val constraints = settings.toResponseConstraints()
        val startTime = getTimeMillis()

        return try {
            val result = client.sendMessage(
                apiKey = settings.apiKey,
                model = settings.model,
                messages = messages,
                constraints = constraints,
            )

            val responseTime = getTimeMillis() - startTime

            result.fold(
                onSuccess = { response ->
                    val metric = MetricRecord(
                        id = 0,
                        prompt = prompt,
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
                    SendMessageResult.Success(response, metric)
                },
                onFailure = { error ->
                    SendMessageResult.Error(error.message ?: "Unknown error")
                },
            )
        } catch (e: ChatException) {
            SendMessageResult.Error(e.message ?: "Unknown error")
        }
    }

    override fun sendMessageStreaming(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings,
    ): Flow<StreamChunk> {
        val constraints = settings.toResponseConstraints()
        return client.sendMessageStreaming(
            apiKey = settings.apiKey,
            model = settings.model,
            messages = messages,
            constraints = constraints,
        )
    }

    override suspend fun runReasoningComparison(
        task: String,
        settings: ApiSettings,
        onProgress: (ReasoningComparison) -> Unit,
    ): ReasoningComparison {
        val currentResults = mutableMapOf<ReasoningMode, ReasoningResult>()

        val loadingResults = ReasoningMode.entries.associateWith { mode ->
            ReasoningResult(
                mode = mode,
                systemPrompt = getSystemPromptForMode(mode),
                actualPrompt = task,
                isLoading = true,
            )
        }
        currentResults.putAll(loadingResults)
        onProgress(ReasoningComparison(task, currentResults.toMap()))

        val deferredResults = ReasoningMode.entries.map { mode ->
            scope.async {
                val systemPrompt = getSystemPromptForMode(mode)
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
                        ReasoningResult(
                            mode = mode,
                            systemPrompt = systemPrompt,
                            actualPrompt = task,
                            response = response.content,
                            responseTimeMs = responseTime,
                            tokensUsed = response.tokensUsed,
                            isLoading = false,
                        )
                    },
                    onFailure = { error ->
                        ReasoningResult(
                            mode = mode,
                            systemPrompt = systemPrompt,
                            actualPrompt = task,
                            isLoading = false,
                            error = error.message,
                        )
                    },
                )
            }
        }

        deferredResults.awaitAll().forEach { reasoningResult ->
            currentResults[reasoningResult.mode] = reasoningResult
            onProgress(ReasoningComparison(task, currentResults.toMap()))
        }

        return ReasoningComparison(task, currentResults.toMap())
    }

    private fun getSystemPromptForMode(mode: ReasoningMode): String = when (mode) {
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
