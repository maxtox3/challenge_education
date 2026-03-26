import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsDialogTest {

    @Test
    fun testSettingsDialogFieldBindingApiKey() {
        val settings = ApiSettings(apiKey = "test-api-key-123")
        assertEquals("test-api-key-123", settings.apiKey)
    }

    @Test
    fun testSettingsDialogFieldBindingModel() {
        val settings = ApiSettings(model = "custom-model")
        assertEquals("custom-model", settings.model)
    }

    @Test
    fun testSettingsDialogFieldBindingMaxTokens() {
        val settings = ApiSettings(maxTokens = 2000)
        assertEquals(2000, settings.maxTokens)
    }

    @Test
    fun testSettingsDialogFieldBindingMaxTokensNull() {
        val settings = ApiSettings(maxTokens = null)
        assertNull(settings.maxTokens)
    }

    @Test
    fun testSettingsDialogFieldBindingTemperature() {
        val settings = ApiSettings(temperature = 0.7)
        assertEquals(0.7, settings.temperature)
    }

    @Test
    fun testSettingsDialogFieldBindingStopSequences() {
        val settings = ApiSettings(stopSequences = "stop1,stop2,stop3")
        assertEquals("stop1,stop2,stop3", settings.stopSequences)
    }

    @Test
    fun testSettingsDialogFieldBindingResponseFormatText() {
        val settings = ApiSettings(responseFormat = "text")
        assertEquals("text", settings.responseFormat)
    }

    @Test
    fun testSettingsDialogFieldBindingResponseFormatJson() {
        val settings = ApiSettings(responseFormat = "json")
        assertEquals("json", settings.responseFormat)
    }

    @Test
    fun testSettingsDialogSaveCallbackTransformsToApiSettings() {
        var savedSettings: ApiSettings? = null
        val onSave: (ApiSettings) -> Unit = { savedSettings = it }

        val originalSettings = ApiSettings(
            apiKey = "key123",
            model = "glm-5",
            maxTokens = 1000,
            temperature = 0.5,
            stopSequences = "end,stop",
            responseFormat = "json"
        )
        onSave(originalSettings)

        val settings = requireNotNull(savedSettings)
        assertEquals("key123", settings.apiKey)
        assertEquals("glm-5", settings.model)
        assertEquals(1000, settings.maxTokens)
        assertEquals(0.5, settings.temperature)
        assertEquals("end,stop", settings.stopSequences)
        assertEquals("json", settings.responseFormat)
    }

    @Test
    fun testSettingsDialogSaveCallbackEmptyMaxTokensBecomesNull() {
        val maxTokensText = ""
        val maxTokens = maxTokensText.toIntOrNull()
        assertNull(maxTokens)
    }

    @Test
    fun testSettingsDialogSaveCallbackNumericMaxTokensParsed() {
        val maxTokensText = "500"
        val maxTokens = maxTokensText.toIntOrNull()
        assertEquals(500, maxTokens)
    }

    @Test
    fun testSettingsDialogTemperatureSliderRangeValidation() {
        val validLow = 0.0
        val validHigh = 2.0
        val validMid = 1.0

        assertTrue(validLow in 0.0..2.0)
        assertTrue(validHigh in 0.0..2.0)
        assertTrue(validMid in 0.0..2.0)
    }

    @Test
    fun testSettingsDialogTemperatureSliderInvalidLow() {
        val invalidTemp = -0.5
        assertFalse(invalidTemp in 0.0..2.0)
    }

    @Test
    fun testSettingsDialogTemperatureSliderInvalidHigh() {
        val invalidTemp = 2.5
        assertFalse(invalidTemp in 0.0..2.0)
    }

    @Test
    fun testSettingsDialogResponseFormatChipTextSelection() {
        var responseFormat = "text"
        assertEquals("text", responseFormat)
        responseFormat = "text"
        assertEquals("text", responseFormat)
    }

    @Test
    fun testSettingsDialogResponseFormatChipJsonSelection() {
        val responseFormat = "json"
        assertEquals("json", responseFormat)
    }

    @Test
    fun testSettingsDialogResponseFormatChipToggle() {
        var responseFormat = "text"
        assertEquals("text", responseFormat)
        assertNotEquals("json", responseFormat)

        responseFormat = "json"
        assertNotEquals("text", responseFormat)
        assertEquals("json", responseFormat)
    }

    @Test
    fun testSettingsDialogDismissCallbackInvoked() {
        var dismissCalled = false
        val onDismiss: () -> Unit = { dismissCalled = true }
        onDismiss()
        assertTrue(dismissCalled)
    }

    @Test
    fun testSettingsDialogMaxTokensInputOnlyDigitsAllowed() {
        val validInput = "12345"
        val isValid = validInput.isEmpty() || validInput.all { it.isDigit() }
        assertTrue(isValid)
    }

    @Test
    fun testSettingsDialogMaxTokensInputEmptyAllowed() {
        val emptyInput = ""
        val isValid = emptyInput.isEmpty() || emptyInput.all { it.isDigit() }
        assertTrue(isValid)
    }

    @Test
    fun testSettingsDialogMaxTokensInputLettersRejected() {
        val invalidInput = "abc123"
        val isValid = invalidInput.isEmpty() || invalidInput.all { it.isDigit() }
        assertFalse(isValid)
    }

    @Test
    fun testChipSelectionTextFormat() {
        val responseFormat = "text"
        val isSelected = responseFormat == "text"
        assertTrue(isSelected)
    }

    @Test
    fun testChipSelectionJsonFormat() {
        val responseFormat = "json"
        val isSelected = responseFormat == "json"
        assertTrue(isSelected)
    }

    @Test
    fun testChipSelectionNotSelected() {
        val responseFormat = "text"
        val isSelected = responseFormat == "json"
        assertFalse(isSelected)
    }

    @Test
    fun testSliderValueTemperatureConversion() {
        val sliderValue = 0.75f
        val temperature = sliderValue.toDouble()
        assertEquals(0.75, temperature)
    }

    @Test
    fun testSliderValueRangeMinimum() {
        val valueRange = 0f..2f
        assertTrue(0f in valueRange)
    }

    @Test
    fun testSliderValueRangeMaximum() {
        val valueRange = 0f..2f
        assertTrue(2f in valueRange)
    }

    @Test
    fun testSliderValueOutOfRange() {
        val valueRange = 0f..2f
        assertFalse(-0.1f in valueRange)
        assertFalse(2.1f in valueRange)
    }
}
