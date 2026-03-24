@file:OptIn(ExperimentalTestApi::class)

package ui

import androidx.compose.ui.test.*
import model.ReasoningComparison
import model.ReasoningMode
import model.ReasoningResult
import ui.components.ReasoningDialog
import ui.components.ReasoningDialogTags
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReasoningDialogUiRealTest {

    private val emptyComparison = ReasoningComparison(
        task = "",
        results = ReasoningMode.entries.associateWith {
            ReasoningResult(
                mode = it,
                systemPrompt = "",
                actualPrompt = "",
            )
        },
    )

    private fun createComparisonWithResult(
        task: String = "Test task",
        mode: ReasoningMode = ReasoningMode.DIRECT,
        response: String = "Test response",
    ): ReasoningComparison = ReasoningComparison(
        task = task,
        results = ReasoningMode.entries.associateWith {
            if (it == mode) {
                ReasoningResult(
                    mode = it,
                    systemPrompt = "System prompt for $it",
                    actualPrompt = "Actual prompt",
                    response = response,
                    responseTimeMs = 100,
                    tokensUsed = 50,
                )
            } else {
                ReasoningResult(
                    mode = it,
                    systemPrompt = "",
                    actualPrompt = "",
                )
            }
        },
    )

    private fun createLoadingComparison(loadingMode: ReasoningMode = ReasoningMode.DIRECT): ReasoningComparison =
        ReasoningComparison(
            task = "Test task",
            results = ReasoningMode.entries.associateWith {
                if (it == loadingMode) {
                    ReasoningResult(
                        mode = it,
                        systemPrompt = "",
                        actualPrompt = "",
                        response = "Loading response",
                        isLoading = true,
                    )
                } else {
                    ReasoningResult(
                        mode = it,
                        systemPrompt = "",
                        actualPrompt = "",
                        response = "Other response",
                    )
                }
            },
        )

    private fun createErrorComparison(
        errorMode: ReasoningMode = ReasoningMode.DIRECT,
        errorMessage: String = "Test error",
    ): ReasoningComparison = ReasoningComparison(
        task = "Test task",
        results = ReasoningMode.entries.associateWith {
            if (it == errorMode) {
                ReasoningResult(
                    mode = it,
                    systemPrompt = "",
                    actualPrompt = "",
                    response = "Partial response",
                    error = errorMessage,
                )
            } else {
                ReasoningResult(
                    mode = it,
                    systemPrompt = "",
                    actualPrompt = "",
                )
            }
        },
    )

    @Test
    fun reasoningDialog_rootExists() = runComposeUiTest {
        setContent {
            ReasoningDialog(
                comparison = emptyComparison,
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithTag(ReasoningDialogTags.ROOT).assertExists()
    }

    @Test
    fun reasoningDialog_dialogSurfaceExists() = runComposeUiTest {
        setContent {
            ReasoningDialog(
                comparison = emptyComparison,
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithTag(ReasoningDialogTags.DIALOG_SURFACE).assertExists()
    }

    @Test
    fun reasoningDialog_titleDisplayed() = runComposeUiTest {
        setContent {
            ReasoningDialog(
                comparison = emptyComparison,
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithTag(ReasoningDialogTags.TITLE).assertExists()
        onNodeWithText("Сравнение способов рассуждения").assertExists()
    }

    @Test
    fun reasoningDialog_closeButtonExists() = runComposeUiTest {
        setContent {
            ReasoningDialog(
                comparison = emptyComparison,
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithTag(ReasoningDialogTags.CLOSE_BUTTON).assertExists()
        onNodeWithText("Закрыть").assertExists()
    }

    @Test
    fun reasoningDialog_closeButtonTriggersCallback() = runComposeUiTest {
        var dismissCalled = false

        setContent {
            ReasoningDialog(
                comparison = emptyComparison,
                isLoading = false,
                onDismiss = { dismissCalled = true },
                onRunComparison = {},
            )
        }

        onNodeWithTag(ReasoningDialogTags.CLOSE_BUTTON).performClick()

        assertTrue(dismissCalled)
    }

    @Test
    fun reasoningDialog_taskInputFieldExists() = runComposeUiTest {
        setContent {
            ReasoningDialog(
                comparison = emptyComparison,
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithTag(ReasoningDialogTags.TASK_INPUT_FIELD).assertExists()
    }

    @Test
    fun reasoningDialog_taskInputDisplaysValue() = runComposeUiTest {
        val comparison = emptyComparison.copy(task = "Initial task")

        setContent {
            ReasoningDialog(
                comparison = comparison,
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithText("Initial task").assertExists()
    }

    @Test
    fun reasoningDialog_runButtonExists() = runComposeUiTest {
        setContent {
            ReasoningDialog(
                comparison = emptyComparison,
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithTag(ReasoningDialogTags.RUN_BUTTON).assertExists()
        onNodeWithText("Запустить сравнение").assertExists()
    }

    @Test
    fun reasoningDialog_runButtonTriggersCallback() = runComposeUiTest {
        var runTask: String? = null

        setContent {
            ReasoningDialog(
                comparison = emptyComparison.copy(task = "Test task"),
                isLoading = false,
                onDismiss = {},
                onRunComparison = { runTask = it },
            )
        }

        onNodeWithTag(ReasoningDialogTags.TASK_INPUT_FIELD).performTextReplacement("New task")
        onNodeWithTag(ReasoningDialogTags.RUN_BUTTON).performClick()

        assertEquals("New task", runTask)
    }

    @Test
    fun reasoningDialog_emptyStateDisplayed() = runComposeUiTest {
        setContent {
            ReasoningDialog(
                comparison = emptyComparison,
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithTag(ReasoningDialogTags.EMPTY_STATE).assertExists()
        onNodeWithText("Введите задачу и нажмите 'Запустить сравнение'").assertExists()
    }

    @Test
    fun reasoningDialog_tabRowDisplayedWhenHasResults() = runComposeUiTest {
        val comparison = createComparisonWithResult()

        setContent {
            ReasoningDialog(
                comparison = comparison,
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithTag(ReasoningDialogTags.TAB_ROW).assertExists()
    }

    @Test
    fun reasoningDialog_contentAreaDisplayedWhenHasResults() = runComposeUiTest {
        val comparison = createComparisonWithResult()

        setContent {
            ReasoningDialog(
                comparison = comparison,
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithTag(ReasoningDialogTags.CONTENT_AREA).assertExists()
    }

    @Test
    fun reasoningDialog_loadingIndicatorDisplayed() = runComposeUiTest {
        val comparison = createLoadingComparison()

        setContent {
            ReasoningDialog(
                comparison = comparison,
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithTag(ReasoningDialogTags.LOADING_INDICATOR).assertExists()
        onNodeWithText("Запрашиваю...").assertExists()
    }

    @Test
    fun reasoningDialog_errorDisplayed() = runComposeUiTest {
        val comparison = createErrorComparison(errorMessage = "Network error")

        setContent {
            ReasoningDialog(
                comparison = comparison,
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithTag(ReasoningDialogTags.ERROR_TEXT).assertExists()
        onNodeWithText("Ошибка").assertExists()
        onNodeWithText("Network error").assertExists()
    }

    @Test
    fun reasoningDialog_responseDisplayed() = runComposeUiTest {
        val comparison = createComparisonWithResult(response = "This is the model response")

        setContent {
            ReasoningDialog(
                comparison = comparison,
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithText("Ответ модели:").assertExists()
        onNodeWithTag(ReasoningDialogTags.CONTENT_AREA).assertExists()
    }

    @Test
    fun reasoningDialog_allTabsDisplayed() = runComposeUiTest {
        val comparison = createComparisonWithResult()

        setContent {
            ReasoningDialog(
                comparison = comparison,
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithText("Прямой ответ", substring = true).assertExists()
        onNodeWithText("Пошагово", substring = true).assertExists()
        onNodeWithText("Мета-промпт", substring = true).assertExists()
        onNodeWithText("Эксперты", substring = true).assertExists()
    }

    @Test
    fun reasoningDialog_comparisonTableNotDisplayedWhenIncomplete() = runComposeUiTest {
        val comparison = createComparisonWithResult()

        setContent {
            ReasoningDialog(
                comparison = comparison,
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithTag(ReasoningDialogTags.COMPARISON_TABLE).assertDoesNotExist()
    }

    @Test
    fun reasoningDialog_comparisonTableDisplayedWhenComplete() = runComposeUiTest {
        val completeResults = ReasoningMode.entries.associateWith {
            ReasoningResult(
                mode = it,
                systemPrompt = "System prompt",
                actualPrompt = "Actual prompt",
                response = "Response for $it",
                responseTimeMs = 100,
                tokensUsed = 50,
            )
        }
        val comparison = ReasoningComparison(task = "Test task", results = completeResults)

        setContent {
            ReasoningDialog(
                comparison = comparison,
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithTag(ReasoningDialogTags.COMPARISON_TABLE).assertExists()
        onNodeWithText("Сводная таблица").assertExists()
    }

    @Test
    fun reasoningDialog_runButtonDisabledWhenLoading() = runComposeUiTest {
        setContent {
            ReasoningDialog(
                comparison = emptyComparison,
                isLoading = true,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithTag(ReasoningDialogTags.RUN_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun reasoningDialog_runButtonDisabledWhenTaskEmpty() = runComposeUiTest {
        setContent {
            ReasoningDialog(
                comparison = emptyComparison.copy(task = ""),
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithTag(ReasoningDialogTags.RUN_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun reasoningDialog_runButtonEnabledWhenTaskNotBlank() = runComposeUiTest {
        setContent {
            ReasoningDialog(
                comparison = emptyComparison.copy(task = "Some task"),
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithTag(ReasoningDialogTags.RUN_BUTTON).assertIsEnabled()
    }

    @Test
    fun reasoningDialog_loadingTextWhenGlobalLoading() = runComposeUiTest {
        setContent {
            ReasoningDialog(
                comparison = emptyComparison,
                isLoading = true,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithText("Выполняю...").assertExists()
    }

    @Test
    fun reasoningDialog_taskInputEditable() = runComposeUiTest {
        var runTask: String? = null

        setContent {
            ReasoningDialog(
                comparison = emptyComparison.copy(task = "Initial"),
                isLoading = false,
                onDismiss = {},
                onRunComparison = { runTask = it },
            )
        }

        onNodeWithTag(ReasoningDialogTags.TASK_INPUT_FIELD).performTextReplacement("Updated task")
        onNodeWithTag(ReasoningDialogTags.RUN_BUTTON).performClick()

        assertEquals("Updated task", runTask)
    }

    @Test
    fun reasoningDialog_systemPromptDisplayed() = runComposeUiTest {
        val comparison = createComparisonWithResult()

        setContent {
            ReasoningDialog(
                comparison = comparison,
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithText("Системный промпт:").assertExists()
        onNodeWithText("System prompt for DIRECT").assertExists()
    }

    @Test
    fun reasoningDialog_metricsDisplayed() = runComposeUiTest {
        val comparison = createComparisonWithResult()

        setContent {
            ReasoningDialog(
                comparison = comparison,
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithText("100ms", substring = true).assertExists()
        onNodeWithText("50", substring = true).assertExists()
    }

    @Test
    fun reasoningDialog_tabStatusIndicators() = runComposeUiTest {
        val comparison = createComparisonWithResult()

        setContent {
            ReasoningDialog(
                comparison = comparison,
                isLoading = false,
                onDismiss = {},
                onRunComparison = {},
            )
        }

        onNodeWithText("Прямой ответ [OK]").assertExists()
    }
}
