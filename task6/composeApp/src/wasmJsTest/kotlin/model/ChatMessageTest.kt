package model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ChatMessageTest {
    companion object {
        private const val TEST_TIMESTAMP = 12345L
        private const val TEST_TOKENS_USED = 100
        private const val TEST_MAX_TOKENS = 1000
    }

    @Test
    fun testChatMessageDefaults() {
        val msg = ChatMessage(role = "user", content = "Hello")
        assertEquals("user", msg.role)
        assertEquals("Hello", msg.content)
        assertEquals(0L, msg.timestamp)
        assertEquals("free", msg.mode)
        assertNull(msg.tokensUsed)
        assertNull(msg.maxTokens)
        assertNull(msg.finishReason)
        assertFalse(msg.isReasoningContent)
    }

    @Test
    fun testChatMessageWithAllFields() {
        val msg = ChatMessage(
            role = "assistant",
            content = "Response",
            timestamp = TEST_TIMESTAMP,
            mode = "reasoning",
            tokensUsed = TEST_TOKENS_USED,
            maxTokens = TEST_MAX_TOKENS,
            finishReason = "stop",
            isReasoningContent = true,
        )
        assertEquals("assistant", msg.role)
        assertEquals("Response", msg.content)
        assertEquals(TEST_TIMESTAMP, msg.timestamp)
        assertEquals("reasoning", msg.mode)
        assertEquals(TEST_TOKENS_USED, msg.tokensUsed)
        assertEquals(TEST_MAX_TOKENS, msg.maxTokens)
        assertEquals("stop", msg.finishReason)
    }
}
