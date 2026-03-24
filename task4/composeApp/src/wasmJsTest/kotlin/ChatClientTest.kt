import model.ResponseFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChatClientTest {
    @Test
    fun testResponseConstraintsDefaults() {
        val constraints = ResponseConstraints()
        assertNull(constraints.maxTokens)
        assertNull(constraints.stop)
        assertNull(constraints.responseFormat)
        assertNull(constraints.temperature)
    }

    @Test
    fun testResponseConstraintsWithAllFields() {
        val constraints = ResponseConstraints(
            maxTokens = 500,
            stop = listOf("stop1", "stop2"),
            responseFormat = ResponseFormat("json_object"),
            temperature = 0.8,
        )
        assertEquals(500, constraints.maxTokens)
        assertEquals(listOf("stop1", "stop2"), constraints.stop)
        assertEquals(ResponseFormat("json_object"), constraints.responseFormat)
        assertEquals(0.8, constraints.temperature)
    }

    @Test
    fun testResponseConstraintsCopy() {
        val original = ResponseConstraints(maxTokens = 100)
        val copy = original.copy(maxTokens = 200)
        assertEquals(100, original.maxTokens)
        assertEquals(200, copy.maxTokens)
    }

    @Test
    fun testResponseConstraintsWithPartialFields() {
        val constraints = ResponseConstraints(maxTokens = 1000)
        assertEquals(1000, constraints.maxTokens)
        assertNull(constraints.stop)
        assertNull(constraints.responseFormat)
        assertNull(constraints.temperature)
    }

    @Test
    fun testResponseConstraintsWithStopOnly() {
        val constraints = ResponseConstraints(stop = listOf("END"))
        assertNull(constraints.maxTokens)
        assertEquals(listOf("END"), constraints.stop)
    }

    @Test
    fun testResponseConstraintsWithTemperatureOnly() {
        val constraints = ResponseConstraints(temperature = 0.5)
        assertEquals(0.5, constraints.temperature)
        assertNull(constraints.maxTokens)
    }

    @Test
    fun testDataClassEquality() {
        val c1 = ResponseConstraints(maxTokens = 100)
        val c2 = ResponseConstraints(maxTokens = 100)
        assertEquals(c1, c2)
    }

    @Test
    fun testDataClassHashCode() {
        val c1 = ResponseConstraints(maxTokens = 100)
        val c2 = ResponseConstraints(maxTokens = 100)
        assertEquals(c1.hashCode(), c2.hashCode())
    }

    @Test
    fun testDataClassToString() {
        val constraints = ResponseConstraints(maxTokens = 100)
        val str = constraints.toString()
        assertTrue(str.contains("ResponseConstraints"))
        assertTrue(str.contains("100"))
    }
}
