package model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ChatMessageTest {
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
            timestamp = 12345L,
            mode = "reasoning",
            tokensUsed = 100,
            maxTokens = 1000,
            finishReason = "stop",
            isReasoningContent = true,
        )
        assertEquals("assistant", msg.role)
        assertEquals("Response", msg.content)
        assertEquals(12345L, msg.timestamp)
        assertEquals("reasoning", msg.mode)
        assertEquals(100, msg.tokensUsed)
        assertEquals(1000, msg.maxTokens)
        assertEquals("stop", msg.finishReason)
    }
}
