@file:OptIn(ExperimentalTestApi::class)

package ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.test.TestResult
import ui.components.DialogHeader
import ui.components.DialogHeaderTags
import ui.components.DialogSurface
import ui.components.DialogSurfaceTags
import ui.components.EmptyStateBox
import ui.components.EmptyStateBoxTags
import ui.components.LoadingButtonContent
import ui.components.LoadingButtonContentTags
import kotlin.test.Test
import kotlin.test.assertTrue

@ExperimentalWasmJsInterop
class CommonComponentsUiRealTest {

    @Test
    fun dialogSurfaceRootExists() = runComposeUiTest {
        setContent {
            DialogSurface {
                Text("Content")
            }
        }

        onNodeWithTag(DialogSurfaceTags.ROOT).assertExists()
    }

    @Test
    fun dialogSurfaceDisplaysContent() = runComposeUiTest {
        setContent {
            DialogSurface {
                Text("Test Content")
            }
        }

        onNodeWithText("Test Content").assertExists()
    }

    @Test
    fun dialogHeaderDisplaysTitle() = runComposeUiTest {
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
    fun dialogHeaderDisplaysDefaultCloseText() = runComposeUiTest {
        setContent {
            DialogHeader(
                title = "Test Dialog",
                onDismiss = {},
            )
        }

        onNodeWithText("Close").assertExists()
    }

    @Test
    fun dialogHeaderDisplaysCustomCloseText() = runComposeUiTest {
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
    fun dialogHeaderCloseButtonExists() = runComposeUiTest {
        setContent {
            DialogHeader(
                title = "Test Dialog",
                onDismiss = {},
            )
        }

        onNodeWithTag(DialogHeaderTags.CLOSE_BUTTON).assertExists()
    }

    @Test
    fun dialogHeaderCloseButtonClick() = runComposeUiTest {
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
    fun dialogHeaderRootExists() = runComposeUiTest {
        setContent {
            DialogHeader(
                title = "Test Dialog",
                onDismiss = {},
            )
        }

        onNodeWithTag(DialogHeaderTags.ROOT).assertExists()
    }

    @Test
    fun loadingButtonContentDisplaysButtonTextWhenNotLoading() = runComposeUiTest {
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
    fun loadingButtonContentDisplaysLoadingTextWhenLoading() = runComposeUiTest {
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
    fun loadingButtonContentDisplaysLoadingIndicatorWhenLoading() = runComposeUiTest {
        setContent {
            LoadingButtonContent(
                isLoading = true,
                loadingText = "Loading...",
            )
        }

        onNodeWithTag(LoadingButtonContentTags.LOADING_INDICATOR).assertExists()
    }

    @Test
    fun loadingButtonContentNoLoadingIndicatorWhenNotLoading() = runComposeUiTest {
        setContent {
            LoadingButtonContent(
                isLoading = false,
                buttonText = "Submit",
            )
        }

        onNodeWithTag(LoadingButtonContentTags.LOADING_INDICATOR).assertDoesNotExist()
    }

    @Test
    fun loadingButtonContentRootExists() = runComposeUiTest {
        setContent {
            LoadingButtonContent(isLoading = false)
        }

        onNodeWithTag(LoadingButtonContentTags.ROOT).assertExists()
    }

    @Test
    fun loadingButtonContentDefaultButtonText() = runComposeUiTest {
        setContent {
            LoadingButtonContent(isLoading = false)
        }

        onNodeWithText("Submit").assertExists()
    }

    @Test
    fun loadingButtonContentDefaultLoadingText() = runComposeUiTest {
        setContent {
            LoadingButtonContent(isLoading = true)
        }

        onNodeWithText("Loading...").assertExists()
    }

    @Test
    fun emptyStateBoxDisplaysMessage() = runComposeUiTest {
        setContent {
            EmptyStateBox(message = "No items found")
        }

        onNodeWithText("No items found").assertExists()
    }

    @Test
    fun emptyStateBoxRootExists() = runComposeUiTest {
        setContent {
            EmptyStateBox(message = "Empty")
        }

        onNodeWithTag(EmptyStateBoxTags.ROOT).assertExists()
    }

    @Test
    fun emptyStateBoxMessageTagExists() = runComposeUiTest {
        setContent {
            EmptyStateBox(message = "Test Message")
        }

        onNodeWithTag(EmptyStateBoxTags.MESSAGE).assertExists()
    }

    @Test
    fun emptyStateBoxDisplaysDifferentMessages() = runComposeUiTest {
        val message = "First message"

        setContent {
            EmptyStateBox(message = message)
        }

        onNodeWithText(message).assertExists()
    }

    @Test
    fun dialogSurfaceCombinedWithDialogHeader() = runComposeUiTest {
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
    fun loadingButtonContentInButton() = runComposeUiTest {
        val buttonText = "Click Me"

        setContent {
            Button(
                onClick = {},
                enabled = true,
            ) {
                LoadingButtonContent(
                    isLoading = false,
                    buttonText = buttonText,
                )
            }
        }

        onNodeWithText(buttonText).assertExists()
    }

    @Test
    fun loadingButtonContentLoadingStateTransition(): TestResult {
        val buttonText = "Start"

        return runComposeUiTest {
            setContent {
                LoadingButtonContent(
                    isLoading = false,
                    loadingText = "Processing...",
                    buttonText = buttonText,
                )
            }

            onNodeWithText(buttonText).assertExists()
            onNodeWithTag(LoadingButtonContentTags.LOADING_INDICATOR).assertDoesNotExist()
        }
    }

    @Test
    fun dialogHeaderCustomDismissTextVariations(): TestResult {
        val dismissText = "Done"

        return runComposeUiTest {
            setContent {
                DialogHeader(
                    title = "Settings",
                    onDismiss = {},
                    dismissText = dismissText,
                )
            }

            onNodeWithText(dismissText).assertExists()
            onNodeWithText("Close").assertDoesNotExist()
        }
    }

    @Test
    fun emptyStateBoxLongMessage() = runComposeUiTest {
        val longMessage = "This is a very long empty state message that should still display correctly in the UI"

        setContent {
            EmptyStateBox(message = longMessage)
        }

        onNodeWithText(longMessage).assertExists()
    }

    @Test
    fun loadingButtonContentCustomTexts(): TestResult {
        val loadingText = "Saving..."

        return runComposeUiTest {
            setContent {
                LoadingButtonContent(
                    isLoading = true,
                    loadingText = loadingText,
                    buttonText = "Save",
                )
            }

            onNodeWithText(loadingText).assertExists()
            onNodeWithTag(LoadingButtonContentTags.LOADING_INDICATOR).assertExists()
        }
    }
}
