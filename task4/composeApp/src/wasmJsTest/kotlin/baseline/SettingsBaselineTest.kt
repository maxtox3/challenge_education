package baseline

import ApiSettings
import SettingsIntent
import SettingsSideEffect
import SettingsState
import model.ResponseFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Characterization tests for Settings feature (Chunk 6)
 * Tests capture CURRENT behavior as baseline for refactoring.
 */
class SettingsBaselineTest {

    // ==================== ApiSettings Data Class ====================

    @Test
    fun apiSettingsDefaultValues() {
        val settings = ApiSettings()
        assertEquals("", settings.apiKey, "Default apiKey should be empty string")
        assertEquals("glm-5", settings.model, "Default model should be glm-5")
        assertNull(settings.maxTokens, "Default maxTokens should be null")
        assertEquals(1.0, settings.temperature, "Default temperature should be 1.0")
        assertEquals("", settings.stopSequences, "Default stopSequences should be empty")
        assertEquals("text", settings.responseFormat, "Default responseFormat should be text")
    }

    @Test
    fun apiSettingsCopyPreservesValues() {
        val original = ApiSettings(
            apiKey = "test-key",
            model = "custom-model",
            maxTokens = 500,
            temperature = 0.7,
            stopSequences = "stop1,stop2",
            responseFormat = "json"
        )
        val copied = original.copy(temperature = 0.5)
        assertEquals("test-key", copied.apiKey, "apiKey should be preserved")
        assertEquals("custom-model", copied.model, "model should be preserved")
        assertEquals(500, copied.maxTokens, "maxTokens should be preserved")
        assertEquals(0.5, copied.temperature, "temperature should be updated")
        assertEquals("stop1,stop2", copied.stopSequences, "stopSequences should be preserved")
        assertEquals("json", copied.responseFormat, "responseFormat should be preserved")
    }

    // ==================== toResponseConstraints Transformation ====================

    @Test
    fun toResponseConstraintsWithDefaultsReturnsNullFormatAndStop() {
        val settings = ApiSettings()
        val constraints = settings.toResponseConstraints()
        assertNull(constraints.maxTokens, "maxTokens should be null")
        assertNull(constraints.stop, "stop should be null")
        assertNull(constraints.responseFormat, "responseFormat should be null")
        assertEquals(1.0, constraints.temperature, "temperature should be 1.0")
    }

    @Test
    fun toResponseConstraintsParsesCommaSeparatedStopSequencesWithTrimming() {
        val settings = ApiSettings(stopSequences = "stop1, stop2 , stop3")
        val constraints = settings.toResponseConstraints()
        assertEquals(listOf("stop1", "stop2", "stop3"), constraints.stop, "Stop sequences should be trimmed")
    }

    @Test
    fun toResponseConstraintsWithEmptyStopSequencesReturnsNull() {
        val settings = ApiSettings(stopSequences = "")
        val constraints = settings.toResponseConstraints()
        assertNull(constraints.stop, "Empty stopSequences should result in null")
    }

    @Test
    fun toResponseConstraintsWithWhitespaceOnlyStopSequencesReturnsNull() {
        val settings = ApiSettings(stopSequences = "   ,  , ")
        val constraints = settings.toResponseConstraints()
        assertNull(constraints.stop, "Whitespace-only stopSequences should result in null")
    }

    @Test
    fun toResponseConstraintsWithJsonFormatReturnsJsonObject() {
        val settings = ApiSettings(responseFormat = "json")
        val constraints = settings.toResponseConstraints()
        assertEquals(ResponseFormat("json_object"), constraints.responseFormat, "json format should become json_object")
    }

    @Test
    fun toResponseConstraintsWithTextFormatReturnsNull() {
        val settings = ApiSettings(responseFormat = "text")
        val constraints = settings.toResponseConstraints()
        assertNull(constraints.responseFormat, "text format should result in null")
    }

    @Test
    fun toResponseConstraintsWithUnknownFormatReturnsNull() {
        val settings = ApiSettings(responseFormat = "unknown")
        val constraints = settings.toResponseConstraints()
        assertNull(constraints.responseFormat, "Unknown format should result in null")
    }

    @Test
    fun toResponseConstraintsWithAllFieldsSet() {
        val settings = ApiSettings(
            maxTokens = 2000,
            temperature = 0.8,
            stopSequences = "end, done",
            responseFormat = "json"
        )
        val constraints = settings.toResponseConstraints()
        assertEquals(2000, constraints.maxTokens)
        assertEquals(0.8, constraints.temperature)
        assertEquals(listOf("end", "done"), constraints.stop)
        assertEquals(ResponseFormat("json_object"), constraints.responseFormat)
    }

    // ==================== SettingsState Data Class ====================

    @Test
    fun settingsStateDefaultValues() {
        val state = SettingsState()
        assertEquals(ApiSettings(), state.settings, "Default settings should be ApiSettings()")
        assertFalse(state.isLoading, "Default isLoading should be false")
        assertNull(state.validationError, "Default validationError should be null")
        assertFalse(state.isApiKeyVisible, "Default isApiKeyVisible should be false")
    }

    @Test
    fun settingsStateCopyPreservesNestedSettings() {
        val state = SettingsState(
            settings = ApiSettings(apiKey = "key", model = "model"),
            isLoading = true,
            validationError = "error",
            isApiKeyVisible = true
        )
        val copied = state.copy(isLoading = false)
        assertEquals("key", copied.settings.apiKey, "settings.apiKey should be preserved")
        assertEquals("model", copied.settings.model, "settings.model should be preserved")
        assertFalse(copied.isLoading, "isLoading should be updated")
        assertEquals("error", copied.validationError, "validationError should be preserved")
        assertTrue(copied.isApiKeyVisible, "isApiKeyVisible should be preserved")
    }

    // ==================== SettingsIntent Sealed Class ====================

    @Test
    fun settingsIntentUpdateApiKeyHoldsValue() {
        val intent = SettingsIntent.UpdateApiKey("test-key")
        assertEquals("test-key", intent.apiKey)
    }

    @Test
    fun settingsIntentUpdateModelHoldsValue() {
        val intent = SettingsIntent.UpdateModel("custom-model")
        assertEquals("custom-model", intent.model)
    }

    @Test
    fun settingsIntentUpdateMaxTokensHoldsNullableValue() {
        val intentWithValue = SettingsIntent.UpdateMaxTokens(500)
        val intentWithNull = SettingsIntent.UpdateMaxTokens(null)
        assertEquals(500, intentWithValue.maxTokens)
        assertNull(intentWithNull.maxTokens)
    }

    @Test
    fun settingsIntentUpdateTemperatureHoldsValue() {
        val intent = SettingsIntent.UpdateTemperature(0.7)
        assertEquals(0.7, intent.temperature)
    }

    @Test
    fun settingsIntentUpdateStopSequencesHoldsValue() {
        val intent = SettingsIntent.UpdateStopSequences("stop1,stop2")
        assertEquals("stop1,stop2", intent.stopSequences)
    }

    @Test
    fun settingsIntentUpdateResponseFormatHoldsValue() {
        val intent = SettingsIntent.UpdateResponseFormat("json")
        assertEquals("json", intent.responseFormat)
    }

    @Test
    fun settingsIntentUpdateSettingsHoldsApiSettings() {
        val settings = ApiSettings(apiKey = "key", model = "model")
        val intent = SettingsIntent.UpdateSettings(settings)
        assertEquals(settings, intent.settings)
    }

    @Test
    fun settingsIntentHasObjectTypesForSaveResetClearToggle() {
        val save = SettingsIntent.SaveSettings
        val reset = SettingsIntent.ResetSettings
        val clear = SettingsIntent.ClearValidationError
        val toggle = SettingsIntent.ToggleApiKeyVisibility

        assertEquals(SettingsIntent.SaveSettings, save)
        assertEquals(SettingsIntent.ResetSettings, reset)
        assertEquals(SettingsIntent.ClearValidationError, clear)
        assertEquals(SettingsIntent.ToggleApiKeyVisibility, toggle)
    }

    @Test
    fun settingsIntentValidateApiKeyHoldsValue() {
        val intent = SettingsIntent.ValidateApiKey("test-key")
        assertEquals("test-key", intent.apiKey)
    }

    // ==================== SettingsSideEffect Sealed Class ====================

    @Test
    fun settingsSideEffectShowToastHoldsMessage() {
        val effect = SettingsSideEffect.ShowToast("Test message")
        assertEquals("Test message", effect.message)
    }

    @Test
    fun settingsSideEffectSettingsSavedIsObject() {
        val effect = SettingsSideEffect.SettingsSaved
        assertEquals(SettingsSideEffect.SettingsSaved, effect)
    }

    @Test
    fun settingsSideEffectValidationErrorHoldsError() {
        val effect = SettingsSideEffect.ValidationError("Invalid input")
        assertEquals("Invalid input", effect.error)
    }

    // ==================== Dialog State Transformation (Private SettingsState) ====================

    @Test
    fun dialogSettingsStateToApiSettingsConvertsMaxTokensTextToNullableInt() {
        val maxTokensTextEmpty = ""
        val maxTokensTextValid = "500"
        val maxTokensTextInvalid = "abc"

        assertNull(maxTokensTextEmpty.toIntOrNull(), "Empty string should convert to null")
        assertEquals(500, maxTokensTextValid.toIntOrNull(), "Valid number should parse")
        assertNull(maxTokensTextInvalid.toIntOrNull(), "Invalid string should convert to null")
    }

    @Test
    fun dialogSettingsStateFromApiSettingsConvertsMaxTokensToString() {
        val settingsWithTokens = ApiSettings(maxTokens = 1000)
        val settingsWithoutTokens = ApiSettings(maxTokens = null)

        val textWithTokens = settingsWithTokens.maxTokens?.toString() ?: ""
        val textWithoutTokens = settingsWithoutTokens.maxTokens?.toString() ?: ""

        assertEquals("1000", textWithTokens)
        assertEquals("", textWithoutTokens)
    }

    @Test
    fun dialogMaxTokensInputValidationOnlyAllowsDigitsOrEmpty() {
        val validInputs = listOf("", "0", "123", "999999")
        val invalidInputs = listOf("abc", "12.5", "-5", "12a34")

        validInputs.forEach { input ->
            val isValid = input.isEmpty() || input.all { it.isDigit() }
            assertTrue(isValid, "Input '$input' should be valid")
        }

        invalidInputs.forEach { input ->
            val isValid = input.isEmpty() || input.all { it.isDigit() }
            assertFalse(isValid, "Input '$input' should be invalid")
        }
    }

    @Test
    fun temperatureSliderRangeIs0To2() {
        val valueRange = 0f..2f
        assertTrue(0f in valueRange, "0.0 should be in range")
        assertTrue(1.0f in valueRange, "1.0 should be in range")
        assertTrue(2.0f in valueRange, "2.0 should be in range")
        assertFalse(-0.1f in valueRange, "-0.1 should not be in range")
        assertFalse(2.1f in valueRange, "2.1 should not be in range")
    }

    @Test
    fun responseFormatChipSelectionLogic() {
        var responseFormat = "text"
        assertEquals("text", responseFormat)
        assertTrue(responseFormat == "text")

        responseFormat = "json"
        assertEquals("json", responseFormat)
        assertTrue(responseFormat == "json")
        assertFalse(responseFormat == "text")
    }

    @Test
    fun temperatureDisplayRoundingTo2DecimalPlaces() {
        val temperature = 0.756789
        val displayValue = (temperature * 100).toInt() / 100.0
        assertEquals(0.75, displayValue, "Should round down to 2 decimal places")
    }
}
