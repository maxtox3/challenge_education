import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import settings.ApiSettings
import settings.SettingsIntent
import settings.SettingsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsViewModelTest {
    private fun createViewModel(onSettingsSaved: (ApiSettings) -> Unit = {}): SettingsViewModel = SettingsViewModel(
        viewModelScope = CoroutineScope(Dispatchers.Default),
        onSettingsSaved = onSettingsSaved
    )

    @Test
    fun testInitialState() {
        val viewModel = createViewModel()
        assertEquals("", viewModel.state.settings.apiKey)
        assertEquals("glm-5", viewModel.state.settings.model)
        assertEquals(1.0, viewModel.state.settings.temperature)
        assertFalse(viewModel.state.isLoading)
        assertNull(viewModel.state.validationError)
        assertFalse(viewModel.state.isApiKeyVisible)
    }

    @Test
    fun testProcessIntentUpdateApiKey() {
        val viewModel = createViewModel()
        viewModel.processIntent(SettingsIntent.UpdateApiKey("new-api-key"))
        assertEquals("new-api-key", viewModel.state.settings.apiKey)
    }

    @Test
    fun testProcessIntentUpdateModel() {
        val viewModel = createViewModel()
        viewModel.processIntent(SettingsIntent.UpdateModel("custom-model"))
        assertEquals("custom-model", viewModel.state.settings.model)
    }

    @Test
    fun testProcessIntentUpdateMaxTokens() {
        val viewModel = createViewModel()
        viewModel.processIntent(SettingsIntent.UpdateMaxTokens(500))
        assertEquals(500, viewModel.state.settings.maxTokens)
    }

    @Test
    fun testProcessIntentUpdateTemperature() {
        val viewModel = createViewModel()
        viewModel.processIntent(SettingsIntent.UpdateTemperature(0.7))
        assertEquals(0.7, viewModel.state.settings.temperature)
    }

    @Test
    fun testProcessIntentUpdateStopSequences() {
        val viewModel = createViewModel()
        viewModel.processIntent(SettingsIntent.UpdateStopSequences("stop1,stop2"))
        assertEquals("stop1,stop2", viewModel.state.settings.stopSequences)
    }

    @Test
    fun testProcessIntentUpdateResponseFormat() {
        val viewModel = createViewModel()
        viewModel.processIntent(SettingsIntent.UpdateResponseFormat("json"))
        assertEquals("json", viewModel.state.settings.responseFormat)
    }

    @Test
    fun testProcessIntentUpdateSettings() {
        val viewModel = createViewModel()
        val newSettings = ApiSettings(
            apiKey = "test-key",
            model = "test-model",
            temperature = 0.5
        )
        viewModel.processIntent(SettingsIntent.UpdateSettings(newSettings))
        assertEquals("test-key", viewModel.state.settings.apiKey)
        assertEquals("test-model", viewModel.state.settings.model)
        assertEquals(0.5, viewModel.state.settings.temperature)
    }

    @Test
    fun testUpdateApiKeyDirectly() {
        val viewModel = createViewModel()
        viewModel.processIntent(SettingsIntent.UpdateApiKey("direct-key"))
        assertEquals("direct-key", viewModel.state.settings.apiKey)
    }

    @Test
    fun testUpdateModelDirectly() {
        val viewModel = createViewModel()
        viewModel.processIntent(SettingsIntent.UpdateModel("direct-model"))
        assertEquals("direct-model", viewModel.state.settings.model)
    }

    @Test
    fun testUpdateTemperatureDirectly() {
        val viewModel = createViewModel()
        viewModel.processIntent(SettingsIntent.UpdateTemperature(0.3))
        assertEquals(0.3, viewModel.state.settings.temperature)
    }

    @Test
    fun testSaveSettingsWithEmptyApiKey() {
        val viewModel = createViewModel()
        viewModel.processIntent(SettingsIntent.SaveSettings)
        assertEquals("API key is required", viewModel.state.validationError)
    }

    @Test
    fun testSaveSettingsWithInvalidTemperatureLow() {
        val viewModel = createViewModel()
        viewModel.processIntent(SettingsIntent.UpdateApiKey("valid-key"))
        viewModel.processIntent(SettingsIntent.UpdateTemperature(-0.5))
        viewModel.processIntent(SettingsIntent.SaveSettings)
        assertEquals("Temperature must be between 0.0 and 2.0", viewModel.state.validationError)
    }

    @Test
    fun testSaveSettingsWithInvalidTemperatureHigh() {
        val viewModel = createViewModel()
        viewModel.processIntent(SettingsIntent.UpdateApiKey("valid-key"))
        viewModel.processIntent(SettingsIntent.UpdateTemperature(3.0))
        viewModel.processIntent(SettingsIntent.SaveSettings)
        assertEquals("Temperature must be between 0.0 and 2.0", viewModel.state.validationError)
    }

    @Test
    fun testSaveSettingsWithValidTemperatureBoundaryLow() {
        var savedSettings: ApiSettings? = null
        val viewModel = createViewModel { settings ->
            savedSettings = settings
        }
        viewModel.processIntent(SettingsIntent.UpdateApiKey("valid-key"))
        viewModel.processIntent(SettingsIntent.UpdateTemperature(0.0))
        viewModel.processIntent(SettingsIntent.SaveSettings)
        assertEquals(0.0, savedSettings?.temperature)
    }

    @Test
    fun testSaveSettingsWithValidTemperatureBoundaryHigh() {
        var savedSettings: ApiSettings? = null
        val viewModel = createViewModel { settings ->
            savedSettings = settings
        }
        viewModel.processIntent(SettingsIntent.UpdateApiKey("valid-key"))
        viewModel.processIntent(SettingsIntent.UpdateTemperature(2.0))
        viewModel.processIntent(SettingsIntent.SaveSettings)
        assertEquals(2.0, savedSettings?.temperature)
    }

    @Test
    fun testResetSettings() {
        val viewModel = createViewModel()
        viewModel.processIntent(SettingsIntent.UpdateApiKey("test-key"))
        viewModel.processIntent(SettingsIntent.UpdateModel("custom-model"))
        viewModel.processIntent(SettingsIntent.UpdateTemperature(0.5))
        viewModel.processIntent(SettingsIntent.SaveSettings)

        viewModel.processIntent(SettingsIntent.ResetSettings)

        assertEquals("", viewModel.state.settings.apiKey)
        assertEquals("glm-5", viewModel.state.settings.model)
        assertEquals(1.0, viewModel.state.settings.temperature)
        assertNull(viewModel.state.validationError)
    }

    @Test
    fun testToggleApiKeyVisibility() {
        val viewModel = createViewModel()
        assertFalse(viewModel.state.isApiKeyVisible)

        viewModel.processIntent(SettingsIntent.ToggleApiKeyVisibility)
        assertTrue(viewModel.state.isApiKeyVisible)

        viewModel.processIntent(SettingsIntent.ToggleApiKeyVisibility)
        assertFalse(viewModel.state.isApiKeyVisible)
    }

    @Test
    fun testClearValidationError() {
        val viewModel = createViewModel()
        viewModel.processIntent(SettingsIntent.SaveSettings)
        assertEquals("API key is required", viewModel.state.validationError)

        viewModel.processIntent(SettingsIntent.ClearValidationError)

        assertNull(viewModel.state.validationError)
    }

    @Test
    fun testValidateApiKeyWithEmptyString() {
        val viewModel = createViewModel()
        viewModel.processIntent(SettingsIntent.ValidateApiKey(""))
        assertEquals("API key cannot be empty", viewModel.state.validationError)
    }

    @Test
    fun testValidateApiKeyWithBlankString() {
        val viewModel = createViewModel()
        viewModel.processIntent(SettingsIntent.ValidateApiKey("   "))
        assertEquals("API key cannot be empty", viewModel.state.validationError)
    }

    @Test
    fun testValidateApiKeyWithValidKey() {
        val viewModel = createViewModel()
        viewModel.processIntent(SettingsIntent.ValidateApiKey("valid-key"))
        assertNull(viewModel.state.validationError)
    }

    @Test
    fun testLoadSettings() {
        val viewModel = createViewModel()
        val settings = ApiSettings(
            apiKey = "loaded-key",
            model = "loaded-model",
            temperature = 0.8
        )

        viewModel.loadSettings(settings)

        assertEquals("loaded-key", viewModel.state.settings.apiKey)
        assertEquals("loaded-model", viewModel.state.settings.model)
        assertEquals(0.8, viewModel.state.settings.temperature)
    }

    @Test
    fun testUpdateSettingsFunction() {
        val viewModel = createViewModel()
        val newSettings = ApiSettings(
            apiKey = "updated-key",
            model = "updated-model"
        )

        viewModel.updateSettings(newSettings)

        assertEquals("updated-key", viewModel.state.settings.apiKey)
        assertEquals("updated-model", viewModel.state.settings.model)
    }

    @Test
    fun testValidationClearsErrorOnSuccess() {
        val viewModel = createViewModel()
        viewModel.processIntent(SettingsIntent.SaveSettings)
        assertEquals("API key is required", viewModel.state.validationError)

        viewModel.processIntent(SettingsIntent.UpdateApiKey("valid-key"))
        viewModel.processIntent(SettingsIntent.SaveSettings)

        assertNull(viewModel.state.validationError)
    }

    @Test
    fun testMultipleUpdates() {
        val viewModel = createViewModel()

        viewModel.processIntent(SettingsIntent.UpdateApiKey("key1"))
        viewModel.processIntent(SettingsIntent.UpdateModel("model1"))
        viewModel.processIntent(SettingsIntent.UpdateTemperature(0.5))
        viewModel.processIntent(SettingsIntent.UpdateMaxTokens(1000))
        viewModel.processIntent(SettingsIntent.UpdateStopSequences("stop1,stop2"))
        viewModel.processIntent(SettingsIntent.UpdateResponseFormat("json"))

        assertEquals("key1", viewModel.state.settings.apiKey)
        assertEquals("model1", viewModel.state.settings.model)
        assertEquals(0.5, viewModel.state.settings.temperature)
        assertEquals(1000, viewModel.state.settings.maxTokens)
        assertEquals("stop1,stop2", viewModel.state.settings.stopSequences)
        assertEquals("json", viewModel.state.settings.responseFormat)
    }

    @Test
    fun testTypedPropertiesDirectAccess() {
        val viewModel = createViewModel()

        viewModel.settings = ApiSettings(apiKey = "direct-access")
        assertEquals("direct-access", viewModel.settings.apiKey)

        viewModel.isLoading = true
        assertTrue(viewModel.isLoading)

        viewModel.validationError = "Error message"
        assertEquals("Error message", viewModel.validationError)

        viewModel.isApiKeyVisible = true
        assertTrue(viewModel.isApiKeyVisible)
    }

    @Test
    fun testStateFlowValueMatchesState() {
        val viewModel = createViewModel()
        assertEquals(viewModel.state, viewModel.uiState.value)

        viewModel.processIntent(SettingsIntent.UpdateApiKey("test"))
        assertEquals(viewModel.state, viewModel.uiState.value)
    }

    @Test
    fun testProcessIntentClearValidationError() {
        val viewModel = createViewModel()
        viewModel.processIntent(SettingsIntent.SaveSettings)
        assertEquals("API key is required", viewModel.state.validationError)

        viewModel.processIntent(SettingsIntent.ClearValidationError)
        assertNull(viewModel.state.validationError)
    }

    @Test
    fun testSaveSettingsWithValidData() {
        var savedSettings: ApiSettings? = null
        val viewModel = createViewModel { settings ->
            savedSettings = settings
        }
        viewModel.processIntent(SettingsIntent.UpdateApiKey("valid-api-key"))
        viewModel.processIntent(SettingsIntent.UpdateModel("test-model"))
        viewModel.processIntent(SettingsIntent.UpdateTemperature(0.8))
        viewModel.processIntent(SettingsIntent.SaveSettings)

        assertEquals("valid-api-key", savedSettings?.apiKey)
        assertEquals("test-model", savedSettings?.model)
        assertEquals(0.8, savedSettings?.temperature)
    }

    @Test
    fun testProcessIntentRoutesToUpdateApiKey() {
        val viewModel = createViewModel()
        val intent = SettingsIntent.UpdateApiKey("routed-key")
        viewModel.processIntent(intent)
        assertEquals("routed-key", viewModel.state.settings.apiKey)
    }

    @Test
    fun testProcessIntentRoutesToUpdateModel() {
        val viewModel = createViewModel()
        val intent = SettingsIntent.UpdateModel("routed-model")
        viewModel.processIntent(intent)
        assertEquals("routed-model", viewModel.state.settings.model)
    }

    @Test
    fun testProcessIntentRoutesToUpdateTemperature() {
        val viewModel = createViewModel()
        val intent = SettingsIntent.UpdateTemperature(0.25)
        viewModel.processIntent(intent)
        assertEquals(0.25, viewModel.state.settings.temperature)
    }

    @Test
    fun testProcessIntentRoutesToResetSettings() {
        val viewModel = createViewModel()
        viewModel.processIntent(SettingsIntent.UpdateApiKey("to-reset"))
        viewModel.processIntent(SettingsIntent.ResetSettings)
        assertEquals("", viewModel.state.settings.apiKey)
    }

    @Test
    fun testProcessIntentRoutesToToggleApiKeyVisibility() {
        val viewModel = createViewModel()
        val intent = SettingsIntent.ToggleApiKeyVisibility
        viewModel.processIntent(intent)
        assertTrue(viewModel.state.isApiKeyVisible)
    }
}
