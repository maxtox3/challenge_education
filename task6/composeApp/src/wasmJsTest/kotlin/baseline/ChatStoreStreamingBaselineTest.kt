package baseline

import chat.ChatIntent
import chat.ChatLabel
import chat.ChatRepository
import chat.ChatState
import chat.SendMessageResult
import chat.store.ChatStoreFactory
import com.arkivanov.mvikotlin.core.rx.Observer
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import model.ChatMessage
import model.StreamChunk
import settings.ApiSettings
import storage.StorageService
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatStoreStreamingBaselineTest {

    private lateinit var fakeRepository: FakeChatRepository
    private lateinit var fakeStorage: FakeStorageService

    @BeforeTest
    fun setup() {
        fakeRepository = FakeChatRepository()
        fakeStorage = FakeStorageService()
    }

    @Test
    fun chatStateDefaultValues() {
        val state = ChatState()
        assertEquals("", state.inputText, "Default inputText should be empty")
        assertEquals(emptyList(), state.messages, "Default messages should be empty list")
        assertFalse(state.isLoading, "Default isLoading should be false")
        assertNull(state.streamingMessage, "Default streamingMessage should be null")
        assertFalse(state.isStreaming, "Default isStreaming should be false")
        assertNull(state.errorMessage, "Default errorMessage should be null")
        assertFalse(state.showSettings, "Default showSettings should be false")
        assertEquals(ApiSettings(), state.settings, "Default settings should be ApiSettings()")
    }

    @Test
    fun chatStateStreamingFieldsCanBeSet() {
        val state = ChatState(
            streamingMessage = "Partial response",
            isStreaming = true,
            isLoading = true
        )
        assertEquals("Partial response", state.streamingMessage, "streamingMessage should be set")
        assertTrue(state.isStreaming, "isStreaming should be true")
        assertTrue(state.isLoading, "isLoading should be true")
    }

    @Test
    fun chatStateCopyPreservesStreamingFields() {
        val original = ChatState(
            streamingMessage = "Streaming text",
            isStreaming = true,
            isLoading = true
        )
        val copied = original.copy(errorMessage = "Error occurred")
        assertEquals("Streaming text", copied.streamingMessage, "streamingMessage should be preserved")
        assertTrue(copied.isStreaming, "isStreaming should be preserved")
        assertTrue(copied.isLoading, "isLoading should be preserved")
        assertEquals("Error occurred", copied.errorMessage, "errorMessage should be updated")
    }

    @Test
    fun streamingStartedSetsStreamingState() = runTest {
        fakeRepository.streamingChunks = listOf(
            StreamChunk.Content("Hello"),
            StreamChunk.Done
        )
        val store = ChatStoreFactory(
            DefaultStoreFactory(),
            fakeRepository,
            fakeStorage
        ).create()

        store.accept(ChatIntent.UpdateInputText("Test"))
        store.accept(ChatIntent.SendMessage)

        advanceTimeBy(100)
        runCurrent()

        val state = store.state
        assertTrue(state.isStreaming || state.messages.isNotEmpty(), "Should have streaming or completed state")
    }

    @Test
    fun streamingFinishedClearsStreamingState() = runTest {
        fakeRepository.streamingChunks = listOf(
            StreamChunk.Content("Response"),
            StreamChunk.Done
        )
        val store = ChatStoreFactory(
            DefaultStoreFactory(),
            fakeRepository,
            fakeStorage
        ).create()

        store.accept(ChatIntent.UpdateInputText("Test"))
        store.accept(ChatIntent.SendMessage)

        advanceTimeBy(200)
        runCurrent()

        val finalState = store.state
        assertFalse(finalState.isStreaming, "isStreaming should be false after streaming finished")
        assertNull(finalState.streamingMessage, "streamingMessage should be null after streaming finished")
        assertFalse(finalState.isLoading, "isLoading should be false after streaming finished")
    }

    @Test
    fun contentChunksAreAppendedToStreamingMessage() = runTest {
        fakeRepository.streamingChunks = listOf(
            StreamChunk.Content("Hello"),
            StreamChunk.Content(" "),
            StreamChunk.Content("World"),
            StreamChunk.Done
        )
        val store = ChatStoreFactory(
            DefaultStoreFactory(),
            fakeRepository,
            fakeStorage
        ).create()

        store.accept(ChatIntent.UpdateInputText("Test"))
        store.accept(ChatIntent.SendMessage)

        advanceTimeBy(200)
        runCurrent()

        val finalState = store.state
        assertEquals(2, finalState.messages.size, "Should have user + assistant messages")
        assertEquals("Hello World", finalState.messages[1].content, "Assistant message should contain appended chunks")
    }

    @Test
    fun reasoningChunksAreAppendedSameAsContent() = runTest {
        fakeRepository.streamingChunks = listOf(
            StreamChunk.Reasoning("Thinking..."),
            StreamChunk.Content("Answer"),
            StreamChunk.Done
        )
        val store = ChatStoreFactory(
            DefaultStoreFactory(),
            fakeRepository,
            fakeStorage
        ).create()

        store.accept(ChatIntent.UpdateInputText("Test"))
        store.accept(ChatIntent.SendMessage)

        advanceTimeBy(200)
        runCurrent()

        val finalState = store.state
        assertEquals(
            "Thinking...Answer",
            finalState.messages[1].content,
            "Reasoning and Content chunks should both be appended (CURRENT BEHAVIOR: they mix together)"
        )
    }

    @Test
    fun doneChunkFinalizesMessageAndAddsToMessagesList() = runTest {
        fakeRepository.streamingChunks = listOf(
            StreamChunk.Content("Final answer"),
            StreamChunk.Done
        )
        val store = ChatStoreFactory(
            DefaultStoreFactory(),
            fakeRepository,
            fakeStorage
        ).create()

        store.accept(ChatIntent.UpdateInputText("Test"))
        store.accept(ChatIntent.SendMessage)

        advanceTimeBy(200)
        runCurrent()

        val finalState = store.state
        assertEquals(2, finalState.messages.size, "Should have user + assistant messages")
        assertEquals("user", finalState.messages[0].role, "First message should be from user")
        assertEquals("Test", finalState.messages[0].content, "User message content should match input")
        assertEquals("assistant", finalState.messages[1].role, "Second message should be from assistant")
        assertEquals(
            "Final answer",
            finalState.messages[1].content,
            "Assistant message should contain streamed content"
        )
    }

    @Test
    fun emptyStreamingMessageDoesNotAddAssistantMessage() = runTest {
        fakeRepository.streamingChunks = listOf(
            StreamChunk.Content(""),
            StreamChunk.Done
        )
        val store = ChatStoreFactory(
            DefaultStoreFactory(),
            fakeRepository,
            fakeStorage
        ).create()

        store.accept(ChatIntent.UpdateInputText("Test"))
        store.accept(ChatIntent.SendMessage)

        advanceTimeBy(200)
        runCurrent()

        val finalState = store.state
        assertEquals(1, finalState.messages.size, "Should only have user message when streamingMessage is empty")
        assertEquals("user", finalState.messages[0].role, "Only message should be from user")
    }

    @Test
    fun streamingErrorClearsStreamingStateAndSetsError() = runTest {
        fakeRepository.streamingChunks = listOf(
            StreamChunk.Content("Partial"),
        )
        fakeRepository.shouldFailStreaming = true
        val store = ChatStoreFactory(
            DefaultStoreFactory(),
            fakeRepository,
            fakeStorage
        ).create()

        store.accept(ChatIntent.UpdateInputText("Test"))
        store.accept(ChatIntent.SendMessage)

        advanceTimeBy(200)
        runCurrent()

        val finalState = store.state
        assertFalse(finalState.isStreaming, "isStreaming should be false after error")
        assertNull(finalState.streamingMessage, "streamingMessage should be null after error")
        assertFalse(finalState.isLoading, "isLoading should be false after error")
        assertTrue(finalState.errorMessage != null, "errorMessage should be set")
    }

    @Test
    fun streamingErrorPublishesShowToastLabel() = runTest {
        fakeRepository.streamingChunks = listOf(
            StreamChunk.Content("Partial"),
        )
        fakeRepository.shouldFailStreaming = true
        val store = ChatStoreFactory(
            DefaultStoreFactory(),
            fakeRepository,
            fakeStorage
        ).create()

        val labels = mutableListOf<ChatLabel>()
        store.labels(object : Observer<ChatLabel> {
            override fun onNext(value: ChatLabel) {
                labels.add(value)
            }
            override fun onComplete() {}
        })

        store.accept(ChatIntent.UpdateInputText("Test"))
        store.accept(ChatIntent.SendMessage)

        advanceTimeBy(200)
        runCurrent()

        val errorLabel = labels.filterIsInstance<ChatLabel.ShowToast>().firstOrNull()
        assertTrue(errorLabel != null, "Should publish ShowToast label on error")
        assertTrue(errorLabel!!.message.isNotEmpty(), "Error message should not be empty")
    }

    @Test
    fun successfulStreamingPublishesScrollToBottomLabel() = runTest {
        fakeRepository.streamingChunks = listOf(
            StreamChunk.Content("Response"),
            StreamChunk.Done
        )
        val store = ChatStoreFactory(
            DefaultStoreFactory(),
            fakeRepository,
            fakeStorage
        ).create()

        val labels = mutableListOf<ChatLabel>()
        store.labels(object : Observer<ChatLabel> {
            override fun onNext(value: ChatLabel) {
                labels.add(value)
            }
            override fun onComplete() {}
        })

        store.accept(ChatIntent.UpdateInputText("Test"))
        store.accept(ChatIntent.SendMessage)

        advanceTimeBy(200)
        runCurrent()

        val scrollLabel = labels.filterIsInstance<ChatLabel.ScrollToBottom>().firstOrNull()
        assertTrue(scrollLabel != null, "Should publish ScrollToBottom label on successful completion")
    }

    @Test
    fun multipleContentChunksBuildUpMessageIncrementally() = runTest {
        fakeRepository.streamingChunks = listOf(
            StreamChunk.Content("A"),
            StreamChunk.Content("B"),
            StreamChunk.Content("C"),
            StreamChunk.Done
        )
        val store = ChatStoreFactory(
            DefaultStoreFactory(),
            fakeRepository,
            fakeStorage
        ).create()

        store.accept(ChatIntent.UpdateInputText("Test"))
        store.accept(ChatIntent.SendMessage)

        advanceTimeBy(200)
        runCurrent()

        val finalState = store.state
        assertEquals("ABC", finalState.messages[1].content, "All chunks should be appended in order")
    }

    @Test
    fun mixedContentAndReasoningChunksInterleave() = runTest {
        fakeRepository.streamingChunks = listOf(
            StreamChunk.Reasoning("R1"),
            StreamChunk.Content("C1"),
            StreamChunk.Reasoning("R2"),
            StreamChunk.Content("C2"),
            StreamChunk.Done
        )
        val store = ChatStoreFactory(
            DefaultStoreFactory(),
            fakeRepository,
            fakeStorage
        ).create()

        store.accept(ChatIntent.UpdateInputText("Test"))
        store.accept(ChatIntent.SendMessage)

        advanceTimeBy(200)
        runCurrent()

        val finalState = store.state
        assertEquals(
            "R1C1R2C2",
            finalState.messages[1].content,
            "Reasoning and Content should interleave in order received (CURRENT BEHAVIOR: no separation)"
        )
    }

    @Test
    fun streamingPreservesExistingMessages() = runTest {
        fakeRepository.streamingChunks = listOf(
            StreamChunk.Content("Response"),
            StreamChunk.Done
        )
        val store = ChatStoreFactory(
            DefaultStoreFactory(),
            fakeRepository,
            fakeStorage
        ).create()

        val existingMessage = ChatMessage(role = "user", content = "Previous")
        store.accept(ChatIntent.UpdateMessages(listOf(existingMessage)))

        advanceTimeBy(100)
        runCurrent()

        store.accept(ChatIntent.UpdateInputText("New"))
        store.accept(ChatIntent.SendMessage)

        advanceTimeBy(200)
        runCurrent()

        val finalState = store.state
        assertEquals(3, finalState.messages.size, "Should have previous + new user + assistant messages")
        assertEquals("Previous", finalState.messages[0].content, "First message should be preserved")
    }

    @Test
    fun sendMessageWithBlankInputDoesNothing() = runTest {
        fakeRepository.streamingChunks = listOf(
            StreamChunk.Content("Response"),
            StreamChunk.Done
        )
        val store = ChatStoreFactory(
            DefaultStoreFactory(),
            fakeRepository,
            fakeStorage
        ).create()

        store.accept(ChatIntent.UpdateInputText("   "))
        store.accept(ChatIntent.SendMessage)

        advanceTimeBy(200)
        runCurrent()

        val finalState = store.state
        assertEquals(0, finalState.messages.size, "Should not send message with blank input")
        assertEquals("   ", finalState.inputText, "Input text should remain unchanged")
    }

    @Test
    fun sendMessageClearsInputText() = runTest {
        fakeRepository.streamingChunks = listOf(
            StreamChunk.Content("Response"),
            StreamChunk.Done
        )
        val store = ChatStoreFactory(
            DefaultStoreFactory(),
            fakeRepository,
            fakeStorage
        ).create()

        store.accept(ChatIntent.UpdateInputText("Test message"))
        store.accept(ChatIntent.SendMessage)

        advanceTimeBy(100)
        runCurrent()

        val state = store.state
        assertEquals("", state.inputText, "Input text should be cleared after sending")
    }

    @Test
    fun clearChatRemovesAllMessages() = runTest {
        fakeRepository.streamingChunks = listOf(
            StreamChunk.Content("Response"),
            StreamChunk.Done
        )
        val store = ChatStoreFactory(
            DefaultStoreFactory(),
            fakeRepository,
            fakeStorage
        ).create()

        store.accept(ChatIntent.UpdateInputText("Test"))
        store.accept(ChatIntent.SendMessage)

        advanceTimeBy(200)
        runCurrent()

        assertEquals(2, store.state.messages.size, "Should have messages before clear")

        store.accept(ChatIntent.ClearChat)

        advanceTimeBy(100)
        runCurrent()

        assertEquals(0, store.state.messages.size, "Should have no messages after clear")
    }

    @Test
    fun clearChatClearsErrorMessage() = runTest {
        fakeRepository.streamingChunks = listOf(
            StreamChunk.Content("Response"),
        )
        fakeRepository.shouldFailStreaming = true
        val store = ChatStoreFactory(
            DefaultStoreFactory(),
            fakeRepository,
            fakeStorage
        ).create()

        store.accept(ChatIntent.UpdateInputText("Test"))
        store.accept(ChatIntent.SendMessage)

        advanceTimeBy(200)
        runCurrent()

        assertTrue(store.state.errorMessage != null, "Should have error message")

        store.accept(ChatIntent.ClearChat)

        advanceTimeBy(100)
        runCurrent()

        assertNull(store.state.errorMessage, "Error message should be cleared")
    }
}

private class FakeChatRepository : ChatRepository {
    var streamingChunks: List<StreamChunk> = emptyList()
    var shouldFailStreaming = false

    override suspend fun sendMessage(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings
    ): SendMessageResult = SendMessageResult.Success(
        ChatMessage(role = "assistant", content = "Non-streaming response")
    )

    override fun sendMessageStreaming(
        prompt: String,
        messages: List<ChatMessage>,
        settings: ApiSettings
    ): Flow<StreamChunk> = flow {
        if (shouldFailStreaming) {
            throw RuntimeException("Streaming failed")
        }
        streamingChunks.forEach { chunk ->
            emit(chunk)
        }
    }
}

private class FakeStorageService : StorageService {
    private var chatHistory: List<ChatMessage> = emptyList()
    private var apiSettings: String? = null

    override suspend fun getChatHistory(): List<ChatMessage> = chatHistory

    override suspend fun setChatHistory(messages: List<ChatMessage>) {
        chatHistory = messages
    }

    override suspend fun clearChatHistory() {
        chatHistory = emptyList()
    }

    override suspend fun getApiSettings(): String? = apiSettings

    override suspend fun setApiSettings(settings: String) {
        apiSettings = settings
    }
}
