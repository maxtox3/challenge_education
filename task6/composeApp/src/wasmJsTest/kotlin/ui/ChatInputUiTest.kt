@file:OptIn(ExperimentalTestApi::class, ExperimentalWasmJsInterop::class)

package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import chat.ui.components.ChatInput
import chat.ui.components.ChatInputTags
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatInputUiTest {

    @Test
    fun warmup() = runComposeUiTest {
        setContent {}
    }

    @Test
    fun chatInputDisplaysPlaceholder() = runComposeUiTest {
        var inputValue = ""

        setContent {
            ChatInput(
                value = inputValue,
                onValueChange = { inputValue = it },
                onSend = {},
                isLoading = false,
            )
        }

        onNodeWithText("Type a message... (Enter to send)", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun chatInputDisplaysCurrentText() = runComposeUiTest {
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
    fun chatInputSendButtonDisabledWhenEmpty() = runComposeUiTest {
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
    fun chatInputSendButtonEnabledWhenHasText() = runComposeUiTest {
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
    fun chatInputSendButtonDisabledWhenLoading() = runComposeUiTest {
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
    fun chatInputOnValueChangeCalled() = runComposeUiTest {
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

        onNodeWithTag(ChatInputTags.TEXT_FIELD).performTextInput("Test")

        assertEquals("Test", lastChangedValue)
    }

    @Test
    fun chatInputSendButtonClickTriggersCallback() = runComposeUiTest {
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
    fun chatInputSendButtonDisabledWhenWhitespaceOnly() = runComposeUiTest {
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
    fun chatInputAppendText() = runComposeUiTest {
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

        onNodeWithTag(ChatInputTags.TEXT_FIELD).performTextReplacement("Hello World")

        assertEquals("Hello World", lastChangedValue)
    }
}
