import model.ApiProvider
import model.ResponseFormat
import settings.ApiSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsStateTest {
    @Test
    fun testApiSettingsDefaults() {
        val settings = ApiSettings()
        assertEquals("9cccc72cda3c456c9263fe143dbae7b1.9ir3VQrPquSuSyvZ", settings.apiKey)
        assertEquals(ApiProvider.ZAI, settings.provider)
        assertEquals("glm-5", settings.model)
        assertNull(settings.maxTokens)
        assertEquals(1.0, settings.temperature)
        assertEquals("", settings.stopSequences)
        assertEquals("text", settings.responseFormat)
    }

    @Test
    fun testApiSettingsCustomValues() {
        val settings = ApiSettings(
            apiKey = "test-key",
            provider = ApiProvider.OPENROUTER,
            model = "custom-model",
            maxTokens = 500,
            temperature = 0.7,
            stopSequences = "stop1,stop2",
            responseFormat = "json",
        )
        assertEquals("test-key", settings.apiKey)
        assertEquals(ApiProvider.OPENROUTER, settings.provider)
        assertEquals("custom-model", settings.model)
        assertEquals(500, settings.maxTokens)
        assertEquals(0.7, settings.temperature)
        assertEquals("stop1,stop2", settings.stopSequences)
        assertEquals("json", settings.responseFormat)
    }

    @Test
    fun testToResponseConstraintsDefault() {
        val settings = ApiSettings()
        val constraints = settings.toResponseConstraints()
        assertNull(constraints.maxTokens)
        assertNull(constraints.stop)
        assertNull(constraints.responseFormat)
        assertEquals(1.0, constraints.temperature)
    }

    @Test
    fun testToResponseConstraintsWithMaxTokens() {
        val settings = ApiSettings(maxTokens = 1000)
        val constraints = settings.toResponseConstraints()
        assertEquals(1000, constraints.maxTokens)
    }

    @Test
    fun testToResponseConstraintsWithStopSequences() {
        val settings = ApiSettings(stopSequences = "stop1, stop2, stop3")
        val constraints = settings.toResponseConstraints()
        assertEquals(listOf("stop1", "stop2", "stop3"), constraints.stop)
    }

    @Test
    fun testToResponseConstraintsWithEmptyStopSequences() {
        val settings = ApiSettings(stopSequences = "")
        val constraints = settings.toResponseConstraints()
        assertNull(constraints.stop)
    }

    @Test
    fun testToResponseConstraintsWithJsonFormat() {
        val settings = ApiSettings(responseFormat = "json")
        val constraints = settings.toResponseConstraints()
        assertEquals(ResponseFormat("json_object"), constraints.responseFormat)
    }

    @Test
    fun testToResponseConstraintsWithTextFormat() {
        val settings = ApiSettings(responseFormat = "text")
        val constraints = settings.toResponseConstraints()
        assertNull(constraints.responseFormat)
    }

    @Test
    fun testToResponseConstraintsWithTemperature() {
        val settings = ApiSettings(temperature = 0.5)
        val constraints = settings.toResponseConstraints()
        assertEquals(0.5, constraints.temperature)
    }

    @Test
    fun testToResponseConstraintsAllFields() {
        val settings = ApiSettings(
            maxTokens = 2000,
            temperature = 0.8,
            stopSequences = "end, done",
            responseFormat = "json",
        )
        val constraints = settings.toResponseConstraints()
        assertEquals(2000, constraints.maxTokens)
        assertEquals(0.8, constraints.temperature)
        assertEquals(listOf("end", "done"), constraints.stop)
        assertEquals(ResponseFormat("json_object"), constraints.responseFormat)
    }
}
