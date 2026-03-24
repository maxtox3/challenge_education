package model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReasoningModeTest {
    @Test
    fun testReasoningModeDirect() {
        assertEquals("Прямой ответ", ReasoningMode.DIRECT.displayName)
        assertEquals("Без дополнительных инструкций", ReasoningMode.DIRECT.description)
    }

    @Test
    fun testReasoningModeStepByStep() {
        assertEquals("Пошагово", ReasoningMode.STEP_BY_STEP.displayName)
        assertEquals("Инструкция: решай пошагово", ReasoningMode.STEP_BY_STEP.description)
    }

    @Test
    fun testReasoningModeMetaPrompt() {
        assertEquals("Мета-промпт", ReasoningMode.META_PROMPT.displayName)
        assertEquals("Сначала составляет промпт, затем использует его", ReasoningMode.META_PROMPT.description)
    }

    @Test
    fun testReasoningModeExpertPanel() {
        assertEquals("Эксперты", ReasoningMode.EXPERT_PANEL.displayName)
        assertEquals("Группа экспертов: аналитик, инженер, критик", ReasoningMode.EXPERT_PANEL.description)
    }

    @Test
    fun testReasoningModeEntries() {
        assertEquals(4, ReasoningMode.entries.size)
    }

    @Test
    fun testReasoningResultDefaults() {
        val result = ReasoningResult(
            mode = ReasoningMode.DIRECT,
            systemPrompt = "",
            actualPrompt = "test",
        )
        assertEquals(ReasoningMode.DIRECT, result.mode)
        assertEquals("", result.systemPrompt)
        assertEquals("test", result.actualPrompt)
        assertEquals("", result.response)
        assertEquals(0L, result.responseTimeMs)
        assertEquals(null, result.tokensUsed)
        assertFalse(result.isLoading)
        assertEquals(null, result.error)
    }

    @Test
    fun testReasoningResultWithAllFields() {
        val result = ReasoningResult(
            mode = ReasoningMode.STEP_BY_STEP,
            systemPrompt = "system",
            actualPrompt = "task",
            response = "answer",
            responseTimeMs = 1000L,
            tokensUsed = 50,
            isLoading = true,
            error = "error msg",
        )
        assertEquals(ReasoningMode.STEP_BY_STEP, result.mode)
        assertEquals("system", result.systemPrompt)
        assertEquals("task", result.actualPrompt)
        assertEquals("answer", result.response)
        assertEquals(1000L, result.responseTimeMs)
        assertEquals(50, result.tokensUsed)
        assertTrue(result.isLoading)
        assertEquals("error msg", result.error)
    }

    @Test
    fun testReasoningComparisonDefaults() {
        val comparison = ReasoningComparison(task = "test task", results = emptyMap())
        assertEquals("test task", comparison.task)
        assertTrue(comparison.results.isEmpty())
        assertTrue(comparison.isComplete)
        assertFalse(comparison.hasAnyResult)
    }

    @Test
    fun testReasoningComparisonIsCompleteWithLoadingResults() {
        val results = mapOf(
            ReasoningMode.DIRECT to ReasoningResult(
                mode = ReasoningMode.DIRECT,
                systemPrompt = "",
                actualPrompt = "test",
                isLoading = true,
            ),
        )
        val comparison = ReasoningComparison(task = "test", results = results)
        assertFalse(comparison.isComplete)
    }

    @Test
    fun testReasoningComparisonIsCompleteWithErrors() {
        val results = mapOf(
            ReasoningMode.DIRECT to ReasoningResult(
                mode = ReasoningMode.DIRECT,
                systemPrompt = "",
                actualPrompt = "test",
                error = "failed",
            ),
        )
        val comparison = ReasoningComparison(task = "test", results = results)
        assertFalse(comparison.isComplete)
    }

    @Test
    fun testReasoningComparisonIsCompleteWithEmptyResponse() {
        val results = mapOf(
            ReasoningMode.DIRECT to ReasoningResult(
                mode = ReasoningMode.DIRECT,
                systemPrompt = "",
                actualPrompt = "test",
                response = "",
            ),
        )
        val comparison = ReasoningComparison(task = "test", results = results)
        assertFalse(comparison.isComplete)
    }

    @Test
    fun testReasoningComparisonIsCompleteWithSuccessfulResults() {
        val results = mapOf(
            ReasoningMode.DIRECT to ReasoningResult(
                mode = ReasoningMode.DIRECT,
                systemPrompt = "",
                actualPrompt = "test",
                response = "answer",
            ),
        )
        val comparison = ReasoningComparison(task = "test", results = results)
        assertTrue(comparison.isComplete)
        assertTrue(comparison.hasAnyResult)
    }

    @Test
    fun testReasoningComparisonHasAnyResult() {
        val results = mapOf(
            ReasoningMode.DIRECT to ReasoningResult(
                mode = ReasoningMode.DIRECT,
                systemPrompt = "",
                actualPrompt = "test",
                response = "",
            ),
            ReasoningMode.STEP_BY_STEP to ReasoningResult(
                mode = ReasoningMode.STEP_BY_STEP,
                systemPrompt = "",
                actualPrompt = "test",
                response = "answer",
            ),
        )
        val comparison = ReasoningComparison(task = "test", results = results)
        assertTrue(comparison.hasAnyResult)
        assertFalse(comparison.isComplete)
    }
}
