package baseline

import agent.AgentFactory
import chat.ChatRepository
import chat.ChatUseCases
import chat.SendMessageResult
import core.exception.ChatStreamingException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import model.ChatMessage
import model.MetricRecord
import model.ReasoningComparison
import model.StreamChunk
import network.ChatClient
import network.ResponseConstraints
import settings.ApiSettings
import kotlin.coroutines.CoroutineContext
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class ImmediateDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        block.run()
    }
}

private class FakeChatClientForUseCases : ChatClient {
    var streamingChunks: List<StreamChunk> = listOf(
        StreamChunk.Content("Hello"),
        StreamChunk.Done
    )
    var lastPrompt: String? = null
    var lastMessages: List<ChatMessage>? = null
    var lastSettings: ApiSettings? = null
    private var exceptionToThrow: Throwable? = null

    fun setExceptionToThrow(exception: Throwable?) {
        exceptionToThrow = exception
    }

    override suspend fun sendMessage(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        constraints: ResponseConstraints,
        systemPrompt: String?
    ): Result<ChatMessage> {
        exceptionToThrow?.let {
            return Result.failure(it)
        }
        lastPrompt = messages.lastOrNull()?.content
        lastMessages = messages
        return Result.success(
            ChatMessage(
                role = "assistant",
                content = "Test response",
                model = model
            )
        )
    }

    override fun sendMessageStreaming(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        constraints: ResponseConstraints,
        systemPrompt: String?
    ): Flow<StreamChunk> {
        lastPrompt = messages.lastOrNull()?.content
        lastMessages = messages

        exceptionToThrow?.let { throw it }

        return flow {
            streamingChunks.forEach { chunk ->
                emit(chunk)
            }
        }
    }
}

private class FakeChatRepositoryForUseCases(private val client: ChatClient,) : ChatRepository {
    override suspend fun sendMessage(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings
    ): SendMessageResult {
        val constraints = settings.toResponseConstraints()
        val result = client.sendMessage(
            apiKey = settings.apiKey,
            model = settings.model,
            messages = messages,
            constraints = constraints
        )

        return result.fold(
            onSuccess = { response ->
                SendMessageResult.Success(
                    response = response,
                    metric = MetricRecord(
                        id = 0,
                        prompt = prompt,
                        response = response.content,
                        mode = "test",
                        responseLength = response.content.length,
                        tokensUsed = 0,
                        maxTokens = null,
                        finishReason = "stop",
                        responseTimeMs = 0,
                        constraints = model.ConstraintsInfo(
                            maxTokens = null,
                            stopSequences = emptyList(),
                            responseFormat = "text",
                            temperature = 1.0
                        )
                    )
                )
            },
            onFailure = { error ->
                SendMessageResult.Error(error.message ?: "Unknown error")
            }
        )
    }

    override fun sendMessageStreaming(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings
    ): Flow<StreamChunk> {
        val constraints = settings.toResponseConstraints()
        return client.sendMessageStreaming(
            apiKey = settings.apiKey,
            model = settings.model,
            messages = messages,
            constraints = constraints
        )
    }

    override suspend fun runReasoningComparison(
        task: String,
        settings: ApiSettings,
        onProgress: (ReasoningComparison) -> Unit
    ): ReasoningComparison = ReasoningComparison(task, emptyMap())
}

class ChatUseCasesBaselineTest {
    private lateinit var fakeClient: FakeChatClientForUseCases
    private lateinit var fakeRepository: FakeChatRepositoryForUseCases
    private lateinit var useCases: ChatUseCases
    private lateinit var testScope: CoroutineScope

    @BeforeTest
    fun setup() {
        fakeClient = FakeChatClientForUseCases()
        testScope = CoroutineScope(SupervisorJob() + ImmediateDispatcher())
        fakeRepository = FakeChatRepositoryForUseCases(fakeClient)
        val fakeAgent = AgentFactory.create(fakeClient, "")
        useCases = ChatUseCases(fakeRepository, fakeAgent, testScope)
    }

    @Test
    fun sendMessage_validates_input_through_MessageHandler() {
        val config = ChatUseCases.SendMessageConfig(
            inputText = "  valid input  ",
            isLoading = false,
            currentMessages = emptyList(),
            settings = ApiSettings()
        )
        var messagesUpdated = false
        val callbacks = ChatUseCases.MessageCallbacks(
            onMessagesUpdate = { messagesUpdated = true },
            onInputTextUpdate = {},
            onLoadingUpdate = {},
            onErrorUpdate = {}
        )

        useCases.sendMessage(config, callbacks)

        assertTrue(messagesUpdated, "Valid input should trigger messages update")
    }

    @Test
    fun sendMessage_rejects_blank_input() {
        val config = ChatUseCases.SendMessageConfig(
            inputText = "   ",
            isLoading = false,
            currentMessages = emptyList(),
            settings = ApiSettings()
        )
        var messagesUpdated = false
        val callbacks = ChatUseCases.MessageCallbacks(
            onMessagesUpdate = { messagesUpdated = true },
            onInputTextUpdate = {},
            onLoadingUpdate = {},
            onErrorUpdate = {}
        )

        useCases.sendMessage(config, callbacks)

        assertFalse(messagesUpdated, "Blank input should NOT trigger messages update")
    }

    @Test
    fun sendMessage_adds_user_message_to_list() {
        val config = ChatUseCases.SendMessageConfig(
            inputText = "test message",
            isLoading = false,
            currentMessages = emptyList(),
            settings = ApiSettings()
        )
        var updatedMessages: List<ChatMessage> = emptyList()
        val callbacks = ChatUseCases.MessageCallbacks(
            onMessagesUpdate = { updatedMessages = it },
            onInputTextUpdate = {},
            onLoadingUpdate = {},
            onErrorUpdate = {}
        )

        useCases.sendMessage(config, callbacks)

        assertTrue(updatedMessages.isNotEmpty(), "Should add user message")
        assertEquals("user", updatedMessages.first().role, "First message should be from user")
        assertEquals("test message", updatedMessages.first().content, "Content should match input")
    }

    @Test
    fun sendMessage_clears_input_text() {
        val config = ChatUseCases.SendMessageConfig(
            inputText = "test",
            isLoading = false,
            currentMessages = emptyList(),
            settings = ApiSettings()
        )
        var inputText = "original"
        val callbacks = ChatUseCases.MessageCallbacks(
            onMessagesUpdate = {},
            onInputTextUpdate = { inputText = it },
            onLoadingUpdate = {},
            onErrorUpdate = {}
        )

        useCases.sendMessage(config, callbacks)

        assertEquals("", inputText, "Input text should be cleared after send")
    }

    @Test
    fun sendMessage_processes_streaming_chunks() {
        fakeClient.streamingChunks = listOf(
            StreamChunk.Content("Hello "),
            StreamChunk.Content("world"),
            StreamChunk.Done
        )
        val config = ChatUseCases.SendMessageConfig(
            inputText = "test",
            isLoading = false,
            currentMessages = emptyList(),
            settings = ApiSettings()
        )
        var updatedMessages: List<ChatMessage> = emptyList()
        val callbacks = ChatUseCases.MessageCallbacks(
            onMessagesUpdate = { updatedMessages = it },
            onInputTextUpdate = {},
            onLoadingUpdate = {},
            onErrorUpdate = {}
        )

        useCases.sendMessage(config, callbacks)

        val assistantMessage = updatedMessages.last()
        assertEquals("assistant", assistantMessage.role, "Last message should be from assistant")
        assertEquals("Hello world", assistantMessage.content, "Content should accumulate from chunks")
    }

    @Test
    fun sendMessage_sets_loading_state_during_operation() {
        val config = ChatUseCases.SendMessageConfig(
            inputText = "test",
            isLoading = false,
            currentMessages = emptyList(),
            settings = ApiSettings()
        )
        val loadingHistory = mutableListOf<Boolean>()
        val callbacks = ChatUseCases.MessageCallbacks(
            onMessagesUpdate = {},
            onInputTextUpdate = {},
            onLoadingUpdate = { loading -> loadingHistory.add(loading) },
            onErrorUpdate = {}
        )

        useCases.sendMessage(config, callbacks)

        assertTrue(loadingHistory.contains(true), "Loading should be set to true at some point")
        assertTrue(loadingHistory.contains(false), "Loading should be set to false after completion")
    }

    @Test
    fun sendMessage_sets_loading_false_after_Done() {
        fakeClient.streamingChunks = listOf(
            StreamChunk.Content("Response"),
            StreamChunk.Done
        )
        val config = ChatUseCases.SendMessageConfig(
            inputText = "test",
            isLoading = false,
            currentMessages = emptyList(),
            settings = ApiSettings()
        )
        var loading = true
        val callbacks = ChatUseCases.MessageCallbacks(
            onMessagesUpdate = {},
            onInputTextUpdate = {},
            onLoadingUpdate = { loading = it },
            onErrorUpdate = {}
        )

        useCases.sendMessage(config, callbacks)

        assertFalse(loading, "Loading should be false after Done")
    }

    @Test
    fun sendMessage_handles_exception() {
        fakeClient.setExceptionToThrow(ChatStreamingException("Stream error"))
        val config = ChatUseCases.SendMessageConfig(
            inputText = "test",
            isLoading = false,
            currentMessages = emptyList(),
            settings = ApiSettings()
        )
        var error: String? = null
        var loading = true
        val callbacks = ChatUseCases.MessageCallbacks(
            onMessagesUpdate = {},
            onInputTextUpdate = {},
            onLoadingUpdate = { loading = it },
            onErrorUpdate = { error = it }
        )

        useCases.sendMessage(config, callbacks)

        assertEquals("Stream error", error, "Should capture error message")
        assertFalse(loading, "Loading should be false after error")
    }

    @Test
    fun runReasoningComparison_sets_loading_true() {
        var loading = false

        useCases.runReasoningComparison(
            task = "test task",
            settings = ApiSettings(),
            onComparisonUpdate = {},
            onLoadingUpdate = { loading = it }
        )

        assertTrue(loading, "Loading should be set to true immediately")
    }
}
