@file:OptIn(ExperimentalTestApi::class)

package ui

import androidx.compose.ui.test.*
import model.ChatMessage
import ui.components.MessageBubble
import ui.components.MessageBubbleTags
import ui.components.TypingIndicator
import kotlin.test.Test

class MessageBubbleUiTest {

    @Test
    fun messageBubble_displaysUserMessage() = runComposeUiTest {
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
    fun messageBubble_displaysAssistantMessage() = runComposeUiTest {
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
    fun messageBubble_displaysSystemMessage() = runComposeUiTest {
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
    fun messageBubble_displaysReasoningContent() = runComposeUiTest {
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
    fun messageBubble_reasoningCollapsedByDefault() = runComposeUiTest {
        val message = ChatMessage(
            role = "assistant",
            content = "Hidden reasoning content",
            isReasoningContent = true,
        )

        setContent {
            MessageBubble(message = message)
        }

        onNodeWithTag(MessageBubbleTags.MARKDOWN_CONTENT).assertDoesNotExist()
    }

    @Test
    fun messageBubble_reasoningExpandsOnClick() = runComposeUiTest {
        val message = ChatMessage(
            role = "assistant",
            content = "Expanded reasoning content",
            isReasoningContent = true,
        )

        setContent {
            MessageBubble(message = message)
        }

        onNodeWithTag(MessageBubbleTags.MARKDOWN_CONTENT).assertDoesNotExist()

        onNodeWithTag(MessageBubbleTags.REASONING_INDICATOR).performClick()

        onNodeWithTag(MessageBubbleTags.MARKDOWN_CONTENT).assertExists()
        onNodeWithTag(MessageBubbleTags.MARKDOWN_CONTENT, useUnmergedTree = true).assertExists()
    }

    @Test
    fun messageBubble_reasoningCollapsesOnSecondClick() = runComposeUiTest {
        val message = ChatMessage(
            role = "assistant",
            content = "Toggle reasoning content",
            isReasoningContent = true,
        )

        setContent {
            MessageBubble(message = message)
        }

        onNodeWithTag(MessageBubbleTags.REASONING_INDICATOR).performClick()
        onNodeWithTag(MessageBubbleTags.MARKDOWN_CONTENT).assertExists()

        onNodeWithTag(MessageBubbleTags.REASONING_INDICATOR).performClick()
        onNodeWithTag(MessageBubbleTags.MARKDOWN_CONTENT).assertDoesNotExist()
    }

    @Test
    fun messageBubble_reasoningIndicatorIsClickable() = runComposeUiTest {
        val message = ChatMessage(
            role = "assistant",
            content = "Test content",
            isReasoningContent = true,
        )

        setContent {
            MessageBubble(message = message)
        }

        onNodeWithTag(MessageBubbleTags.REASONING_INDICATOR)
            .assertHasClickAction()
    }

    @Test
    fun messageBubble_displaysTokensInfo() = runComposeUiTest {
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
    fun messageBubble_displaysTruncatedStatus() = runComposeUiTest {
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
    fun messageBubble_noTokensInfoWhenNull() = runComposeUiTest {
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
    fun messageBubble_nonReasoningShowsContentDirectly() = runComposeUiTest {
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
    fun typingIndicator_exists() = runComposeUiTest {
        setContent {
            TypingIndicator()
        }

        onNodeWithTag(MessageBubbleTags.TYPING_INDICATOR).assertExists()
    }
}
