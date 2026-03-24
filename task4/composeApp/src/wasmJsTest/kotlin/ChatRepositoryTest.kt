import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import model.ChatMessage
import model.ReasoningComparison
import model.ReasoningMode
import kotlin.coroutines.CoroutineContext
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class ImmediateDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        block.run()
    }
}

class ChatRepositoryTest {
    private lateinit var fakeClient: FakeChatClient
    private lateinit var repository: ChatRepositoryImpl
    private lateinit var testScope: CoroutineScope

    @BeforeTest
    fun setup() {
        fakeClient = FakeChatClient()
        testScope = CoroutineScope(SupervisorJob() + ImmediateDispatcher())
        repository = ChatRepositoryImpl(client = fakeClient, scope = testScope)
    }

    @Test
    fun testSendMessage_Success() {
        val response = ChatMessage(
            role = "assistant",
            content = "Hello, how can I help?",
            mode = "free",
            tokensUsed = 50,
            maxTokens = null,
            finishReason = "stop",
        )
        fakeClient.sendResult = Result.success(response)

        val settings = ApiSettings(
            apiKey = "test-key",
            model = "glm-5",
            maxTokens = 100,
            temperature = 0.8,
            stopSequences = "END,STOP",
            responseFormat = "text",
        )
        val messages = listOf(ChatMessage(role = "user", content = "Hello"))

        lateinit var result: SendMessageResult
        testScope.launch {
            result = repository.sendMessage("Hello", messages, settings)
        }

        assertTrue(result is SendMessageResult.Success)
        assertEquals("Hello, how can I help?", (result as SendMessageResult.Success).response.content)
        assertEquals("free", (result as SendMessageResult.Success).metric.mode)
        assertEquals(50, (result as SendMessageResult.Success).metric.tokensUsed)
    }

    @Test
    fun testSendMessage_Error() {
        fakeClient.sendResult = Result.failure(Exception("API Error: 401"))

        val settings = ApiSettings(apiKey = "invalid-key", model = "glm-5")
        val messages = listOf(ChatMessage(role = "user", content = "Hello"))

        lateinit var result: SendMessageResult
        testScope.launch {
            result = repository.sendMessage("Hello", messages, settings)
        }

        assertTrue(result is SendMessageResult.Error)
        assertEquals("API Error: 401", (result as SendMessageResult.Error).message)
    }

    @Test
    fun testSendMessage_MetricRecordCorrectness() {
        val response = ChatMessage(
            role = "assistant",
            content = "Test response content",
            mode = "free",
            tokensUsed = 15,
            maxTokens = null,
            finishReason = "stop",
        )
        fakeClient.sendResult = Result.success(response)

        val settings = ApiSettings(
            apiKey = "test-key",
            model = "glm-5",
            maxTokens = 200,
            temperature = 0.9,
            stopSequences = "STOP, END",
            responseFormat = "text",
        )
        val messages = listOf(ChatMessage(role = "user", content = "Test prompt"))

        lateinit var result: SendMessageResult.Success
        testScope.launch {
            result = repository.sendMessage("Test prompt", messages, settings) as SendMessageResult.Success
        }

        val metric = result.metric
        assertEquals(0, metric.id)
        assertEquals("Test prompt", metric.prompt)
        assertEquals("Test response content", metric.response)
        assertEquals("free", metric.mode)
        assertEquals("Test response content".length, metric.responseLength)
        assertEquals(15, metric.tokensUsed)
        assertNull(metric.maxTokens)
        assertEquals("stop", metric.finishReason)
        assertTrue(metric.responseTimeMs >= 0)
        assertEquals(200, metric.constraints.maxTokens)
        assertEquals(listOf("STOP", "END"), metric.constraints.stopSequences)
        assertEquals("text", metric.constraints.responseFormat)
        assertEquals(0.9, metric.constraints.temperature)
    }

    @Test
    fun testSendMessage_WithConstraints() {
        val response = ChatMessage(
            role = "assistant",
            content = "Constrained response",
            mode = "constrained",
            tokensUsed = 20,
            maxTokens = 100,
            finishReason = "stop",
        )
        fakeClient.sendResult = Result.success(response)

        val settings = ApiSettings(
            apiKey = "test-key",
            model = "glm-5",
            maxTokens = 100,
            temperature = 0.5,
            stopSequences = "END",
            responseFormat = "json",
        )
        val messages = listOf(ChatMessage(role = "user", content = "Test"))

        lateinit var result: SendMessageResult
        testScope.launch {
            result = repository.sendMessage("Test", messages, settings)
        }

        assertTrue(result is SendMessageResult.Success)
        assertEquals("Constrained response", (result as SendMessageResult.Success).response.content)
        assertEquals(100, fakeClient.lastConstraints?.maxTokens)
        assertEquals(0.5, fakeClient.lastConstraints?.temperature)
        assertEquals(listOf("END"), fakeClient.lastConstraints?.stop)
        assertNotNull(fakeClient.lastConstraints?.responseFormat)
    }

    @Test
    fun testSendMessage_ConstraintsInfoParsing() {
        val response = ChatMessage(
            role = "assistant",
            content = "Response",
            mode = "free",
            tokensUsed = 10,
            finishReason = "stop",
        )
        fakeClient.sendResult = Result.success(response)

        val settings = ApiSettings(
            apiKey = "key",
            model = "model",
            maxTokens = 500,
            temperature = 1.5,
            stopSequences = "  a  ,  b  ,  c  ",
            responseFormat = "text",
        )

        lateinit var result: SendMessageResult.Success
        testScope.launch {
            result = repository.sendMessage("prompt", emptyList(), settings) as SendMessageResult.Success
        }

        assertEquals(listOf("a", "b", "c"), result.metric.constraints.stopSequences)
        assertEquals(1.5, result.metric.constraints.temperature)
    }

    @Test
    fun testSendMessage_EmptyStopSequences() {
        val response = ChatMessage(role = "assistant", content = "Response")
        fakeClient.sendResult = Result.success(response)

        val settings = ApiSettings(
            apiKey = "key",
            model = "model",
            stopSequences = "  ,  ,  ",
        )

        lateinit var result: SendMessageResult.Success
        testScope.launch {
            result = repository.sendMessage("prompt", emptyList(), settings) as SendMessageResult.Success
        }

        assertEquals(emptyList<String>(), result.metric.constraints.stopSequences)
    }

    @Test
    fun testSendMessage_WithJsonResponseFormat() {
        val response = ChatMessage(
            role = "assistant",
            content = """{"result": "ok"}""",
            mode = "constrained",
            tokensUsed = 5,
        )
        fakeClient.sendResult = Result.success(response)

        val settings = ApiSettings(
            apiKey = "key",
            model = "model",
            responseFormat = "json",
        )

        testScope.launch {
            repository.sendMessage("prompt", emptyList(), settings)
        }

        assertNotNull(fakeClient.lastConstraints?.responseFormat)
        assertEquals("json_object", fakeClient.lastConstraints?.responseFormat?.type)
    }

    @Test
    fun testRunReasoningComparison_ParallelExecution() {
        fakeClient.sendResult = Result.success(
            ChatMessage(role = "assistant", content = "Test response", tokensUsed = 10)
        )

        val settings = ApiSettings(apiKey = "key", model = "model")
        val progressUpdates = mutableListOf<ReasoningComparison>()

        lateinit var result: ReasoningComparison
        testScope.launch {
            result = repository.runReasoningComparison(
                task = "Test task",
                settings = settings,
                onProgress = { progressUpdates.add(it) }
            )
        }

        assertEquals(4, result.results.size)
        assertEquals(ReasoningMode.entries.size, result.results.size)
        assertTrue(progressUpdates.isNotEmpty())
    }

    @Test
    fun testRunReasoningComparison_InitialLoadingState() {
        fakeClient.sendResult = Result.success(
            ChatMessage(role = "assistant", content = "Response")
        )

        val settings = ApiSettings(apiKey = "key", model = "model")
        var firstProgress: ReasoningComparison? = null

        testScope.launch {
            repository.runReasoningComparison(
                task = "Test",
                settings = settings,
                onProgress = { comparison ->
                    if (firstProgress == null) {
                        firstProgress = comparison
                    }
                }
            )
        }

        assertNotNull(firstProgress)
        firstProgress!!.results.values.forEach { res ->
            assertTrue(res.isLoading)
            assertNull(res.error)
            assertEquals("", res.response)
        }
    }

    @Test
    fun testRunReasoningComparison_FinalResultsNotLoading() {
        fakeClient.sendResult = Result.success(
            ChatMessage(role = "assistant", content = "Response", tokensUsed = 5)
        )

        val settings = ApiSettings(apiKey = "key", model = "model")
        lateinit var result: ReasoningComparison
        testScope.launch {
            result = repository.runReasoningComparison(
                task = "Test task",
                settings = settings,
                onProgress = {}
            )
        }

        result.results.values.forEach { reasoningResult ->
            assertFalse(reasoningResult.isLoading)
        }
    }

    @Test
    fun testRunReasoningComparison_SystemPromptsForModes() {
        fakeClient.sendResult = Result.success(
            ChatMessage(role = "assistant", content = "Response")
        )

        val settings = ApiSettings(apiKey = "key", model = "model")
        lateinit var result: ReasoningComparison
        testScope.launch {
            result = repository.runReasoningComparison(
                task = "Test",
                settings = settings,
                onProgress = {}
            )
        }

        assertEquals("", result.results[ReasoningMode.DIRECT]?.systemPrompt)
        assertTrue(result.results[ReasoningMode.STEP_BY_STEP]?.systemPrompt?.contains("пошагово") == true)
        assertTrue(result.results[ReasoningMode.META_PROMPT]?.systemPrompt?.contains("промпт") == true)
        assertTrue(result.results[ReasoningMode.EXPERT_PANEL]?.systemPrompt?.contains("экспертов") == true)
    }

    @Test
    fun testRunReasoningComparison_WithError() {
        fakeClient.sendResult = Result.failure(Exception("Network error"))

        val settings = ApiSettings(apiKey = "key", model = "model")
        lateinit var result: ReasoningComparison
        testScope.launch {
            result = repository.runReasoningComparison(
                task = "Test",
                settings = settings,
                onProgress = {}
            )
        }

        result.results.values.forEach { reasoningResult ->
            assertFalse(reasoningResult.isLoading)
            assertEquals("Network error", reasoningResult.error)
            assertEquals("", reasoningResult.response)
        }
    }

    @Test
    fun testRunReasoningComparison_TokensUsedCaptured() {
        fakeClient.sendResult = Result.success(
            ChatMessage(role = "assistant", content = "Response", tokensUsed = 42)
        )

        val settings = ApiSettings(apiKey = "key", model = "model")
        lateinit var result: ReasoningComparison
        testScope.launch {
            result = repository.runReasoningComparison(
                task = "Test",
                settings = settings,
                onProgress = {}
            )
        }

        result.results.values.forEach { reasoningResult ->
            assertEquals(42, reasoningResult.tokensUsed)
        }
    }

    @Test
    fun testRunReasoningComparison_ResponseTimeMs() {
        fakeClient.sendResult = Result.success(
            ChatMessage(role = "assistant", content = "Response")
        )

        val settings = ApiSettings(apiKey = "key", model = "model")
        lateinit var result: ReasoningComparison
        testScope.launch {
            result = repository.runReasoningComparison(
                task = "Test",
                settings = settings,
                onProgress = {}
            )
        }

        result.results.values.forEach { reasoningResult ->
            assertTrue(reasoningResult.responseTimeMs >= 0)
        }
    }

    @Test
    fun testRunReasoningComparison_ProgressUpdatesCount() {
        fakeClient.sendResult = Result.success(
            ChatMessage(role = "assistant", content = "Response")
        )

        val settings = ApiSettings(apiKey = "key", model = "model")
        val progressUpdates = mutableListOf<ReasoningComparison>()

        testScope.launch {
            repository.runReasoningComparison(
                task = "Test",
                settings = settings,
                onProgress = { progressUpdates.add(it) }
            )
        }

        val expectedUpdates = ReasoningMode.entries.size + 1
        assertEquals(expectedUpdates, progressUpdates.size)
    }

    @Test
    fun testRunReasoningComparison_TaskPassedCorrectly() {
        fakeClient.sendResult = Result.success(
            ChatMessage(role = "assistant", content = "Response")
        )

        val task = "Solve this complex problem"
        val settings = ApiSettings(apiKey = "key", model = "model")
        lateinit var result: ReasoningComparison
        testScope.launch {
            result = repository.runReasoningComparison(
                task = task,
                settings = settings,
                onProgress = {}
            )
        }

        assertEquals(task, result.task)
        result.results.values.forEach { reasoningResult ->
            assertEquals(task, reasoningResult.actualPrompt)
        }
    }

    @Test
    fun testSendMessage_UnknownErrorMessage() {
        fakeClient.sendResult = Result.failure(Exception())

        val settings = ApiSettings(apiKey = "key", model = "model")
        lateinit var result: SendMessageResult
        testScope.launch {
            result = repository.sendMessage("prompt", emptyList(), settings)
        }

        assertTrue(result is SendMessageResult.Error)
        assertEquals("Unknown error", (result as SendMessageResult.Error).message)
    }

    @Test
    fun testSendMessage_CatchesException() {
        fakeClient.shouldThrow = true

        val settings = ApiSettings(apiKey = "key", model = "model")
        lateinit var result: SendMessageResult
        testScope.launch {
            result = repository.sendMessage("prompt", emptyList(), settings)
        }

        assertTrue(result is SendMessageResult.Error)
        assertEquals("Test exception", (result as SendMessageResult.Error).message)
    }

    @Test
    fun testMetricRecord_ResponseLengthMatchesContent() {
        val content = "This is a test response with specific length"
        val response = ChatMessage(
            role = "assistant",
            content = content,
            mode = "free",
            tokensUsed = 10,
        )
        fakeClient.sendResult = Result.success(response)

        lateinit var result: SendMessageResult.Success
        testScope.launch {
            result = repository.sendMessage("prompt", emptyList(), ApiSettings()) as SendMessageResult.Success
        }

        assertEquals(content.length, result.metric.responseLength)
        assertEquals(content.length, result.metric.response.length)
    }

    @Test
    fun testSendMessage_MultipleMessages() {
        val response = ChatMessage(role = "assistant", content = "Multi-message response")
        fakeClient.sendResult = Result.success(response)

        val messages = listOf(
            ChatMessage(role = "system", content = "You are helpful"),
            ChatMessage(role = "user", content = "Hello"),
            ChatMessage(role = "assistant", content = "Hi there"),
            ChatMessage(role = "user", content = "How are you?"),
        )

        lateinit var result: SendMessageResult
        testScope.launch {
            result = repository.sendMessage("How are you?", messages, ApiSettings())
        }

        assertTrue(result is SendMessageResult.Success)
        assertEquals(4, fakeClient.lastMessages?.size)
    }

    @Test
    fun testRunReasoningComparison_AllModesPresent() {
        fakeClient.sendResult = Result.success(
            ChatMessage(role = "assistant", content = "Response")
        )

        lateinit var result: ReasoningComparison
        testScope.launch {
            result = repository.runReasoningComparison(
                task = "Test",
                settings = ApiSettings(),
                onProgress = {}
            )
        }

        assertTrue(result.results.containsKey(ReasoningMode.DIRECT))
        assertTrue(result.results.containsKey(ReasoningMode.STEP_BY_STEP))
        assertTrue(result.results.containsKey(ReasoningMode.META_PROMPT))
        assertTrue(result.results.containsKey(ReasoningMode.EXPERT_PANEL))
    }
}

class FakeChatClient : ChatClient {
    var sendResult: Result<ChatMessage> = Result.success(ChatMessage(role = "assistant", content = "Default"))
    var lastConstraints: ResponseConstraints? = null
    var lastMessages: List<ChatMessage>? = null
    var lastSystemPrompt: String? = null
    var shouldThrow: Boolean = false

    override suspend fun sendMessage(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        constraints: ResponseConstraints,
        systemPrompt: String?,
    ): Result<ChatMessage> {
        if (shouldThrow) {
            throw Exception("Test exception")
        }
        lastConstraints = constraints
        lastMessages = messages
        lastSystemPrompt = systemPrompt
        return sendResult
    }
}
