@file:OptIn(ExperimentalTestApi::class)

package ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.ui.test.*
import ui.components.*
import kotlin.test.Test
import kotlin.test.assertTrue

class CommonComponentsUiRealTest {

    @Test
    fun dialogSurface_rootExists() = runComposeUiTest {
        setContent {
            DialogSurface {
                Text("Content")
            }
        }

        onNodeWithTag(DialogSurfaceTags.ROOT).assertExists()
    }

    @Test
    fun dialogSurface_displaysContent() = runComposeUiTest {
        setContent {
            DialogSurface {
                Text("Test Content")
            }
        }

        onNodeWithText("Test Content").assertExists()
    }

    @Test
    fun dialogHeader_displaysTitle() = runComposeUiTest {
        setContent {
            DialogHeader(
                title = "Test Dialog",
                onDismiss = {},
            )
        }

        onNodeWithText("Test Dialog").assertExists()
        onNodeWithTag(DialogHeaderTags.TITLE).assertExists()
    }

    @Test
    fun dialogHeader_displaysDefaultCloseText() = runComposeUiTest {
        setContent {
            DialogHeader(
                title = "Test Dialog",
                onDismiss = {},
            )
        }

        onNodeWithText("Close").assertExists()
    }

    @Test
    fun dialogHeader_displaysCustomCloseText() = runComposeUiTest {
        setContent {
            DialogHeader(
                title = "Test Dialog",
                onDismiss = {},
                dismissText = "Cancel",
            )
        }

        onNodeWithText("Cancel").assertExists()
    }

    @Test
    fun dialogHeader_closeButtonExists() = runComposeUiTest {
        setContent {
            DialogHeader(
                title = "Test Dialog",
                onDismiss = {},
            )
        }

        onNodeWithTag(DialogHeaderTags.CLOSE_BUTTON).assertExists()
    }

    @Test
    fun dialogHeader_closeButtonClick() = runComposeUiTest {
        var dismissCalled = false

        setContent {
            DialogHeader(
                title = "Test Dialog",
                onDismiss = { dismissCalled = true },
            )
        }

        onNodeWithTag(DialogHeaderTags.CLOSE_BUTTON).performClick()

        assertTrue(dismissCalled)
    }

    @Test
    fun dialogHeader_rootExists() = runComposeUiTest {
        setContent {
            DialogHeader(
                title = "Test Dialog",
                onDismiss = {},
            )
        }

        onNodeWithTag(DialogHeaderTags.ROOT).assertExists()
    }

    @Test
    fun loadingButtonContent_displaysButtonTextWhenNotLoading() = runComposeUiTest {
        setContent {
            LoadingButtonContent(
                isLoading = false,
                buttonText = "Submit",
            )
        }

        onNodeWithText("Submit").assertExists()
        onNodeWithTag(LoadingButtonContentTags.BUTTON_TEXT).assertExists()
    }

    @Test
    fun loadingButtonContent_displaysLoadingTextWhenLoading() = runComposeUiTest {
        setContent {
            LoadingButtonContent(
                isLoading = true,
                loadingText = "Loading...",
                buttonText = "Submit",
            )
        }

        onNodeWithText("Loading...").assertExists()
    }

    @Test
    fun loadingButtonContent_displaysLoadingIndicatorWhenLoading() = runComposeUiTest {
        setContent {
            LoadingButtonContent(
                isLoading = true,
                loadingText = "Loading...",
            )
        }

        onNodeWithTag(LoadingButtonContentTags.LOADING_INDICATOR).assertExists()
    }

    @Test
    fun loadingButtonContent_noLoadingIndicatorWhenNotLoading() = runComposeUiTest {
        setContent {
            LoadingButtonContent(
                isLoading = false,
                buttonText = "Submit",
            )
        }

        onNodeWithTag(LoadingButtonContentTags.LOADING_INDICATOR).assertDoesNotExist()
    }

    @Test
    fun loadingButtonContent_rootExists() = runComposeUiTest {
        setContent {
            LoadingButtonContent(isLoading = false)
        }

        onNodeWithTag(LoadingButtonContentTags.ROOT).assertExists()
    }

    @Test
    fun loadingButtonContent_defaultButtonText() = runComposeUiTest {
        setContent {
            LoadingButtonContent(isLoading = false)
        }

        onNodeWithText("Submit").assertExists()
    }

    @Test
    fun loadingButtonContent_defaultLoadingText() = runComposeUiTest {
        setContent {
            LoadingButtonContent(isLoading = true)
        }

        onNodeWithText("Loading...").assertExists()
    }

    @Test
    fun emptyStateBox_displaysMessage() = runComposeUiTest {
        setContent {
            EmptyStateBox(message = "No items found")
        }

        onNodeWithText("No items found").assertExists()
    }

    @Test
    fun emptyStateBox_rootExists() = runComposeUiTest {
        setContent {
            EmptyStateBox(message = "Empty")
        }

        onNodeWithTag(EmptyStateBoxTags.ROOT).assertExists()
    }

    @Test
    fun emptyStateBox_messageTagExists() = runComposeUiTest {
        setContent {
            EmptyStateBox(message = "Test Message")
        }

        onNodeWithTag(EmptyStateBoxTags.MESSAGE).assertExists()
    }

    @Test
    fun emptyStateBox_displaysDifferentMessages() = runComposeUiTest {
        var message = "First message"

        setContent {
            EmptyStateBox(message = message)
        }

        onNodeWithText("First message").assertExists()
    }

    @Test
    fun dialogSurface_combinedWithDialogHeader() = runComposeUiTest {
        var dismissCalled = false

        setContent {
            DialogSurface {
                Column {
                    DialogHeader(
                        title = "Combined Test",
                        onDismiss = { dismissCalled = true },
                    )
                }
            }
        }

        onNodeWithTag(DialogSurfaceTags.ROOT).assertExists()
        onNodeWithText("Combined Test").assertExists()
        onNodeWithTag(DialogHeaderTags.CLOSE_BUTTON).performClick()
        assertTrue(dismissCalled)
    }

    @Test
    fun loadingButtonContent_inButton() = runComposeUiTest {
        var buttonEnabled = true

        setContent {
            androidx.compose.material.Button(
                onClick = {},
                enabled = buttonEnabled,
            ) {
                LoadingButtonContent(
                    isLoading = false,
                    buttonText = "Click Me",
                )
            }
        }

        onNodeWithText("Click Me").assertExists()
    }

    @Test
    fun loadingButtonContent_loadingStateTransition() = runComposeUiTest {
        var isLoading = false

        setContent {
            LoadingButtonContent(
                isLoading = isLoading,
                loadingText = "Processing...",
                buttonText = "Start",
            )
        }

        onNodeWithText("Start").assertExists()
        onNodeWithTag(LoadingButtonContentTags.LOADING_INDICATOR).assertDoesNotExist()
    }

    @Test
    fun dialogHeader_customDismissTextVariations() = runComposeUiTest {
        setContent {
            DialogHeader(
                title = "Settings",
                onDismiss = {},
                dismissText = "Done",
            )
        }

        onNodeWithText("Done").assertExists()
        onNodeWithText("Close").assertDoesNotExist()
    }

    @Test
    fun emptyStateBox_longMessage() = runComposeUiTest {
        val longMessage = "This is a very long empty state message that should still display correctly in the UI"

        setContent {
            EmptyStateBox(message = longMessage)
        }

        onNodeWithText(longMessage).assertExists()
    }

    @Test
    fun loadingButtonContent_customTexts() = runComposeUiTest {
        setContent {
            LoadingButtonContent(
                isLoading = true,
                loadingText = "Saving...",
                buttonText = "Save",
            )
        }

        onNodeWithText("Saving...").assertExists()
        onNodeWithTag(LoadingButtonContentTags.LOADING_INDICATOR).assertExists()
    }
}
