@file:OptIn(ExperimentalTestApi::class)

package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import chat.ui.components.MessageBubble
import chat.ui.components.MessageBubbleTags
import chat.ui.components.TypingIndicator
import model.ChatMessage
import kotlin.test.Test

@ExperimentalWasmJsInterop
class MessageBubbleUiTest {

    @Test
    fun messageBubbleDisplaysUserMessage() = runComposeUiTest {
        val message = ChatMessage(
            role = "user",
            content = "Hello, world!",
        )

        setContent {
            MessageBubble(message = message)
        }

        onNodeWithTag(MessageBubbleTags.ROOT).assertExists()
        onNodeWithTag(MessageBubbleTags.ROLE_TEXT).assertExists()
        onNodeWithText("You").assertExists()
        onNodeWithTag(MessageBubbleTags.MARKDOWN_CONTENT, useUnmergedTree = true).assertExists()
    }

    @Test
    fun messageBubbleDisplaysAssistantMessage() = runComposeUiTest {
        val message = ChatMessage(
            role = "assistant",
            content = "Hello! How can I help you?",
        )

        setContent {
            MessageBubble(message = message)
        }

        onNodeWithTag(MessageBubbleTags.ROOT).assertExists()
        onNodeWithTag(MessageBubbleTags.ROLE_TEXT).assertExists()
        onNodeWithText("GLM-5").assertExists()
        onNodeWithTag(MessageBubbleTags.MARKDOWN_CONTENT, useUnmergedTree = true).assertExists()
    }

    @Test
    fun messageBubbleDisplaysSystemMessage() = runComposeUiTest {
        val message = ChatMessage(
            role = "system",
            content = "System notification",
        )

        setContent {
            MessageBubble(message = message)
        }

        onNodeWithTag(MessageBubbleTags.ROOT).assertExists()
        onNodeWithText("System").assertExists()
        onNodeWithTag(MessageBubbleTags.MARKDOWN_CONTENT, useUnmergedTree = true).assertExists()
    }

    @Test
    fun messageBubbleDisplaysReasoningContent() = runComposeUiTest {
        val message = ChatMessage(
            role = "assistant",
            content = "Reasoning content here",
            isReasoningContent = true,
        )

        setContent {
            MessageBubble(message = message)
        }

        onNodeWithTag(MessageBubbleTags.REASONING_INDICATOR).assertExists()
    }

    @Test
    fun messageBubbleReasoningContentAlwaysVisible() = runComposeUiTest {
        val message = ChatMessage(
            role = "assistant",
            content = "Reasoning content here",
            isReasoningContent = true,
        )

        setContent {
            MessageBubble(message = message)
        }

        onNodeWithTag(MessageBubbleTags.REASONING_INDICATOR).assertExists()
        onNodeWithTag(MessageBubbleTags.MARKDOWN_CONTENT).assertExists()
        onNodeWithText("[!]").assertExists()
    }

    @Test
    fun messageBubbleDisplaysTokensInfo() = runComposeUiTest {
        val message = ChatMessage(
            role = "assistant",
            content = "Response",
            tokensUsed = 42,
            finishReason = "stop",
        )

        setContent {
            MessageBubble(message = message)
        }

        onNodeWithTag(MessageBubbleTags.TOKENS_INFO).assertExists()
        onNodeWithText("42 tokens", useUnmergedTree = true).assertExists()
        onNodeWithText(" • complete", useUnmergedTree = true).assertExists()
    }

    @Test
    fun messageBubbleDisplaysTruncatedStatus() = runComposeUiTest {
        val message = ChatMessage(
            role = "assistant",
            content = "Response",
            tokensUsed = 100,
            finishReason = "length",
        )

        setContent {
            MessageBubble(message = message)
        }

        onNodeWithText(" • truncated", useUnmergedTree = true).assertExists()
    }

    @Test
    fun messageBubbleNoTokensInfoWhenNull() = runComposeUiTest {
        val message = ChatMessage(
            role = "assistant",
            content = "Response",
            tokensUsed = null,
        )

        setContent {
            MessageBubble(message = message)
        }

        onNodeWithTag(MessageBubbleTags.TOKENS_INFO).assertDoesNotExist()
    }

    @Test
    fun messageBubbleNonReasoningShowsContentDirectly() = runComposeUiTest {
        val message = ChatMessage(
            role = "assistant",
            content = "Regular content",
            isReasoningContent = false,
        )

        setContent {
            MessageBubble(message = message)
        }

        onNodeWithTag(MessageBubbleTags.MARKDOWN_CONTENT).assertExists()
        onNodeWithTag(MessageBubbleTags.MARKDOWN_CONTENT, useUnmergedTree = true).assertExists()
        onNodeWithTag(MessageBubbleTags.REASONING_INDICATOR).assertDoesNotExist()
    }

    @Test
    fun typingIndicatorExists() = runComposeUiTest {
        setContent {
            TypingIndicator()
        }

        onNodeWithTag(MessageBubbleTags.TYPING_INDICATOR).assertExists()
    }
}
