@file:OptIn(ExperimentalTestApi::class)

package ui

import ApiSettings
import androidx.compose.ui.test.*
import ui.components.SettingsDialog
import ui.components.SettingsDialogTags
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsDialogUiRealTest {

    private val defaultSettings = ApiSettings(
        apiKey = "test-key",
        model = "glm-5",
        maxTokens = 1000,
        temperature = 0.7,
        stopSequences = "",
        responseFormat = "text",
    )

    @Test
    fun settingsDialog_rootExists() = runComposeUiTest {
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
    fun settingsDialog_dialogSurfaceExists() = runComposeUiTest {
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
    fun settingsDialog_titleDisplayed() = runComposeUiTest {
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
    fun settingsDialog_apiKeyFieldDisplaysValue() = runComposeUiTest {
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
    fun settingsDialog_modelFieldDisplaysValue() = runComposeUiTest {
        setContent {
            SettingsDialog(
                currentSettings = defaultSettings.copy(model = "custom-model"),
                onDismiss = {},
                onSave = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.MODEL_FIELD).assertExists()
        onNodeWithText("custom-model").assertExists()
    }

    @Test
    fun settingsDialog_maxTokensFieldDisplaysValue() = runComposeUiTest {
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
    fun settingsDialog_maxTokensFieldEmptyWhenNull() = runComposeUiTest {
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
    fun settingsDialog_temperatureSliderExists() = runComposeUiTest {
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
    fun settingsDialog_temperatureLabelDisplaysValue() = runComposeUiTest {
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
    fun settingsDialog_stopSequencesFieldDisplaysValue() = runComposeUiTest {
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
    fun settingsDialog_textFormatChipExists() = runComposeUiTest {
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
    fun settingsDialog_jsonFormatChipExists() = runComposeUiTest {
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
    fun settingsDialog_cancelButtonExists() = runComposeUiTest {
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
    fun settingsDialog_saveButtonExists() = runComposeUiTest {
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
    fun settingsDialog_cancelButtonTriggersCallback() = runComposeUiTest {
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
    fun settingsDialog_saveButtonTriggersCallback() = runComposeUiTest {
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
    fun settingsDialog_savePassesCorrectApiKey() = runComposeUiTest {
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
    fun settingsDialog_savePassesCorrectModel() = runComposeUiTest {
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
    fun settingsDialog_savePassesCorrectTemperature() = runComposeUiTest {
        var savedSettings: ApiSettings? = null

        setContent {
            SettingsDialog(
                currentSettings = defaultSettings.copy(temperature = 1.5),
                onDismiss = {},
                onSave = { savedSettings = it },
            )
        }

        onNodeWithTag(SettingsDialogTags.SAVE_BUTTON).performClick()

        assertEquals(1.5, savedSettings?.temperature)
    }

    @Test
    fun settingsDialog_savePassesCorrectResponseFormat() = runComposeUiTest {
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
    fun settingsDialog_savePassesCorrectStopSequences() = runComposeUiTest {
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
    fun settingsDialog_apiKeyFieldEditable() = runComposeUiTest {
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
    fun settingsDialog_modelFieldEditable() = runComposeUiTest {
        var savedSettings: ApiSettings? = null

        setContent {
            SettingsDialog(
                currentSettings = defaultSettings,
                onDismiss = {},
                onSave = { savedSettings = it },
            )
        }

        onNodeWithTag(SettingsDialogTags.MODEL_FIELD).performTextReplacement("new-model")

        onNodeWithTag(SettingsDialogTags.SAVE_BUTTON).performClick()

        assertEquals("new-model", savedSettings?.model)
    }

    @Test
    fun settingsDialog_maxTokensFieldEditable() = runComposeUiTest {
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

        assertEquals(500, savedSettings?.maxTokens)
    }

    @Test
    fun settingsDialog_stopSequencesFieldEditable() = runComposeUiTest {
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
    fun settingsDialog_emptyMaxTokensSavesAsNull() = runComposeUiTest {
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
    fun settingsDialog_textFormatChipClickable() = runComposeUiTest {
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
    fun settingsDialog_jsonFormatChipClickable() = runComposeUiTest {
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
    fun settingsDialog_allFieldsPresent() = runComposeUiTest {
        setContent {
            SettingsDialog(
                currentSettings = defaultSettings,
                onDismiss = {},
                onSave = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.API_KEY_FIELD).assertExists()
        onNodeWithTag(SettingsDialogTags.MODEL_FIELD).assertExists()
        onNodeWithTag(SettingsDialogTags.MAX_TOKENS_FIELD).assertExists()
        onNodeWithTag(SettingsDialogTags.TEMPERATURE_SLIDER).assertExists()
        onNodeWithTag(SettingsDialogTags.STOP_SEQUENCES_FIELD).assertExists()
        onNodeWithTag(SettingsDialogTags.TEXT_FORMAT_CHIP).assertExists()
        onNodeWithTag(SettingsDialogTags.JSON_FORMAT_CHIP).assertExists()
    }

    @Test
    fun settingsDialog_allButtonsPresent() = runComposeUiTest {
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
    fun settingsDialog_labelsDisplayed() = runComposeUiTest {
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
