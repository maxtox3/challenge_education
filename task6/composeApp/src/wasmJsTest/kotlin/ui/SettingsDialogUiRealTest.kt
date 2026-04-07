@file:OptIn(ExperimentalTestApi::class)

package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import model.ApiProvider
import model.ModelType
import settings.ApiSettings
import settings.SettingsComponent
import settings.SettingsIntent
import settings.SettingsState
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
        provider = ApiProvider.ZAI,
        model = "glm-5",
        maxTokens = 1000,
        temperature = 0.7,
        stopSequences = "",
        responseFormat = "text",
    )

    private fun createTestComponent(
        initialSettings: ApiSettings = defaultSettings,
        onSettingsSaved: (ApiSettings) -> Unit = {}
    ): SettingsComponent = object : SettingsComponent {
        private val _state = MutableValue(SettingsState(settings = initialSettings))

        override val state: Value<SettingsState> = _state

        override fun accept(intent: SettingsIntent) {
            when (intent) {
                is SettingsIntent.UpdateApiKey -> {
                    _state.update { it.copy(settings = it.settings.copy(apiKey = intent.apiKey)) }
                }

                is SettingsIntent.UpdateProvider -> {
                    _state.update { it.copy(settings = it.settings.copy(provider = intent.provider)) }
                }

                is SettingsIntent.UpdateModel -> {
                    _state.update { it.copy(settings = it.settings.copy(model = intent.model)) }
                }

                is SettingsIntent.UpdateMaxTokens -> {
                    _state.update { it.copy(settings = it.settings.copy(maxTokens = intent.maxTokens)) }
                }

                is SettingsIntent.UpdateTemperature -> {
                    _state.update { it.copy(settings = it.settings.copy(temperature = intent.temperature)) }
                }

                is SettingsIntent.UpdateStopSequences -> {
                    _state.update { it.copy(settings = it.settings.copy(stopSequences = intent.stopSequences)) }
                }

                is SettingsIntent.UpdateResponseFormat -> {
                    _state.update { it.copy(settings = it.settings.copy(responseFormat = intent.responseFormat)) }
                }

                is SettingsIntent.SaveSettings -> {
                    onSettingsSaved(_state.value.settings)
                }

                else -> Unit
            }
        }
    }

    @Test
    fun settingsDialogRootExists() = runComposeUiTest {
        setContent {
            SettingsDialog(
                component = createTestComponent(),
                onDismiss = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.ROOT).assertExists()
    }

    @Test
    fun settingsDialogDialogSurfaceExists() = runComposeUiTest {
        setContent {
            SettingsDialog(
                component = createTestComponent(),
                onDismiss = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.DIALOG_SURFACE).assertExists()
    }

    @Test
    fun settingsDialogTitleDisplayed() = runComposeUiTest {
        setContent {
            SettingsDialog(
                component = createTestComponent(),
                onDismiss = {},
            )
        }

        onNodeWithText("API Settings").assertExists()
        onNodeWithTag(SettingsDialogTags.TITLE).assertExists()
    }

    @Test
    fun settingsDialogApiKeyFieldDisplaysValue() = runComposeUiTest {
        setContent {
            SettingsDialog(
                component = createTestComponent(initialSettings = defaultSettings.copy(apiKey = "my-secret-key")),
                onDismiss = {},
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
                component = createTestComponent(initialSettings = defaultSettings.copy(model = model.id)),
                onDismiss = {},
            )
        }

        onNodeWithTag(ModelSelectorTags.TEXT_FIELD).assertExists()
        onNodeWithText("${model.displayName} (${model.level})").assertExists()
    }

    @Test
    fun settingsDialogMaxTokensFieldDisplaysValue() = runComposeUiTest {
        setContent {
            SettingsDialog(
                component = createTestComponent(initialSettings = defaultSettings.copy(maxTokens = 2000)),
                onDismiss = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.MAX_TOKENS_FIELD).assertExists()
        onNodeWithText("2000").assertExists()
    }

    @Test
    fun settingsDialogMaxTokensFieldEmptyWhenNull() = runComposeUiTest {
        setContent {
            SettingsDialog(
                component = createTestComponent(initialSettings = defaultSettings.copy(maxTokens = null)),
                onDismiss = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.MAX_TOKENS_FIELD).assertExists()
    }

    @Test
    fun settingsDialogTemperatureSliderExists() = runComposeUiTest {
        setContent {
            SettingsDialog(
                component = createTestComponent(),
                onDismiss = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.TEMPERATURE_SLIDER).assertExists()
    }

    @Test
    fun settingsDialogTemperatureLabelDisplaysValue() = runComposeUiTest {
        setContent {
            SettingsDialog(
                component = createTestComponent(initialSettings = defaultSettings.copy(temperature = 0.5)),
                onDismiss = {},
            )
        }

        onNodeWithText("Temperature: 0.5").assertExists()
    }

    @Test
    fun settingsDialogStopSequencesFieldDisplaysValue() = runComposeUiTest {
        setContent {
            SettingsDialog(
                component = createTestComponent(initialSettings = defaultSettings.copy(stopSequences = "stop1,stop2")),
                onDismiss = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.STOP_SEQUENCES_FIELD).assertExists()
        onNodeWithText("stop1,stop2").assertExists()
    }

    @Test
    fun settingsDialogTextFormatChipExists() = runComposeUiTest {
        setContent {
            SettingsDialog(
                component = createTestComponent(initialSettings = defaultSettings.copy(responseFormat = "text")),
                onDismiss = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.TEXT_FORMAT_CHIP).assertExists()
        onNodeWithText("Text").assertExists()
    }

    @Test
    fun settingsDialogJsonFormatChipExists() = runComposeUiTest {
        setContent {
            SettingsDialog(
                component = createTestComponent(initialSettings = defaultSettings.copy(responseFormat = "json")),
                onDismiss = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.JSON_FORMAT_CHIP).assertExists()
        onNodeWithText("JSON").assertExists()
    }

    @Test
    fun settingsDialogCancelButtonExists() = runComposeUiTest {
        setContent {
            SettingsDialog(
                component = createTestComponent(),
                onDismiss = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.CANCEL_BUTTON).assertExists()
        onNodeWithText("Cancel").assertExists()
    }

    @Test
    fun settingsDialogSaveButtonExists() = runComposeUiTest {
        setContent {
            SettingsDialog(
                component = createTestComponent(),
                onDismiss = {},
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
                component = createTestComponent(),
                onDismiss = { dismissCalled = true },
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
                component = createTestComponent(onSettingsSaved = { savedSettings = it }),
                onDismiss = {},
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
                component = createTestComponent(
                    initialSettings = defaultSettings.copy(apiKey = "saved-api-key"),
                    onSettingsSaved = { savedSettings = it }
                ),
                onDismiss = {},
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
                component = createTestComponent(
                    initialSettings = defaultSettings.copy(model = "saved-model"),
                    onSettingsSaved = { savedSettings = it }
                ),
                onDismiss = {},
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
                component = createTestComponent(
                    initialSettings = defaultSettings.copy(temperature = 1.5),
                    onSettingsSaved = { savedSettings = it }
                ),
                onDismiss = {},
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
                component = createTestComponent(
                    initialSettings = defaultSettings.copy(responseFormat = "json"),
                    onSettingsSaved = { savedSettings = it }
                ),
                onDismiss = {},
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
                component = createTestComponent(
                    initialSettings = defaultSettings.copy(stopSequences = "stop,end"),
                    onSettingsSaved = { savedSettings = it }
                ),
                onDismiss = {},
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
                component = createTestComponent(onSettingsSaved = { savedSettings = it }),
                onDismiss = {},
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
                component = createTestComponent(
                    initialSettings = defaultSettings.copy(maxTokens = null),
                    onSettingsSaved = { savedSettings = it }
                ),
                onDismiss = {},
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
                component = createTestComponent(
                    initialSettings = defaultSettings.copy(stopSequences = ""),
                    onSettingsSaved = { savedSettings = it }
                ),
                onDismiss = {},
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
                component = createTestComponent(
                    initialSettings = defaultSettings.copy(maxTokens = 1000),
                    onSettingsSaved = { savedSettings = it }
                ),
                onDismiss = {},
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
                component = createTestComponent(
                    initialSettings = defaultSettings.copy(responseFormat = "json"),
                    onSettingsSaved = { savedSettings = it }
                ),
                onDismiss = {},
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
                component = createTestComponent(
                    initialSettings = defaultSettings.copy(responseFormat = "text"),
                    onSettingsSaved = { savedSettings = it }
                ),
                onDismiss = {},
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
                component = createTestComponent(),
                onDismiss = {},
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
                component = createTestComponent(),
                onDismiss = {},
            )
        }

        onNodeWithTag(SettingsDialogTags.CANCEL_BUTTON).assertExists()
        onNodeWithTag(SettingsDialogTags.SAVE_BUTTON).assertExists()
    }

    @Test
    fun settingsDialogLabelsDisplayed() = runComposeUiTest {
        setContent {
            SettingsDialog(
                component = createTestComponent(),
                onDismiss = {},
            )
        }

        onNodeWithText("API Key").assertExists()
        onNodeWithText("Model").assertExists()
        onNodeWithText("Max Tokens (empty = unlimited)").assertExists()
        onNodeWithText("Stop Sequences (comma-separated)").assertExists()
        onNodeWithText("Response Format").assertExists()
    }
}
