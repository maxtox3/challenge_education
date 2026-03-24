@file:OptIn(ExperimentalTestApi::class)

package ui

import androidx.compose.ui.test.*
import ui.components.ChatInput
import ui.components.ChatInputTags
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatInputUiTest {

    @Test
    fun chatInput_displaysPlaceholder() = runComposeUiTest {
        var inputValue = ""

        setContent {
            ChatInput(
                value = inputValue,
                onValueChange = { inputValue = it },
                onSend = {},
                isLoading = false,
            )
        }

        onNodeWithText("Type a message... (Enter to send)").assertExists()
    }

    @Test
    fun chatInput_displaysCurrentText() = runComposeUiTest {
        var inputValue = "Hello, world!"

        setContent {
            ChatInput(
                value = inputValue,
                onValueChange = { inputValue = it },
                onSend = {},
                isLoading = false,
            )
        }

        onNodeWithText("Hello, world!").assertExists()
    }

    @Test
    fun chatInput_sendButton_disabledWhenEmpty() = runComposeUiTest {
        var inputValue = ""

        setContent {
            ChatInput(
                value = inputValue,
                onValueChange = { inputValue = it },
                onSend = {},
                isLoading = false,
            )
        }

        onNodeWithTag(ChatInputTags.SEND_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun chatInput_sendButton_enabledWhenHasText() = runComposeUiTest {
        var inputValue = "Hello"

        setContent {
            ChatInput(
                value = inputValue,
                onValueChange = { inputValue = it },
                onSend = {},
                isLoading = false,
            )
        }

        onNodeWithTag(ChatInputTags.SEND_BUTTON).assertIsEnabled()
    }

    @Test
    fun chatInput_sendButton_disabledWhenLoading() = runComposeUiTest {
        var inputValue = "Hello"

        setContent {
            ChatInput(
                value = inputValue,
                onValueChange = { inputValue = it },
                onSend = {},
                isLoading = true,
            )
        }

        onNodeWithText("Sending...").assertExists()
        onNodeWithTag(ChatInputTags.SEND_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun chatInput_onValueChangeCalled() = runComposeUiTest {
        var inputValue = ""
        var lastChangedValue = ""

        setContent {
            ChatInput(
                value = inputValue,
                onValueChange = {
                    lastChangedValue = it
                    inputValue = it
                },
                onSend = {},
                isLoading = false,
            )
        }

        onNodeWithTag(ChatInputTags.TEXT_FIELD)
            .performTextInput("Test")

        assertEquals("Test", lastChangedValue)
    }

    @Test
    fun chatInput_sendButton_clickTriggersCallback() = runComposeUiTest {
        var inputValue = "Hello"
        var sendCalled = false

        setContent {
            ChatInput(
                value = inputValue,
                onValueChange = { inputValue = it },
                onSend = { sendCalled = true },
                isLoading = false,
            )
        }

        onNodeWithTag(ChatInputTags.SEND_BUTTON).performClick()

        assertTrue(sendCalled)
    }

    @Test
    fun chatInput_sendButton_disabledWhenWhitespaceOnly() = runComposeUiTest {
        var inputValue = "   "

        setContent {
            ChatInput(
                value = inputValue,
                onValueChange = { inputValue = it },
                onSend = {},
                isLoading = false,
            )
        }

        onNodeWithTag(ChatInputTags.SEND_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun chatInput_rootExists() = runComposeUiTest {
        var inputValue = ""

        setContent {
            ChatInput(
                value = inputValue,
                onValueChange = { inputValue = it },
                onSend = {},
                isLoading = false,
            )
        }

        onNodeWithTag(ChatInputTags.ROOT).assertExists()
    }

    @Test
    fun chatInput_textFieldExists() = runComposeUiTest {
        var inputValue = ""

        setContent {
            ChatInput(
                value = inputValue,
                onValueChange = { inputValue = it },
                onSend = {},
                isLoading = false,
            )
        }

        onNodeWithTag(ChatInputTags.TEXT_FIELD).assertExists()
    }

    @Test
    fun chatInput_appendText() = runComposeUiTest {
        var inputValue = "Hello"
        var lastChangedValue = ""

        setContent {
            ChatInput(
                value = inputValue,
                onValueChange = {
                    lastChangedValue = it
                    inputValue = it
                },
                onSend = {},
                isLoading = false,
            )
        }

        onNodeWithTag(ChatInputTags.TEXT_FIELD)
            .performTextReplacement("Hello World")

        assertEquals("Hello World", lastChangedValue)
    }
}
