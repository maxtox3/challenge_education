import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import model.ChatMessage
import model.MetricRecord
import model.ReasoningComparison

class ChatStateHolder(
    private val repository: ChatRepository,
    val scope: CoroutineScope,
    val listState: LazyListState,
) {
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
        when (val result = MessageHandler.validateAndPrepare(inputText, isLoading)) {
            is MessageHandler.ValidationResult.Valid -> {
                messages = messages + result.message
                inputText = ""
                isLoading = true
                errorMessage = null

                scope.launch {
                    val apiResult = repository.sendMessage(
                        prompt = result.prompt,
                        messages = messages,
                        settings = settings,
                    )

                    isLoading = false

                    when (apiResult) {
                        is SendMessageResult.Success -> {
                            messages = messages + apiResult.response
                            metricCounter++
                            val metric = apiResult.metric.copy(id = metricCounter)
                            metrics = metrics + metric
                        }

                        is SendMessageResult.Error -> {
                            errorMessage = apiResult.message
                        }
                    }
                }
            }

            is MessageHandler.ValidationResult.Invalid -> {
                // Input is blank or already loading - do nothing
            }
        }
    }

    fun runReasoningComparison(task: String) {
        isReasoningLoading = true

        scope.launch {
            repository.runReasoningComparison(
                task = task,
                settings = settings,
                onProgress = { comparison ->
                    reasoningComparison = comparison
                    isReasoningLoading = comparison.results.values.any { it.isLoading }
                },
            )
        }
    }
}

@Composable
fun rememberChatStateHolder(): ChatStateHolder {
    val client = remember { ChatClient() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val repository = remember(client) { ChatRepositoryImpl(client, scope) }

    return remember(repository, scope, listState) {
        ChatStateHolder(repository, scope, listState)
    }
}
