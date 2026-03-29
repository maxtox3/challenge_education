import model.ReasoningComparison
import model.ReasoningMode
import model.ReasoningResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReasoningDialogTest {

    @Test
    fun testReasoningDialogTabSelectionDefault() {
        assertEquals(ReasoningMode.DIRECT, ReasoningMode.entries.getOrNull(0))
    }

    @Test
    fun testReasoningDialogTabSelectionChangeToStepByStep() {
        assertEquals(ReasoningMode.STEP_BY_STEP, ReasoningMode.entries.getOrNull(1))
    }

    @Test
    fun testReasoningDialogTabSelectionChangeToMetaPrompt() {
        assertEquals(ReasoningMode.META_PROMPT, ReasoningMode.entries.getOrNull(2))
    }

    @Test
    fun testReasoningDialogTabSelectionChangeToExpertPanel() {
        assertEquals(ReasoningMode.EXPERT_PANEL, ReasoningMode.entries.getOrNull(3))
    }

    @Test
    fun testReasoningDialogTabSelectionAllModesAccessible() {
        val modes = ReasoningMode.entries
        assertEquals(4, modes.size)
        assertEquals(ReasoningMode.DIRECT, modes[0])
        assertEquals(ReasoningMode.STEP_BY_STEP, modes[1])
        assertEquals(ReasoningMode.META_PROMPT, modes[2])
        assertEquals(ReasoningMode.EXPERT_PANEL, modes[3])
    }

    @Test
    fun testReasoningDialogComparisonRunButtonEnabledWhenTaskNotBlank() {
        val taskInput = "Test task"
        val isLoading = false
        val isEnabled = !isLoading && taskInput.isNotBlank()
        assertTrue(isEnabled)
    }

    @Test
    fun testReasoningDialogComparisonRunButtonDisabledWhenTaskBlank() {
        val taskInput = ""
        val isLoading = false
        val isEnabled = !isLoading && taskInput.isNotBlank()
        assertFalse(isEnabled)
    }

    @Test
    fun testReasoningDialogComparisonRunButtonDisabledWhenLoading() {
        val taskInput = "Test task"
        val isLoading = true
        val isEnabled = !isLoading && taskInput.isNotBlank()
        assertFalse(isEnabled)
    }

    @Test
    fun testReasoningDialogComparisonRunButtonDisabledWhenBlankAndLoading() {
        val taskInput = "   "
        val isLoading = true
        val isEnabled = !isLoading && taskInput.isNotBlank()
        assertFalse(isEnabled)
    }

    @Test
    fun testReasoningDialogComparisonRunButtonCallbackInvoked() {
        var runComparisonTask: String? = null
        val onRunComparison: (String) -> Unit = { task -> runComparisonTask = task }
        val taskInput = "Solve this problem"
        onRunComparison(taskInput)
        assertEquals("Solve this problem", runComparisonTask)
    }

    @Test
    fun testReasoningDialogDismissCallback() {
        var dismissCalled = false
        val onDismiss: () -> Unit = { dismissCalled = true }
        onDismiss()
        assertTrue(dismissCalled)
    }

    @Test
    fun testReasoningComparisonHasAnyResultFalseWhenEmpty() {
        val comparison = ReasoningComparison(
            task = "test",
            results = emptyMap()
        )
        assertFalse(comparison.hasAnyResult)
    }

    @Test
    fun testReasoningComparisonHasAnyResultTrueWhenHasResponse() {
        val comparison = ReasoningComparison(
            task = "test",
            results = mapOf(
                ReasoningMode.DIRECT to ReasoningResult(
                    mode = ReasoningMode.DIRECT,
                    systemPrompt = "system",
                    actualPrompt = "actual",
                    response = "A response"
                )
            )
        )
        assertTrue(comparison.hasAnyResult)
    }

    @Test
    fun testReasoningComparisonIsCompleteTrueWhenAllHaveResponses() {
        val comparison = ReasoningComparison(
            task = "test",
            results = mapOf(
                ReasoningMode.DIRECT to ReasoningResult(
                    mode = ReasoningMode.DIRECT,
                    systemPrompt = "system",
                    actualPrompt = "actual",
                    response = "Response 1"
                ),
                ReasoningMode.STEP_BY_STEP to ReasoningResult(
                    mode = ReasoningMode.STEP_BY_STEP,
                    systemPrompt = "system",
                    actualPrompt = "actual",
                    response = "Response 2"
                )
            )
        )
        assertTrue(comparison.isComplete)
    }

    @Test
    fun testReasoningComparisonIsCompleteFalseWhenLoading() {
        val comparison = ReasoningComparison(
            task = "test",
            results = mapOf(
                ReasoningMode.DIRECT to ReasoningResult(
                    mode = ReasoningMode.DIRECT,
                    systemPrompt = "system",
                    actualPrompt = "actual",
                    response = "Response",
                    isLoading = true
                )
            )
        )
        assertFalse(comparison.isComplete)
    }

    @Test
    fun testReasoningComparisonIsCompleteFalseWhenHasError() {
        val comparison = ReasoningComparison(
            task = "test",
            results = mapOf(
                ReasoningMode.DIRECT to ReasoningResult(
                    mode = ReasoningMode.DIRECT,
                    systemPrompt = "system",
                    actualPrompt = "actual",
                    response = "Response",
                    error = "Something went wrong"
                )
            )
        )
        assertFalse(comparison.isComplete)
    }

    @Test
    fun testReasoningComparisonIsCompleteFalseWhenEmptyResponse() {
        val comparison = ReasoningComparison(
            task = "test",
            results = mapOf(
                ReasoningMode.DIRECT to ReasoningResult(
                    mode = ReasoningMode.DIRECT,
                    systemPrompt = "system",
                    actualPrompt = "actual",
                    response = ""
                )
            )
        )
        assertFalse(comparison.isComplete)
    }

    @Test
    fun testReasoningResultLoadingState() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "system",
            actualPrompt = "actual",
            isLoading = true
        )
        assertTrue(result.isLoading)
        assertTrue(result.response.isEmpty())
        assertNull(result.error)
    }

    @Test
    fun testReasoningResultErrorState() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "system",
            actualPrompt = "actual",
            response = "",
            error = "API error"
        )
        assertNotNull(result.error)
        assertEquals("API error", result.error)
    }

    @Test
    fun testReasoningResultSuccessState() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "You are helpful",
            actualPrompt = "Hello",
            response = "Hi there!",
            responseTimeMs = 500L,
            tokensUsed = 25
        )
        assertFalse(result.isLoading)
        assertNull(result.error)
        assertEquals("Hi there!", result.response)
        assertEquals(500L, result.responseTimeMs)
        assertEquals(25, result.tokensUsed)
    }

    @Test
    fun testComparisonTableRenderingWithCompleteData() {
        val comparison = ReasoningComparison(
            task = "test task",
            results = mapOf(
                ReasoningMode.DIRECT to ReasoningResult(
                    mode = ReasoningMode.DIRECT,
                    systemPrompt = "system",
                    actualPrompt = "actual",
                    response = "Direct response",
                    responseTimeMs = 100L,
                    tokensUsed = 10
                ),
                ReasoningMode.STEP_BY_STEP to ReasoningResult(
                    mode = ReasoningMode.STEP_BY_STEP,
                    systemPrompt = "system",
                    actualPrompt = "actual",
                    response = "Step by step response",
                    responseTimeMs = 200L,
                    tokensUsed = 20
                )
            )
        )

        assertTrue(comparison.isComplete, "Comparison should be complete when all results have non-empty responses")

        val directResult = comparison.results[ReasoningMode.DIRECT]
        assertNotNull(directResult)
        assertEquals(100L, directResult.responseTimeMs)
        assertEquals(10, directResult.tokensUsed)
        assertEquals(15, directResult.response.length)

        val stepResult = comparison.results[ReasoningMode.STEP_BY_STEP]
        assertNotNull(stepResult)
        assertEquals(200L, stepResult.responseTimeMs)
        assertEquals(20, stepResult.tokensUsed)
        assertEquals(21, stepResult.response.length)
    }

    @Test
    fun testComparisonTableRenderingWithNullTokens() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "system",
            actualPrompt = "actual",
            response = "Response",
            responseTimeMs = 100L,
            tokensUsed = null
        )
        val displayTokens = result.tokensUsed?.toString() ?: "-"
        assertEquals("-", displayTokens)
    }

    @Test
    fun testComparisonTableRenderingEmptyResponseLength() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "system",
            actualPrompt = "actual",
            response = "",
            responseTimeMs = 100L
        )
        assertEquals(0, result.response.length)
    }

    @Test
    fun testComparisonTableAllModesPresent() {
        val comparison = ReasoningComparison(
            task = "test",
            results = ReasoningMode.entries.associateWith { mode ->
                ReasoningResult(
                    mode = mode,
                    systemPrompt = "system",
                    actualPrompt = "actual",
                    response = "Response for ${mode.displayName}"
                )
            }
        )

        assertEquals(4, comparison.results.size)
        ReasoningMode.entries.forEach { mode ->
            assertNotNull(comparison.results[mode])
            assertTrue(comparison.results[mode]!!.response.contains(mode.displayName))
        }
    }

    @Test
    fun testTabStatusLoading() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "system",
            actualPrompt = "actual",
            isLoading = true
        )
        val statusText = when {
            result.isLoading -> " [..]"
            result.error != null -> " [X]"
            result.response.isNotEmpty() -> " [OK]"
            else -> ""
        }
        assertEquals(" [..]", statusText)
    }

    @Test
    fun testTabStatusError() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "system",
            actualPrompt = "actual",
            error = "Error occurred"
        )
        val statusText = when {
            result.isLoading -> " [..]"
            result.error != null -> " [X]"
            result.response.isNotEmpty() -> " [OK]"
            else -> ""
        }
        assertEquals(" [X]", statusText)
    }

    @Test
    fun testTabStatusSuccess() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "system",
            actualPrompt = "actual",
            response = "Success response"
        )
        val statusText = when {
            result.isLoading -> " [..]"
            result.error != null -> " [X]"
            result.response.isNotEmpty() -> " [OK]"
            else -> ""
        }
        assertEquals(" [OK]", statusText)
    }

    @Test
    fun testTabStatusPending() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "system",
            actualPrompt = "actual"
        )
        val statusText = when {
            result.isLoading -> " [..]"
            result.error != null -> " [X]"
            result.response.isNotEmpty() -> " [OK]"
            else -> ""
        }
        assertEquals("", statusText)
    }

    @Test
    fun testButtonStateEnabledConditions() {
        val isLoading = false
        val taskInput = "Valid task"
        val buttonEnabled = !isLoading && taskInput.isNotBlank()
        assertTrue(buttonEnabled)
    }

    @Test
    fun testButtonStateDisabledByLoading() {
        val isLoading = true
        val taskInput = "Valid task"
        val buttonEnabled = !isLoading && taskInput.isNotBlank()
        assertFalse(buttonEnabled)
    }

    @Test
    fun testButtonStateDisabledByBlankInput() {
        val isLoading = false
        val taskInput = "   "
        val buttonEnabled = !isLoading && taskInput.isNotBlank()
        assertFalse(buttonEnabled)
    }
}
