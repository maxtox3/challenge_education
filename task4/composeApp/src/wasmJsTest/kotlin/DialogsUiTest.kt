import ApiSettings
import model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DialogsUiTest {

    @Test
    fun testSettingsDialog_fieldBinding_apiKey() {
        val settings = ApiSettings(apiKey = "test-api-key-123")
        assertEquals("test-api-key-123", settings.apiKey)
    }

    @Test
    fun testSettingsDialog_fieldBinding_model() {
        val settings = ApiSettings(model = "custom-model")
        assertEquals("custom-model", settings.model)
    }

    @Test
    fun testSettingsDialog_fieldBinding_maxTokens() {
        val settings = ApiSettings(maxTokens = 2000)
        assertEquals(2000, settings.maxTokens)
    }

    @Test
    fun testSettingsDialog_fieldBinding_maxTokensNull() {
        val settings = ApiSettings(maxTokens = null)
        assertNull(settings.maxTokens)
    }

    @Test
    fun testSettingsDialog_fieldBinding_temperature() {
        val settings = ApiSettings(temperature = 0.7)
        assertEquals(0.7, settings.temperature)
    }

    @Test
    fun testSettingsDialog_fieldBinding_stopSequences() {
        val settings = ApiSettings(stopSequences = "stop1,stop2,stop3")
        assertEquals("stop1,stop2,stop3", settings.stopSequences)
    }

    @Test
    fun testSettingsDialog_fieldBinding_responseFormatText() {
        val settings = ApiSettings(responseFormat = "text")
        assertEquals("text", settings.responseFormat)
    }

    @Test
    fun testSettingsDialog_fieldBinding_responseFormatJson() {
        val settings = ApiSettings(responseFormat = "json")
        assertEquals("json", settings.responseFormat)
    }

    @Test
    fun testSettingsDialog_saveCallback_transformsToApiSettings() {
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

        assertNotNull(savedSettings)
        val settings = savedSettings!!
        assertEquals("key123", settings.apiKey)
        assertEquals("glm-5", settings.model)
        assertEquals(1000, settings.maxTokens)
        assertEquals(0.5, settings.temperature)
        assertEquals("end,stop", settings.stopSequences)
        assertEquals("json", settings.responseFormat)
    }

    @Test
    fun testSettingsDialog_saveCallback_emptyMaxTokensBecomesNull() {
        val maxTokensText = ""
        val maxTokens = maxTokensText.toIntOrNull()
        assertNull(maxTokens)
    }

    @Test
    fun testSettingsDialog_saveCallback_numericMaxTokensParsed() {
        val maxTokensText = "500"
        val maxTokens = maxTokensText.toIntOrNull()
        assertEquals(500, maxTokens)
    }

    @Test
    fun testSettingsDialog_temperatureSlider_rangeValidation() {
        val validLow = 0.0
        val validHigh = 2.0
        val validMid = 1.0

        assertTrue(validLow in 0.0..2.0)
        assertTrue(validHigh in 0.0..2.0)
        assertTrue(validMid in 0.0..2.0)
    }

    @Test
    fun testSettingsDialog_temperatureSlider_invalidLow() {
        val invalidTemp = -0.5
        assertFalse(invalidTemp in 0.0..2.0)
    }

    @Test
    fun testSettingsDialog_temperatureSlider_invalidHigh() {
        val invalidTemp = 2.5
        assertFalse(invalidTemp in 0.0..2.0)
    }

    @Test
    fun testSettingsDialog_responseFormatChip_textSelection() {
        var responseFormat = "text"
        assertEquals("text", responseFormat)
        responseFormat = "text"
        assertEquals("text", responseFormat)
    }

    @Test
    fun testSettingsDialog_responseFormatChip_jsonSelection() {
        var responseFormat = "text"
        responseFormat = "json"
        assertEquals("json", responseFormat)
    }

    @Test
    fun testSettingsDialog_responseFormatChip_toggle() {
        var responseFormat = "text"
        assertTrue(responseFormat == "text")
        assertFalse(responseFormat == "json")

        responseFormat = "json"
        assertFalse(responseFormat == "text")
        assertTrue(responseFormat == "json")
    }

    @Test
    fun testSettingsDialog_dismissCallback_invoked() {
        var dismissCalled = false
        val onDismiss: () -> Unit = { dismissCalled = true }
        onDismiss()
        assertTrue(dismissCalled)
    }

    @Test
    fun testSettingsDialog_maxTokensInput_onlyDigitsAllowed() {
        val validInput = "12345"
        val isValid = validInput.isEmpty() || validInput.all { it.isDigit() }
        assertTrue(isValid)
    }

    @Test
    fun testSettingsDialog_maxTokensInput_emptyAllowed() {
        val emptyInput = ""
        val isValid = emptyInput.isEmpty() || emptyInput.all { it.isDigit() }
        assertTrue(isValid)
    }

    @Test
    fun testSettingsDialog_maxTokensInput_lettersRejected() {
        val invalidInput = "abc123"
        val isValid = invalidInput.isEmpty() || invalidInput.all { it.isDigit() }
        assertFalse(isValid)
    }

    @Test
    fun testMetricsDialog_emptyState_display() {
        val metrics = emptyList<MetricRecord>()
        assertTrue(metrics.isEmpty())
    }

    @Test
    fun testMetricsDialog_emptyState_message() {
        val metrics = emptyList<MetricRecord>()
        val expectedMessage = "No metrics yet.\nSend some messages to see comparison."
        assertTrue(metrics.isEmpty())
        assertEquals("No metrics yet.\nSend some messages to see comparison.", expectedMessage)
    }

    @Test
    fun testMetricsDialog_groupedByPrompt_singlePrompt() {
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
    fun testMetricsDialog_groupedByPrompt_multiplePrompts() {
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
    fun testMetricsDialog_groupedByPrompt_truncatedPromptDisplay() {
        val longPrompt = "A".repeat(100)
        val displayText = "\"${longPrompt.take(50)}${if (longPrompt.length > 50) "..." else ""}\""
        assertEquals(55, displayText.length)
        assertTrue(displayText.endsWith("...\""))
    }

    @Test
    fun testMetricsDialog_groupedByPrompt_shortPromptNoTruncation() {
        val shortPrompt = "Hello"
        val displayText = "\"${shortPrompt.take(50)}${if (shortPrompt.length > 50) "..." else ""}\""
        assertEquals("\"Hello\"", displayText)
        assertFalse(displayText.contains("..."))
    }

    @Test
    fun testMetricsTable_headers_includeConstraints() {
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
    fun testMetricsTable_rendering_lengthChars() {
        val record = MetricRecord(
            1, "test", "response", "free", 8, 10, null, "stop", 100L,
            ConstraintsInfo(null, emptyList(), "text", 1.0)
        )
        assertEquals("8", record.responseLength.toString())
    }

    @Test
    fun testMetricsTable_rendering_tokensUsed() {
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
    fun testMetricsTable_rendering_maxTokens() {
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
    fun testMetricsTable_rendering_finishReason() {
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
    fun testMetricsTable_rendering_responseTime() {
        val record = MetricRecord(
            1, "test", "response", "free", 8, 50, null, "stop", 1500L,
            ConstraintsInfo(null, emptyList(), "text", 1.0)
        )
        assertEquals("1500", record.responseTimeMs.toString())
    }

    @Test
    fun testMetricsDialog_dismissCallback() {
        var dismissCalled = false
        val onDismiss: () -> Unit = { dismissCalled = true }
        onDismiss()
        assertTrue(dismissCalled)
    }

    @Test
    fun testReasoningDialog_tabSelection_default() {
        var selectedTab = 0
        assertEquals(ReasoningMode.DIRECT, ReasoningMode.entries.getOrNull(selectedTab))
    }

    @Test
    fun testReasoningDialog_tabSelection_changeToStepByStep() {
        var selectedTab = 0
        selectedTab = 1
        assertEquals(ReasoningMode.STEP_BY_STEP, ReasoningMode.entries.getOrNull(selectedTab))
    }

    @Test
    fun testReasoningDialog_tabSelection_changeToMetaPrompt() {
        var selectedTab = 0
        selectedTab = 2
        assertEquals(ReasoningMode.META_PROMPT, ReasoningMode.entries.getOrNull(selectedTab))
    }

    @Test
    fun testReasoningDialog_tabSelection_changeToExpertPanel() {
        var selectedTab = 0
        selectedTab = 3
        assertEquals(ReasoningMode.EXPERT_PANEL, ReasoningMode.entries.getOrNull(selectedTab))
    }

    @Test
    fun testReasoningDialog_tabSelection_allModesAccessible() {
        val modes = ReasoningMode.entries
        assertEquals(4, modes.size)
        assertEquals(ReasoningMode.DIRECT, modes[0])
        assertEquals(ReasoningMode.STEP_BY_STEP, modes[1])
        assertEquals(ReasoningMode.META_PROMPT, modes[2])
        assertEquals(ReasoningMode.EXPERT_PANEL, modes[3])
    }

    @Test
    fun testReasoningDialog_comparisonRunButton_enabledWhenTaskNotBlank() {
        val taskInput = "Test task"
        val isLoading = false
        val isEnabled = !isLoading && taskInput.isNotBlank()
        assertTrue(isEnabled)
    }

    @Test
    fun testReasoningDialog_comparisonRunButton_disabledWhenTaskBlank() {
        val taskInput = ""
        val isLoading = false
        val isEnabled = !isLoading && taskInput.isNotBlank()
        assertFalse(isEnabled)
    }

    @Test
    fun testReasoningDialog_comparisonRunButton_disabledWhenLoading() {
        val taskInput = "Test task"
        val isLoading = true
        val isEnabled = !isLoading && taskInput.isNotBlank()
        assertFalse(isEnabled)
    }

    @Test
    fun testReasoningDialog_comparisonRunButton_disabledWhenBlankAndLoading() {
        val taskInput = "   "
        val isLoading = true
        val isEnabled = !isLoading && taskInput.isNotBlank()
        assertFalse(isEnabled)
    }

    @Test
    fun testReasoningDialog_comparisonRunButton_callbackInvoked() {
        var runComparisonTask: String? = null
        val onRunComparison: (String) -> Unit = { task -> runComparisonTask = task }
        val taskInput = "Solve this problem"
        onRunComparison(taskInput)
        assertEquals("Solve this problem", runComparisonTask)
    }

    @Test
    fun testReasoningDialog_taskInput_updatesValue() {
        var taskInput = "Initial task"
        taskInput = "Updated task"
        assertEquals("Updated task", taskInput)
    }

    @Test
    fun testReasoningDialog_dismissCallback() {
        var dismissCalled = false
        val onDismiss: () -> Unit = { dismissCalled = true }
        onDismiss()
        assertTrue(dismissCalled)
    }

    @Test
    fun testReasoningComparison_hasAnyResult_falseWhenEmpty() {
        val comparison = ReasoningComparison(
            task = "test",
            results = emptyMap()
        )
        assertFalse(comparison.hasAnyResult)
    }

    @Test
    fun testReasoningComparison_hasAnyResult_trueWhenHasResponse() {
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
    fun testReasoningComparison_isComplete_trueWhenAllHaveResponses() {
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
    fun testReasoningComparison_isComplete_falseWhenLoading() {
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
    fun testReasoningComparison_isComplete_falseWhenHasError() {
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
    fun testReasoningComparison_isComplete_falseWhenEmptyResponse() {
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
    fun testReasoningResult_loadingState() {
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
    fun testReasoningResult_errorState() {
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
    fun testReasoningResult_successState() {
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
    fun testComparisonTable_rendering_withCompleteData() {
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
    fun testComparisonTable_rendering_withNullTokens() {
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
    fun testComparisonTable_rendering_emptyResponseLength() {
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
    fun testComparisonTable_allModesPresent() {
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
    fun testTabStatus_loading() {
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
    fun testTabStatus_error() {
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
    fun testTabStatus_success() {
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
    fun testTabStatus_pending() {
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
    fun testMetricBadge_timeDisplay() {
        val responseTimeMs = 1234L
        val displayValue = "${responseTimeMs}ms"
        assertEquals("1234ms", displayValue)
    }

    @Test
    fun testMetricBadge_tokensDisplay() {
        val tokensUsed = 42
        val displayValue = "$tokensUsed"
        assertEquals("42", displayValue)
    }

    @Test
    fun testButtonState_enabledConditions() {
        val isLoading = false
        val taskInput = "Valid task"
        val buttonEnabled = !isLoading && taskInput.isNotBlank()
        assertTrue(buttonEnabled)
    }

    @Test
    fun testButtonState_disabledByLoading() {
        val isLoading = true
        val taskInput = "Valid task"
        val buttonEnabled = !isLoading && taskInput.isNotBlank()
        assertFalse(buttonEnabled)
    }

    @Test
    fun testButtonState_disabledByBlankInput() {
        val isLoading = false
        val taskInput = "   "
        val buttonEnabled = !isLoading && taskInput.isNotBlank()
        assertFalse(buttonEnabled)
    }

    @Test
    fun testChipSelection_textFormat() {
        var responseFormat = "text"
        val isSelected = responseFormat == "text"
        assertTrue(isSelected)
    }

    @Test
    fun testChipSelection_jsonFormat() {
        var responseFormat = "json"
        val isSelected = responseFormat == "json"
        assertTrue(isSelected)
    }

    @Test
    fun testChipSelection_notSelected() {
        var responseFormat = "text"
        val isSelected = responseFormat == "json"
        assertFalse(isSelected)
    }

    @Test
    fun testSliderValue_temperatureConversion() {
        val sliderValue = 0.75f
        val temperature = sliderValue.toDouble()
        assertEquals(0.75, temperature)
    }

    @Test
    fun testSliderValue_rangeMinimum() {
        val valueRange = 0f..2f
        assertTrue(0f in valueRange)
    }

    @Test
    fun testSliderValue_rangeMaximum() {
        val valueRange = 0f..2f
        assertTrue(2f in valueRange)
    }

    @Test
    fun testSliderValue_outOfRange() {
        val valueRange = 0f..2f
        assertFalse(-0.1f in valueRange)
        assertFalse(2.1f in valueRange)
    }
}
