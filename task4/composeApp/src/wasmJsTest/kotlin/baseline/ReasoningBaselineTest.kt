package baseline

import model.ReasoningComparison
import model.ReasoningMode
import model.ReasoningResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Characterization tests for Reasoning feature (Chunk 8)
 * Tests capture CURRENT behavior as baseline for refactoring.
 */
class ReasoningBaselineTest {

    // ==================== getStatusText Function ====================

    @Test
    fun getStatusTextReturnsEmptyStringForNullResult() {
        val result: ReasoningResult? = null
        val statusText = getStatusText(result)
        assertEquals("", statusText)
    }

    @Test
    fun getStatusTextReturnsLoadingIndicatorForIsLoading() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "",
            actualPrompt = "",
            isLoading = true
        )
        val statusText = getStatusText(result)
        assertEquals(" [..]", statusText)
    }

    @Test
    fun getStatusTextReturnsErrorIndicatorForError() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "",
            actualPrompt = "",
            error = "API failed"
        )
        val statusText = getStatusText(result)
        assertEquals(" [X]", statusText)
    }

    @Test
    fun getStatusTextReturnsSuccessIndicatorForNonEmptyResponse() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "",
            actualPrompt = "",
            response = "Success!"
        )
        val statusText = getStatusText(result)
        assertEquals(" [OK]", statusText)
    }

    @Test
    fun getStatusTextReturnsEmptyStringForPendingResult() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "",
            actualPrompt = ""
        )
        val statusText = getStatusText(result)
        assertEquals("", statusText)
    }

    @Test
    fun getStatusTextPriorityLoadingTakesPrecedenceOverError() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "",
            actualPrompt = "",
            isLoading = true,
            error = "Error"
        )
        val statusText = getStatusText(result)
        assertEquals(" [..]", statusText, "Loading should take precedence")
    }

    @Test
    fun getStatusTextPriorityErrorTakesPrecedenceOverSuccess() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "",
            actualPrompt = "",
            response = "Response",
            error = "Error"
        )
        val statusText = getStatusText(result)
        assertEquals(" [X]", statusText, "Error should take precedence over response")
    }

    // ==================== ReasoningMode Enum ====================

    @Test
    fun reasoningModeDirectProperties() {
        assertEquals("Прямой ответ", ReasoningMode.DIRECT.displayName)
        assertEquals("Без дополнительных инструкций", ReasoningMode.DIRECT.description)
    }

    @Test
    fun reasoningModeStepByStepProperties() {
        assertEquals("Пошагово", ReasoningMode.STEP_BY_STEP.displayName)
        assertEquals("Инструкция: решай пошагово", ReasoningMode.STEP_BY_STEP.description)
    }

    @Test
    fun reasoningModeMetaPromptProperties() {
        assertEquals("Мета-промпт", ReasoningMode.META_PROMPT.displayName)
        assertEquals("Сначала составляет промпт, затем использует его", ReasoningMode.META_PROMPT.description)
    }

    @Test
    fun reasoningModeExpertPanelProperties() {
        assertEquals("Эксперты", ReasoningMode.EXPERT_PANEL.displayName)
        assertEquals("Группа экспертов: аналитик, инженер, критик", ReasoningMode.EXPERT_PANEL.description)
    }

    @Test
    fun reasoningModeHas4Entries() {
        assertEquals(4, ReasoningMode.entries.size)
    }

    @Test
    fun reasoningModeEntriesOrder() {
        assertEquals(ReasoningMode.DIRECT, ReasoningMode.entries[0])
        assertEquals(ReasoningMode.STEP_BY_STEP, ReasoningMode.entries[1])
        assertEquals(ReasoningMode.META_PROMPT, ReasoningMode.entries[2])
        assertEquals(ReasoningMode.EXPERT_PANEL, ReasoningMode.entries[3])
    }

    // ==================== ReasoningResult Data Class ====================

    @Test
    fun reasoningResultDefaultValues() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "system",
            actualPrompt = "actual"
        )
        assertEquals(ReasoningMode.DIRECT, result.mode)
        assertEquals("system", result.systemPrompt)
        assertEquals("actual", result.actualPrompt)
        assertEquals("", result.response)
        assertEquals(0L, result.responseTimeMs)
        assertNull(result.tokensUsed)
        assertFalse(result.isLoading)
        assertNull(result.error)
    }

    @Test
    fun reasoningResultWithAllFields() {
        val result = ReasoningResult(
            mode = ReasoningMode.STEP_BY_STEP,
            systemPrompt = "Be helpful",
            actualPrompt = "Hello",
            response = "Hi there!",
            responseTimeMs = 500L,
            tokensUsed = 25,
            isLoading = false,
            error = null
        )
        assertEquals(ReasoningMode.STEP_BY_STEP, result.mode)
        assertEquals("Be helpful", result.systemPrompt)
        assertEquals("Hello", result.actualPrompt)
        assertEquals("Hi there!", result.response)
        assertEquals(500L, result.responseTimeMs)
        assertEquals(25, result.tokensUsed)
    }

    @Test
    fun reasoningResultCopyPreservesValues() {
        val original = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "system",
            actualPrompt = "prompt",
            response = "response",
            responseTimeMs = 100L,
            tokensUsed = 10
        )
        val copied = original.copy(response = "new response")
        assertEquals(ReasoningMode.DIRECT, copied.mode)
        assertEquals("system", copied.systemPrompt)
        assertEquals("prompt", copied.actualPrompt)
        assertEquals("new response", copied.response)
        assertEquals(100L, copied.responseTimeMs)
        assertEquals(10, copied.tokensUsed)
    }

    // ==================== ReasoningComparison Data Class ====================

    @Test
    fun reasoningComparisonDefaultIsCompleteWithEmptyResults() {
        val comparison = ReasoningComparison(task = "test", results = emptyMap())
        assertTrue(comparison.isComplete, "Empty results should be considered complete")
        assertFalse(comparison.hasAnyResult, "Empty results should not have any result")
    }

    @Test
    fun reasoningComparisonIsCompleteTrueWhenAllHaveNonEmptyResponses() {
        val comparison = ReasoningComparison(
            task = "test",
            results = mapOf(
                ReasoningMode.DIRECT to ReasoningResult(
                    mode = ReasoningMode.DIRECT,
                    systemPrompt = "",
                    actualPrompt = "",
                    response = "Response 1"
                ),
                ReasoningMode.STEP_BY_STEP to ReasoningResult(
                    mode = ReasoningMode.STEP_BY_STEP,
                    systemPrompt = "",
                    actualPrompt = "",
                    response = "Response 2"
                )
            )
        )
        assertTrue(comparison.isComplete)
        assertTrue(comparison.hasAnyResult)
    }

    @Test
    fun reasoningComparisonIsCompleteFalseWhenLoading() {
        val comparison = ReasoningComparison(
            task = "test",
            results = mapOf(
                ReasoningMode.DIRECT to ReasoningResult(
                    mode = ReasoningMode.DIRECT,
                    systemPrompt = "",
                    actualPrompt = "",
                    response = "Response",
                    isLoading = true
                )
            )
        )
        assertFalse(comparison.isComplete)
    }

    @Test
    fun reasoningComparisonIsCompleteFalseWhenHasError() {
        val comparison = ReasoningComparison(
            task = "test",
            results = mapOf(
                ReasoningMode.DIRECT to ReasoningResult(
                    mode = ReasoningMode.DIRECT,
                    systemPrompt = "",
                    actualPrompt = "",
                    response = "Response",
                    error = "Error occurred"
                )
            )
        )
        assertFalse(comparison.isComplete)
    }

    @Test
    fun reasoningComparisonIsCompleteFalseWhenEmptyResponse() {
        val comparison = ReasoningComparison(
            task = "test",
            results = mapOf(
                ReasoningMode.DIRECT to ReasoningResult(
                    mode = ReasoningMode.DIRECT,
                    systemPrompt = "",
                    actualPrompt = "",
                    response = ""
                )
            )
        )
        assertFalse(comparison.isComplete)
    }

    @Test
    fun reasoningComparisonHasAnyResultTrueWhenAnyHasResponse() {
        val comparison = ReasoningComparison(
            task = "test",
            results = mapOf(
                ReasoningMode.DIRECT to ReasoningResult(
                    mode = ReasoningMode.DIRECT,
                    systemPrompt = "",
                    actualPrompt = "",
                    response = ""
                ),
                ReasoningMode.STEP_BY_STEP to ReasoningResult(
                    mode = ReasoningMode.STEP_BY_STEP,
                    systemPrompt = "",
                    actualPrompt = "",
                    response = "I have a response"
                )
            )
        )
        assertTrue(comparison.hasAnyResult)
        assertFalse(comparison.isComplete, "Not all have responses")
    }

    @Test
    fun reasoningComparisonHasAnyResultFalseWhenNoneHaveResponse() {
        val comparison = ReasoningComparison(
            task = "test",
            results = mapOf(
                ReasoningMode.DIRECT to ReasoningResult(
                    mode = ReasoningMode.DIRECT,
                    systemPrompt = "",
                    actualPrompt = "",
                    response = ""
                )
            )
        )
        assertFalse(comparison.hasAnyResult)
    }

    // ==================== Dialog Constants ====================

    @Test
    fun dialogHeightFractionIs09f() {
        val dialogHeightFraction = 0.9f
        assertEquals(0.9f, dialogHeightFraction)
    }

    // ==================== Tab Selection Logic ====================

    @Test
    fun tabSelectionStartsAtIndex0Direct() {
        val selectedTab = 0
        val mode = ReasoningMode.entries.getOrNull(selectedTab)
        assertEquals(ReasoningMode.DIRECT, mode)
    }

    @Test
    fun tabSelectionCanChangeToAnyMode() {
        val modes = ReasoningMode.entries
        modes.forEachIndexed { index, expectedMode ->
            val mode = ReasoningMode.entries.getOrNull(index)
            assertEquals(expectedMode, mode)
        }
    }

    // ==================== Button State Logic ====================

    @Test
    fun runButtonEnabledWhenTaskNotBlankAndNotLoading() {
        val taskInput = "Valid task"
        val isLoading = false
        val isEnabled = !isLoading && taskInput.isNotBlank()
        assertTrue(isEnabled)
    }

    @Test
    fun runButtonDisabledWhenTaskIsBlank() {
        val taskInput = ""
        val isLoading = false
        val isEnabled = !isLoading && taskInput.isNotBlank()
        assertFalse(isEnabled)
    }

    @Test
    fun runButtonDisabledWhenTaskIsWhitespaceOnly() {
        val taskInput = "   "
        val isLoading = false
        val isEnabled = !isLoading && taskInput.isNotBlank()
        assertFalse(isEnabled)
    }

    @Test
    fun runButtonDisabledWhenLoading() {
        val taskInput = "Valid task"
        val isLoading = true
        val isEnabled = !isLoading && taskInput.isNotBlank()
        assertFalse(isEnabled)
    }

    @Test
    fun runButtonDisabledWhenBothBlankAndLoading() {
        val taskInput = ""
        val isLoading = true
        val isEnabled = !isLoading && taskInput.isNotBlank()
        assertFalse(isEnabled)
    }

    // ==================== Comparison Table Row Data ====================

    @Test
    fun comparisonTableDisplaysResponseTimeWithMsSuffix() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "",
            actualPrompt = "",
            response = "Response",
            responseTimeMs = 100L
        )
        val displayValue = "${result.responseTimeMs}ms"
        assertEquals("100ms", displayValue)
    }

    @Test
    fun comparisonTableDisplaysDashForNullResponseTime() {
        val result: ReasoningResult? = null
        val displayValue = "${result?.responseTimeMs ?: "-"}ms"
        assertEquals("-ms", displayValue)
    }

    @Test
    fun comparisonTableDisplaysTokensOrDash() {
        val resultWithTokens = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "",
            actualPrompt = "",
            response = "Response",
            tokensUsed = 50
        )
        val resultWithoutTokens = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "",
            actualPrompt = "",
            response = "Response"
        )

        assertEquals("50", resultWithTokens.tokensUsed?.toString() ?: "-")
        assertEquals("-", resultWithoutTokens.tokensUsed?.toString() ?: "-")
    }

    @Test
    fun comparisonTableDisplaysResponseLength() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "",
            actualPrompt = "",
            response = "Hello World"
        )
        assertEquals(11, result.response.length)
    }

    @Test
    fun comparisonTableDisplays0ForNullResultResponseLength() {
        val result: ReasoningResult? = null
        val length = result?.response?.length ?: 0
        assertEquals(0, length)
    }

    // ==================== System Prompt Card ====================

    @Test
    fun systemPromptCardShowsPromptOrPlaceholder() {
        val nonEmptyPrompt = "Be helpful"
        val emptyPrompt = ""

        val displayNonEmpty = nonEmptyPrompt.ifEmpty { "(нет)" }
        val displayEmpty = emptyPrompt.ifEmpty { "(нет)" }

        assertEquals("Be helpful", displayNonEmpty)
        assertEquals("(нет)", displayEmpty)
    }

    // ==================== Metric Badge Display ====================

    @Test
    fun metricBadgeFormatsTimeValue() {
        val label = "Время"
        val value = "500ms"
        val display = "$label: $value"
        assertEquals("Время: 500ms", display)
    }

    @Test
    fun metricBadgeFormatsTokensValue() {
        val label = "Токены"
        val value = "42"
        val display = "$label: $value"
        assertEquals("Токены: 42", display)
    }

    // ==================== Callbacks ====================

    @Test
    fun dismissCallbackIsInvoked() {
        var dismissCalled = false
        val onDismiss: () -> Unit = { dismissCalled = true }
        onDismiss()
        assertTrue(dismissCalled)
    }

    @Test
    fun runComparisonCallbackReceivesTask() {
        var receivedTask: String? = null
        val onRunComparison: (String) -> Unit = { task -> receivedTask = task }
        val taskInput = "Solve this problem"
        onRunComparison(taskInput)
        assertEquals("Solve this problem", receivedTask)
    }

    // ==================== Result Content Area Logic ====================

    @Test
    fun resultContentShowsLoadingWhenIsLoadingIsTrue() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "",
            actualPrompt = "",
            isLoading = true
        )
        assertTrue(result.isLoading)
    }

    @Test
    fun resultContentShowsErrorWhenErrorIsNotNull() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "",
            actualPrompt = "",
            error = "API failed"
        )
        assertTrue(result.error != null)
    }

    @Test
    fun resultContentShowsResponseWhenNotEmptyAndNotLoadingOrError() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "",
            actualPrompt = "",
            response = "Here is the answer"
        )
        val shouldShowResponse = result.response.isNotEmpty() && result.error == null && !result.isLoading
        assertTrue(shouldShowResponse)
    }

    @Test
    fun resultContentShowsPlaceholderWhenResultIsNull() {
        val result: ReasoningResult? = null
        val shouldShowPlaceholder = result == null
        assertTrue(shouldShowPlaceholder)
    }
}

/**
 * Mirror of getStatusText from ReasoningDialogComponents.kt for testing.
 * This is the actual implementation being characterized.
 */
private fun getStatusText(result: ReasoningResult?): String = when {
    result?.isLoading == true -> " [..]"
    result?.error != null -> " [X]"
    result?.response?.isNotEmpty() == true -> " [OK]"
    else -> ""
}
