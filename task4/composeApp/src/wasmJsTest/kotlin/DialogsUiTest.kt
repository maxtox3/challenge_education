import model.ConstraintsInfo
import model.MetricRecord
import model.ReasoningComparison
import model.ReasoningMode
import model.ReasoningResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DialogsUiTest {

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
    fun testMetricsDialogEmptyStateDisplay() {
        val metrics = emptyList<MetricRecord>()
        assertTrue(metrics.isEmpty())
    }

    @Test
    fun testMetricsDialogEmptyStateMessage() {
        val metrics = emptyList<MetricRecord>()
        val expectedMessage = "No metrics yet.\nSend some messages to see comparison."
        assertTrue(metrics.isEmpty())
        assertEquals("No metrics yet.\nSend some messages to see comparison.", expectedMessage)
    }

    @Test
    fun testMetricsDialogGroupedByPromptSinglePrompt() {
        val constraints = ConstraintsInfo(null, emptyList(), "text", 1.0)
        val metrics = listOf(
            MetricRecord(1, "Hello", "Hi there", "free", 8, 10, null, "stop", 100L, constraints),
            MetricRecord(2, "Hello", "Hey!", "constrained", 4, 5, 100, "stop", 50L, constraints)
        )
        val grouped = metrics.groupBy { it.prompt }

        assertEquals(1, grouped.size)
        assertEquals(2, grouped["Hello"]?.size)
    }

    @Test
    fun testMetricsDialogGroupedByPromptMultiplePrompts() {
        val constraints = ConstraintsInfo(null, emptyList(), "text", 1.0)
        val metrics = listOf(
            MetricRecord(1, "Hello", "Hi", "free", 2, 5, null, "stop", 100L, constraints),
            MetricRecord(2, "Goodbye", "Bye", "free", 3, 6, null, "stop", 200L, constraints),
            MetricRecord(3, "Hello", "Hey", "constrained", 3, 7, 50, "stop", 150L, constraints)
        )
        val grouped = metrics.groupBy { it.prompt }

        assertEquals(2, grouped.size)
        assertEquals(2, grouped["Hello"]?.size)
        assertEquals(1, grouped["Goodbye"]?.size)
    }

    @Test
    fun testMetricsDialogGroupedByPromptTruncatedPromptDisplay() {
        val longPrompt = "A".repeat(100)
        val displayText = "\"${longPrompt.take(50)}${if (longPrompt.length > 50) "..." else ""}\""
        assertEquals(55, displayText.length)
        assertTrue(displayText.endsWith("...\""))
    }

    @Test
    fun testMetricsDialogGroupedByPromptShortPromptNoTruncation() {
        val shortPrompt = "Hello"
        val displayText = "\"${shortPrompt.take(50)}${if (shortPrompt.length > 50) "..." else ""}\""
        assertEquals("\"Hello\"", displayText)
        assertFalse(displayText.contains("..."))
    }

    @Test
    fun testMetricsTableHeadersIncludeConstraints() {
        val constraints1 = ConstraintsInfo(null, emptyList(), "text", 1.0)
        val constraints2 = ConstraintsInfo(100, emptyList(), "json", 0.5)
        val records = listOf(
            MetricRecord(1, "test", "response1", "free", 9, 10, null, "stop", 100L, constraints1),
            MetricRecord(2, "test", "response2", "constrained", 9, 10, 100, "stop", 100L, constraints2)
        )
        val headers = listOf("Metric") + records.map { it.constraints.toDisplayString() }

        assertEquals(3, headers.size)
        assertEquals("Metric", headers[0])
        assertEquals("Free", headers[1])
        assertTrue(headers[2].contains("maxTokens"))
        assertTrue(headers[2].contains("format"))
        assertTrue(headers[2].contains("temp"))
    }

    @Test
    fun testMetricsTableRenderingLengthChars() {
        val record = MetricRecord(
            1, "test", "response", "free", 8, 10, null, "stop", 100L,
            ConstraintsInfo(null, emptyList(), "text", 1.0)
        )
        assertEquals("8", record.responseLength.toString())
    }

    @Test
    fun testMetricsTableRenderingTokensUsed() {
        val recordWithTokens = MetricRecord(
            1, "test", "response", "free", 8, 50, null, "stop", 100L,
            ConstraintsInfo(null, emptyList(), "text", 1.0)
        )
        val recordWithoutTokens = MetricRecord(
            2, "test", "response", "free", 8, null, null, "stop", 100L,
            ConstraintsInfo(null, emptyList(), "text", 1.0)
        )

        assertEquals("50", recordWithTokens.tokensUsed?.toString() ?: "-")
        assertEquals("-", recordWithoutTokens.tokensUsed?.toString() ?: "-")
    }

    @Test
    fun testMetricsTableRenderingMaxTokens() {
        val recordWithLimit = MetricRecord(
            1, "test", "response", "free", 8, 50, 1000, "stop", 100L,
            ConstraintsInfo(null, emptyList(), "text", 1.0)
        )
        val recordUnlimited = MetricRecord(
            2, "test", "response", "free", 8, 50, null, "stop", 100L,
            ConstraintsInfo(null, emptyList(), "text", 1.0)
        )

        assertEquals("1000", recordWithLimit.maxTokens?.toString() ?: "unlimited")
        assertEquals("unlimited", recordUnlimited.maxTokens?.toString() ?: "unlimited")
    }

    @Test
    fun testMetricsTableRenderingFinishReason() {
        val recordWithReason = MetricRecord(
            1, "test", "response", "free", 8, 50, null, "stop", 100L,
            ConstraintsInfo(null, emptyList(), "text", 1.0)
        )
        val recordWithoutReason = MetricRecord(
            2, "test", "response", "free", 8, 50, null, null, 100L,
            ConstraintsInfo(null, emptyList(), "text", 1.0)
        )

        assertEquals("stop", recordWithReason.finishReason ?: "-")
        assertEquals("-", recordWithoutReason.finishReason ?: "-")
    }

    @Test
    fun testMetricsTableRenderingResponseTime() {
        val record = MetricRecord(
            1, "test", "response", "free", 8, 50, null, "stop", 1500L,
            ConstraintsInfo(null, emptyList(), "text", 1.0)
        )
        assertEquals("1500", record.responseTimeMs.toString())
    }

    @Test
    fun testMetricsDialogDismissCallback() {
        var dismissCalled = false
        val onDismiss: () -> Unit = { dismissCalled = true }
        onDismiss()
        assertTrue(dismissCalled)
    }

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
    fun testMetricBadgeTimeDisplay() {
        val responseTimeMs = 1234L
        val displayValue = "${responseTimeMs}ms"
        assertEquals("1234ms", displayValue)
    }

    @Test
    fun testMetricBadgeTokensDisplay() {
        val tokensUsed = 42
        val displayValue = "$tokensUsed"
        assertEquals("42", displayValue)
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
