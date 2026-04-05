import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import settings.ApiSettings
import settings.SettingsIntent
import settings.SettingsState
import settings.store.SettingsStore
import settings.store.SettingsStoreFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsStoreTest {
    private fun createStore(
        initialState: SettingsState = SettingsState(),
        onSettingsSaved: (ApiSettings) -> Unit = {}
    ): SettingsStore = SettingsStoreFactory(
        storeFactory = DefaultStoreFactory(),
        onSettingsSaved = onSettingsSaved
    ).create(initialState)

    @Test
    fun testInitialState() = runTest {
        val store = createStore()
        assertEquals("", store.state.settings.apiKey)
        assertEquals("glm-5", store.state.settings.model)
        assertEquals(1.0, store.state.settings.temperature)
        assertFalse(store.state.isLoading)
        assertNull(store.state.validationError)
        assertFalse(store.state.isApiKeyVisible)
    }

    @Test
    fun testIntentUpdateApiKey() = runTest {
        val store = createStore()
        store.accept(SettingsIntent.UpdateApiKey("new-api-key"))
        assertEquals("new-api-key", store.state.settings.apiKey)
    }

    @Test
    fun testIntentUpdateModel() = runTest {
        val store = createStore()
        store.accept(SettingsIntent.UpdateModel("custom-model"))
        assertEquals("custom-model", store.state.settings.model)
    }

    @Test
    fun testIntentUpdateMaxTokens() = runTest {
        val store = createStore()
        store.accept(SettingsIntent.UpdateMaxTokens(500))
        assertEquals(500, store.state.settings.maxTokens)
    }

    @Test
    fun testIntentUpdateTemperature() = runTest {
        val store = createStore()
        store.accept(SettingsIntent.UpdateTemperature(0.7))
        assertEquals(0.7, store.state.settings.temperature)
    }

    @Test
    fun testIntentUpdateStopSequences() = runTest {
        val store = createStore()
        store.accept(SettingsIntent.UpdateStopSequences("stop1,stop2"))
        assertEquals("stop1,stop2", store.state.settings.stopSequences)
    }

    @Test
    fun testIntentUpdateResponseFormat() = runTest {
        val store = createStore()
        store.accept(SettingsIntent.UpdateResponseFormat("json"))
        assertEquals("json", store.state.settings.responseFormat)
    }

    @Test
    fun testIntentUpdateSettings() = runTest {
        val store = createStore()
        val newSettings = ApiSettings(
            apiKey = "test-key",
            model = "test-model",
            temperature = 0.5
        )
        store.accept(SettingsIntent.UpdateSettings(newSettings))
        assertEquals("test-key", store.state.settings.apiKey)
        assertEquals("test-model", store.state.settings.model)
        assertEquals(0.5, store.state.settings.temperature)
    }

    @Test
    fun testUpdateApiKeyDirectly() = runTest {
        val store = createStore()
        store.accept(SettingsIntent.UpdateApiKey("direct-key"))
        assertEquals("direct-key", store.state.settings.apiKey)
    }

    @Test
    fun testUpdateModelDirectly() = runTest {
        val store = createStore()
        store.accept(SettingsIntent.UpdateModel("direct-model"))
        assertEquals("direct-model", store.state.settings.model)
    }

    @Test
    fun testUpdateTemperatureDirectly() = runTest {
        val store = createStore()
        store.accept(SettingsIntent.UpdateTemperature(0.3))
        assertEquals(0.3, store.state.settings.temperature)
    }

    @Test
    fun testSaveSettingsWithEmptyApiKey() = runTest {
        val store = createStore()
        store.accept(SettingsIntent.SaveSettings)
        assertEquals("API key is required", store.state.validationError)
    }

    @Test
    fun testSaveSettingsWithInvalidTemperatureLow() = runTest {
        val store = createStore()
        store.accept(SettingsIntent.UpdateApiKey("valid-key"))
        store.accept(SettingsIntent.UpdateTemperature(-0.5))
        store.accept(SettingsIntent.SaveSettings)
        assertEquals("Temperature must be between 0.0 and 2.0", store.state.validationError)
    }

    @Test
    fun testSaveSettingsWithInvalidTemperatureHigh() = runTest {
        val store = createStore()
        store.accept(SettingsIntent.UpdateApiKey("valid-key"))
        store.accept(SettingsIntent.UpdateTemperature(3.0))
        store.accept(SettingsIntent.SaveSettings)
        assertEquals("Temperature must be between 0.0 and 2.0", store.state.validationError)
    }

    @Test
    fun testSaveSettingsWithValidTemperatureBoundaryLow() = runTest {
        var savedSettings: ApiSettings? = null
        val store = createStore(onSettingsSaved = { settings ->
            savedSettings = settings
        })
        store.accept(SettingsIntent.UpdateApiKey("valid-key"))
        store.accept(SettingsIntent.UpdateTemperature(0.0))
        store.accept(SettingsIntent.SaveSettings)
        assertEquals(0.0, savedSettings?.temperature)
    }

    @Test
    fun testSaveSettingsWithValidTemperatureBoundaryHigh() = runTest {
        var savedSettings: ApiSettings? = null
        val store = createStore(onSettingsSaved = { settings ->
            savedSettings = settings
        })
        store.accept(SettingsIntent.UpdateApiKey("valid-key"))
        store.accept(SettingsIntent.UpdateTemperature(2.0))
        store.accept(SettingsIntent.SaveSettings)
        assertEquals(2.0, savedSettings?.temperature)
    }

    @Test
    fun testResetSettings() = runTest {
        val store = createStore()
        store.accept(SettingsIntent.UpdateApiKey("test-key"))
        store.accept(SettingsIntent.UpdateModel("custom-model"))
        store.accept(SettingsIntent.UpdateTemperature(0.5))
        store.accept(SettingsIntent.SaveSettings)

        store.accept(SettingsIntent.ResetSettings)

        assertEquals("", store.state.settings.apiKey)
        assertEquals("glm-5", store.state.settings.model)
        assertEquals(1.0, store.state.settings.temperature)
        assertNull(store.state.validationError)
    }

    @Test
    fun testToggleApiKeyVisibility() = runTest {
        val store = createStore()
        assertFalse(store.state.isApiKeyVisible)

        store.accept(SettingsIntent.ToggleApiKeyVisibility)
        assertTrue(store.state.isApiKeyVisible)

        store.accept(SettingsIntent.ToggleApiKeyVisibility)
        assertFalse(store.state.isApiKeyVisible)
    }

    @Test
    fun testClearValidationError() = runTest {
        val store = createStore()
        store.accept(SettingsIntent.SaveSettings)
        assertEquals("API key is required", store.state.validationError)

        store.accept(SettingsIntent.ClearValidationError)

        assertNull(store.state.validationError)
    }

    @Test
    fun testValidateApiKeyWithEmptyString() = runTest {
        val store = createStore()
        store.accept(SettingsIntent.ValidateApiKey(""))
        assertEquals("API key cannot be empty", store.state.validationError)
    }

    @Test
    fun testValidateApiKeyWithBlankString() = runTest {
        val store = createStore()
        store.accept(SettingsIntent.ValidateApiKey("   "))
        assertEquals("API key cannot be empty", store.state.validationError)
    }

    @Test
    fun testValidateApiKeyWithValidKey() = runTest {
        val store = createStore()
        store.accept(SettingsIntent.ValidateApiKey("valid-key"))
        assertNull(store.state.validationError)
    }

    @Test
    fun testLoadSettingsViaInitialState() = runTest {
        val settings = ApiSettings(
            apiKey = "loaded-key",
            model = "loaded-model",
            temperature = 0.8
        )
        val store = createStore(initialState = SettingsState(settings = settings))

        assertEquals("loaded-key", store.state.settings.apiKey)
        assertEquals("loaded-model", store.state.settings.model)
        assertEquals(0.8, store.state.settings.temperature)
    }

    @Test
    fun testUpdateSettingsFunction() = runTest {
        val store = createStore()
        val newSettings = ApiSettings(
            apiKey = "updated-key",
            model = "updated-model"
        )

        store.accept(SettingsIntent.UpdateSettings(newSettings))

        assertEquals("updated-key", store.state.settings.apiKey)
        assertEquals("updated-model", store.state.settings.model)
    }

    @Test
    fun testValidationClearsErrorOnSuccess() = runTest {
        val store = createStore()
        store.accept(SettingsIntent.SaveSettings)
        assertEquals("API key is required", store.state.validationError)

        store.accept(SettingsIntent.UpdateApiKey("valid-key"))
        store.accept(SettingsIntent.SaveSettings)

        assertNull(store.state.validationError)
    }

    @Test
    fun testMultipleUpdates() = runTest {
        val store = createStore()

        store.accept(SettingsIntent.UpdateApiKey("key1"))
        store.accept(SettingsIntent.UpdateModel("model1"))
        store.accept(SettingsIntent.UpdateTemperature(0.5))
        store.accept(SettingsIntent.UpdateMaxTokens(1000))
        store.accept(SettingsIntent.UpdateStopSequences("stop1,stop2"))
        store.accept(SettingsIntent.UpdateResponseFormat("json"))

        assertEquals("key1", store.state.settings.apiKey)
        assertEquals("model1", store.state.settings.model)
        assertEquals(0.5, store.state.settings.temperature)
        assertEquals(1000, store.state.settings.maxTokens)
        assertEquals("stop1,stop2", store.state.settings.stopSequences)
        assertEquals("json", store.state.settings.responseFormat)
    }

    @Test
    fun testSaveSettingsWithValidData() = runTest {
        var savedSettings: ApiSettings? = null
        val store = createStore(onSettingsSaved = { settings ->
            savedSettings = settings
        })
        store.accept(SettingsIntent.UpdateApiKey("valid-api-key"))
        store.accept(SettingsIntent.UpdateModel("test-model"))
        store.accept(SettingsIntent.UpdateTemperature(0.8))
        store.accept(SettingsIntent.SaveSettings)

        assertEquals("valid-api-key", savedSettings?.apiKey)
        assertEquals("test-model", savedSettings?.model)
        assertEquals(0.8, savedSettings?.temperature)
    }

    @Test
    fun testIntentRoutesToUpdateApiKey() = runTest {
        val store = createStore()
        val intent = SettingsIntent.UpdateApiKey("routed-key")
        store.accept(intent)
        assertEquals("routed-key", store.state.settings.apiKey)
    }

    @Test
    fun testIntentRoutesToUpdateModel() = runTest {
        val store = createStore()
        val intent = SettingsIntent.UpdateModel("routed-model")
        store.accept(intent)
        assertEquals("routed-model", store.state.settings.model)
    }

    @Test
    fun testIntentRoutesToUpdateTemperature() = runTest {
        val store = createStore()
        val intent = SettingsIntent.UpdateTemperature(0.25)
        store.accept(intent)
        assertEquals(0.25, store.state.settings.temperature)
    }

    @Test
    fun testIntentRoutesToResetSettings() = runTest {
        val store = createStore()
        store.accept(SettingsIntent.UpdateApiKey("to-reset"))
        store.accept(SettingsIntent.ResetSettings)
        assertEquals("", store.state.settings.apiKey)
    }

    @Test
    fun testIntentRoutesToToggleApiKeyVisibility() = runTest {
        val store = createStore()
        val intent = SettingsIntent.ToggleApiKeyVisibility
        store.accept(intent)
        assertTrue(store.state.isApiKeyVisible)
    }
}
