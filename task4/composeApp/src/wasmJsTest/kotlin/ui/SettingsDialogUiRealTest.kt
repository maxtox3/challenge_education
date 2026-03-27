@file:OptIn(ExperimentalTestApi::class)

package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import model.ModelType
import settings.ApiSettings
import settings.ui.ModelSelectorTags
import settings.ui.SettingsDialog
import settings.ui.SettingsDialogTags
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalWasmJsInterop
class SettingsDialogUiRealTest {

    companion object {
        private const val TEST_MAX_TOKENS = 500
        private const val TEST_TEMPERATURE = 1.5
    }

    private val defaultSettings = ApiSettings(
        apiKey = "test-key",
        model = "glm-5",
        maxTokens = 1000,
        temperature = 0.7,
        stopSequences = "",
        responseFormat = "text",
    )

    @Test
    fun settingsDialogRootExists() = runComposeUiTest {
        setContent {
            SettingsDialog(
                currentSettings = defaultSettings,
                onDismiss = {},
                onSave = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.ROOT).assertExists()
    }

    @Test
    fun settingsDialogDialogSurfaceExists() = runComposeUiTest {
        setContent {
            SettingsDialog(
                currentSettings = defaultSettings,
                onDismiss = {},
                onSave = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.DIALOG_SURFACE).assertExists()
    }

    @Test
    fun settingsDialogTitleDisplayed() = runComposeUiTest {
        setContent {
            SettingsDialog(
                currentSettings = defaultSettings,
                onDismiss = {},
                onSave = {},
            )
        }

        onNodeWithText("API Settings").assertExists()
        onNodeWithTag(SettingsDialogTags.TITLE).assertExists()
    }

    @Test
    fun settingsDialogApiKeyFieldDisplaysValue() = runComposeUiTest {
        setContent {
            SettingsDialog(
                currentSettings = defaultSettings.copy(apiKey = "my-secret-key"),
                onDismiss = {},
                onSave = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.API_KEY_FIELD).assertExists()
        onNodeWithText("my-secret-key").assertExists()
    }

    @Test
    fun settingsDialogModelFieldDisplaysValue() = runComposeUiTest {
        val model = ModelType.MIDDLE
        setContent {
            SettingsDialog(
                currentSettings = defaultSettings.copy(model = model.id),
                onDismiss = {},
                onSave = {},
            )
        }

        onNodeWithTag(ModelSelectorTags.TEXT_FIELD).assertExists()
        onNodeWithText("${model.displayName} (${model.level})").assertExists()
    }

    @Test
    fun settingsDialogMaxTokensFieldDisplaysValue() = runComposeUiTest {
        setContent {
            SettingsDialog(
                currentSettings = defaultSettings.copy(maxTokens = 2000),
                onDismiss = {},
                onSave = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.MAX_TOKENS_FIELD).assertExists()
        onNodeWithText("2000").assertExists()
    }

    @Test
    fun settingsDialogMaxTokensFieldEmptyWhenNull() = runComposeUiTest {
        setContent {
            SettingsDialog(
                currentSettings = defaultSettings.copy(maxTokens = null),
                onDismiss = {},
                onSave = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.MAX_TOKENS_FIELD).assertExists()
    }

    @Test
    fun settingsDialogTemperatureSliderExists() = runComposeUiTest {
        setContent {
            SettingsDialog(
                currentSettings = defaultSettings,
                onDismiss = {},
                onSave = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.TEMPERATURE_SLIDER).assertExists()
    }

    @Test
    fun settingsDialogTemperatureLabelDisplaysValue() = runComposeUiTest {
        setContent {
            SettingsDialog(
                currentSettings = defaultSettings.copy(temperature = 0.5),
                onDismiss = {},
                onSave = {},
            )
        }

        onNodeWithText("Temperature: 0.5").assertExists()
    }

    @Test
    fun settingsDialogStopSequencesFieldDisplaysValue() = runComposeUiTest {
        setContent {
            SettingsDialog(
                currentSettings = defaultSettings.copy(stopSequences = "stop1,stop2"),
                onDismiss = {},
                onSave = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.STOP_SEQUENCES_FIELD).assertExists()
        onNodeWithText("stop1,stop2").assertExists()
    }

    @Test
    fun settingsDialogTextFormatChipExists() = runComposeUiTest {
        setContent {
            SettingsDialog(
                currentSettings = defaultSettings.copy(responseFormat = "text"),
                onDismiss = {},
                onSave = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.TEXT_FORMAT_CHIP).assertExists()
        onNodeWithText("Text").assertExists()
    }

    @Test
    fun settingsDialogJsonFormatChipExists() = runComposeUiTest {
        setContent {
            SettingsDialog(
                currentSettings = defaultSettings.copy(responseFormat = "json"),
                onDismiss = {},
                onSave = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.JSON_FORMAT_CHIP).assertExists()
        onNodeWithText("JSON").assertExists()
    }

    @Test
    fun settingsDialogCancelButtonExists() = runComposeUiTest {
        setContent {
            SettingsDialog(
                currentSettings = defaultSettings,
                onDismiss = {},
                onSave = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.CANCEL_BUTTON).assertExists()
        onNodeWithText("Cancel").assertExists()
    }

    @Test
    fun settingsDialogSaveButtonExists() = runComposeUiTest {
        setContent {
            SettingsDialog(
                currentSettings = defaultSettings,
                onDismiss = {},
                onSave = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.SAVE_BUTTON).assertExists()
        onNodeWithText("Save").assertExists()
    }

    @Test
    fun settingsDialogCancelButtonTriggersCallback() = runComposeUiTest {
        var dismissCalled = false

        setContent {
            SettingsDialog(
                currentSettings = defaultSettings,
                onDismiss = { dismissCalled = true },
                onSave = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.CANCEL_BUTTON).performClick()

        assertTrue(dismissCalled)
    }

    @Test
    fun settingsDialogSaveButtonTriggersCallback() = runComposeUiTest {
        var savedSettings: ApiSettings? = null

        setContent {
            SettingsDialog(
                currentSettings = defaultSettings,
                onDismiss = {},
                onSave = { savedSettings = it },
            )
        }

        onNodeWithTag(SettingsDialogTags.SAVE_BUTTON).performClick()

        assertTrue(savedSettings != null)
    }

    @Test
    fun settingsDialogSavePassesCorrectApiKey() = runComposeUiTest {
        var savedSettings: ApiSettings? = null

        setContent {
            SettingsDialog(
                currentSettings = defaultSettings.copy(apiKey = "saved-api-key"),
                onDismiss = {},
                onSave = { savedSettings = it },
            )
        }

        onNodeWithTag(SettingsDialogTags.SAVE_BUTTON).performClick()

        assertEquals("saved-api-key", savedSettings?.apiKey)
    }

    @Test
    fun settingsDialogSavePassesCorrectModel() = runComposeUiTest {
        var savedSettings: ApiSettings? = null

        setContent {
            SettingsDialog(
                currentSettings = defaultSettings.copy(model = "saved-model"),
                onDismiss = {},
                onSave = { savedSettings = it },
            )
        }

        onNodeWithTag(SettingsDialogTags.SAVE_BUTTON).performClick()

        assertEquals("saved-model", savedSettings?.model)
    }

    @Test
    fun settingsDialogSavePassesCorrectTemperature() = runComposeUiTest {
        var savedSettings: ApiSettings? = null

        setContent {
            SettingsDialog(
                currentSettings = defaultSettings.copy(temperature = 1.5),
                onDismiss = {},
                onSave = { savedSettings = it },
            )
        }

        onNodeWithTag(SettingsDialogTags.SAVE_BUTTON).performClick()

        assertEquals(TEST_TEMPERATURE, savedSettings?.temperature)
    }

    @Test
    fun settingsDialogSavePassesCorrectResponseFormat() = runComposeUiTest {
        var savedSettings: ApiSettings? = null

        setContent {
            SettingsDialog(
                currentSettings = defaultSettings.copy(responseFormat = "json"),
                onDismiss = {},
                onSave = { savedSettings = it },
            )
        }

        onNodeWithTag(SettingsDialogTags.SAVE_BUTTON).performClick()

        assertEquals("json", savedSettings?.responseFormat)
    }

    @Test
    fun settingsDialogSavePassesCorrectStopSequences() = runComposeUiTest {
        var savedSettings: ApiSettings? = null

        setContent {
            SettingsDialog(
                currentSettings = defaultSettings.copy(stopSequences = "stop,end"),
                onDismiss = {},
                onSave = { savedSettings = it },
            )
        }

        onNodeWithTag(SettingsDialogTags.SAVE_BUTTON).performClick()

        assertEquals("stop,end", savedSettings?.stopSequences)
    }

    @Test
    fun settingsDialogApiKeyFieldEditable() = runComposeUiTest {
        var savedSettings: ApiSettings? = null

        setContent {
            SettingsDialog(
                currentSettings = defaultSettings,
                onDismiss = {},
                onSave = { savedSettings = it },
            )
        }

        onNodeWithTag(SettingsDialogTags.API_KEY_FIELD).performTextReplacement("new-api-key")

        onNodeWithTag(SettingsDialogTags.SAVE_BUTTON).performClick()

        assertEquals("new-api-key", savedSettings?.apiKey)
    }

    @Test
    fun settingsDialogMaxTokensFieldEditable() = runComposeUiTest {
        var savedSettings: ApiSettings? = null

        setContent {
            SettingsDialog(
                currentSettings = defaultSettings.copy(maxTokens = null),
                onDismiss = {},
                onSave = { savedSettings = it },
            )
        }

        onNodeWithTag(SettingsDialogTags.MAX_TOKENS_FIELD).performTextInput("500")

        onNodeWithTag(SettingsDialogTags.SAVE_BUTTON).performClick()

        assertEquals(TEST_MAX_TOKENS, savedSettings?.maxTokens)
    }

    @Test
    fun settingsDialogStopSequencesFieldEditable() = runComposeUiTest {
        var savedSettings: ApiSettings? = null

        setContent {
            SettingsDialog(
                currentSettings = defaultSettings.copy(stopSequences = ""),
                onDismiss = {},
                onSave = { savedSettings = it },
            )
        }

        onNodeWithTag(SettingsDialogTags.STOP_SEQUENCES_FIELD).performTextInput("new-stop")

        onNodeWithTag(SettingsDialogTags.SAVE_BUTTON).performClick()

        assertEquals("new-stop", savedSettings?.stopSequences)
    }

    @Test
    fun settingsDialogEmptyMaxTokensSavesAsNull() = runComposeUiTest {
        var savedSettings: ApiSettings? = null

        setContent {
            SettingsDialog(
                currentSettings = defaultSettings.copy(maxTokens = 1000),
                onDismiss = {},
                onSave = { savedSettings = it },
            )
        }

        onNodeWithTag(SettingsDialogTags.MAX_TOKENS_FIELD).performTextReplacement("")

        onNodeWithTag(SettingsDialogTags.SAVE_BUTTON).performClick()

        assertEquals(null, savedSettings?.maxTokens)
    }

    @Test
    fun settingsDialogTextFormatChipClickable() = runComposeUiTest {
        var savedSettings: ApiSettings? = null

        setContent {
            SettingsDialog(
                currentSettings = defaultSettings.copy(responseFormat = "json"),
                onDismiss = {},
                onSave = { savedSettings = it },
            )
        }

        onNodeWithTag(SettingsDialogTags.TEXT_FORMAT_CHIP).performClick()
        onNodeWithTag(SettingsDialogTags.SAVE_BUTTON).performClick()

        assertEquals("text", savedSettings?.responseFormat)
    }

    @Test
    fun settingsDialogJsonFormatChipClickable() = runComposeUiTest {
        var savedSettings: ApiSettings? = null

        setContent {
            SettingsDialog(
                currentSettings = defaultSettings.copy(responseFormat = "text"),
                onDismiss = {},
                onSave = { savedSettings = it },
            )
        }

        onNodeWithTag(SettingsDialogTags.JSON_FORMAT_CHIP).performClick()
        onNodeWithTag(SettingsDialogTags.SAVE_BUTTON).performClick()

        assertEquals("json", savedSettings?.responseFormat)
    }

    @Test
    fun settingsDialogAllFieldsPresent() = runComposeUiTest {
        setContent {
            SettingsDialog(
                currentSettings = defaultSettings,
                onDismiss = {},
                onSave = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.API_KEY_FIELD).assertExists()
        onNodeWithTag(ModelSelectorTags.TEXT_FIELD).assertExists()
        onNodeWithTag(SettingsDialogTags.MAX_TOKENS_FIELD).assertExists()
        onNodeWithTag(SettingsDialogTags.TEMPERATURE_SLIDER).assertExists()
        onNodeWithTag(SettingsDialogTags.STOP_SEQUENCES_FIELD).assertExists()
        onNodeWithTag(SettingsDialogTags.TEXT_FORMAT_CHIP).assertExists()
        onNodeWithTag(SettingsDialogTags.JSON_FORMAT_CHIP).assertExists()
    }

    @Test
    fun settingsDialogAllButtonsPresent() = runComposeUiTest {
        setContent {
            SettingsDialog(
                currentSettings = defaultSettings,
                onDismiss = {},
                onSave = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.CANCEL_BUTTON).assertExists()
        onNodeWithTag(SettingsDialogTags.SAVE_BUTTON).assertExists()
    }

    @Test
    fun settingsDialogLabelsDisplayed() = runComposeUiTest {
        setContent {
            SettingsDialog(
                currentSettings = defaultSettings,
                onDismiss = {},
                onSave = {},
            )
        }

        onNodeWithText("API Key").assertExists()
        onNodeWithText("Model").assertExists()
        onNodeWithText("Max Tokens (empty = unlimited)").assertExists()
        onNodeWithText("Stop Sequences (comma-separated)").assertExists()
        onNodeWithText("Response Format").assertExists()
    }
}
