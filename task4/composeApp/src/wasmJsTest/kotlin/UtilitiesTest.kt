import kotlinx.coroutines.flow.MutableStateFlow
import model.ChatMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UtilitiesTest {

    private data class StringState(val value: String)
    private data class IntState(val counter: Int)
    private data class BooleanState(val flag: Boolean)
    private data class InnerState(val inner: StringState)
    private data class ListState(val items: List<String>)

    // ==================== MessageHandler.validateInput ====================

    @Test
    fun testValidateInputEmptyString() {
        assertFalse(MessageHandler.validateInput("", false))
    }

    @Test
    fun testValidateInputWhitespaceOnly() {
        assertFalse(MessageHandler.validateInput("   ", false))
        assertFalse(MessageHandler.validateInput("\t\n", false))
    }

    @Test
    fun testValidateInputValidInput() {
        assertTrue(MessageHandler.validateInput("Hello", false))
        assertTrue(MessageHandler.validateInput("  text  ", false))
    }

    @Test
    fun testValidateInputWhileLoading() {
        assertFalse(MessageHandler.validateInput("Hello", true))
        assertFalse(MessageHandler.validateInput("Valid text", true))
    }

    @Test
    fun testValidateInputEmptyWhileLoading() {
        assertFalse(MessageHandler.validateInput("", true))
    }

    @Test
    fun testValidateInputLongInput() {
        val longInput = "a".repeat(10000)
        assertTrue(MessageHandler.validateInput(longInput, false))
    }

    @Test
    fun testValidateInputUnicodeCharacters() {
        assertTrue(MessageHandler.validateInput("Привет мир", false))
        assertTrue(MessageHandler.validateInput("你好世界", false))
        assertTrue(MessageHandler.validateInput("🎉🎊", false))
    }

    // ==================== MessageHandler.formatUserMessage ====================

    @Test
    fun testFormatUserMessageSimple() {
        val message = MessageHandler.formatUserMessage("Hello")
        assertEquals("user", message.role)
        assertEquals("Hello", message.content)
    }

    @Test
    fun testFormatUserMessageTrimsWhitespace() {
        val message = MessageHandler.formatUserMessage("  Hello World  ")
        assertEquals("Hello World", message.content)
    }

    @Test
    fun testFormatUserMessageEmptyString() {
        val message = MessageHandler.formatUserMessage("")
        assertEquals("", message.content)
    }

    @Test
    fun testFormatUserMessageWhitespaceOnly() {
        val message = MessageHandler.formatUserMessage("   ")
        assertEquals("", message.content)
    }

    @Test
    fun testFormatUserMessagePreservesInternalWhitespace() {
        val message = MessageHandler.formatUserMessage("  Hello   World  ")
        assertEquals("Hello   World", message.content)
    }

    @Test
    fun testFormatUserMessageMultiline() {
        val message = MessageHandler.formatUserMessage("  Line1\nLine2\nLine3  ")
        assertEquals("Line1\nLine2\nLine3", message.content)
    }

    @Test
    fun testFormatUserMessageWithTabs() {
        val message = MessageHandler.formatUserMessage("\tHello\t")
        assertEquals("Hello", message.content)
    }

    @Test
    fun testFormatUserMessageLongContent() {
        val longContent = "a".repeat(10000)
        val message = MessageHandler.formatUserMessage(longContent)
        assertEquals(longContent, message.content)
    }

    @Test
    fun testFormatUserMessageReturnsChatMessage() {
        val message = MessageHandler.formatUserMessage("test")
        assertIs<ChatMessage>(message)
    }

    // ==================== MessageHandler.preparePromptForApi ====================

    @Test
    fun testPreparePromptForApiSimple() {
        assertEquals("Hello", MessageHandler.preparePromptForApi("Hello"))
    }

    @Test
    fun testPreparePromptForApiTrimsWhitespace() {
        assertEquals("text", MessageHandler.preparePromptForApi("  text  "))
    }

    @Test
    fun testPreparePromptForApiEmptyString() {
        assertEquals("", MessageHandler.preparePromptForApi(""))
    }

    @Test
    fun testPreparePromptForApiWhitespaceOnly() {
        assertEquals("", MessageHandler.preparePromptForApi("   "))
    }

    @Test
    fun testPreparePromptForApiPreservesInternalWhitespace() {
        assertEquals("Hello   World", MessageHandler.preparePromptForApi("  Hello   World  "))
    }

    @Test
    fun testPreparePromptForApiMultiline() {
        assertEquals("Line1\nLine2", MessageHandler.preparePromptForApi("  Line1\nLine2  "))
    }

    // ==================== MessageHandler.validateAndPrepare ====================

    @Test
    fun testValidateAndPrepareValidInput() {
        val result = MessageHandler.validateAndPrepare("Hello", false)
        assertIs<MessageHandler.ValidationResult.Valid>(result)
        assertEquals("Hello", result.prompt)
        assertEquals("user", result.message.role)
        assertEquals("Hello", result.message.content)
    }

    @Test
    fun testValidateAndPrepareEmptyInput() {
        val result = MessageHandler.validateAndPrepare("", false)
        assertIs<MessageHandler.ValidationResult.Invalid>(result)
    }

    @Test
    fun testValidateAndPrepareWhileLoading() {
        val result = MessageHandler.validateAndPrepare("Hello", true)
        assertIs<MessageHandler.ValidationResult.Invalid>(result)
    }

    @Test
    fun testValidateAndPrepareWhitespaceOnly() {
        val result = MessageHandler.validateAndPrepare("   ", false)
        assertIs<MessageHandler.ValidationResult.Invalid>(result)
    }

    @Test
    fun testValidateAndPrepareTrimsInput() {
        val result = MessageHandler.validateAndPrepare("  Hello  ", false)
        assertIs<MessageHandler.ValidationResult.Valid>(result)
        val validResult = result as MessageHandler.ValidationResult.Valid
        assertEquals("Hello", validResult.prompt)
        assertEquals("Hello", validResult.message.content)
    }

    @Test
    fun testValidateAndPrepareLongInput() {
        val longInput = "a".repeat(5000)
        val result = MessageHandler.validateAndPrepare(longInput, false)
        assertIs<MessageHandler.ValidationResult.Valid>(result)
    }

    @Test
    fun testValidateAndPrepareUnicodeInput() {
        val result = MessageHandler.validateAndPrepare("Привет мир", false)
        assertIs<MessageHandler.ValidationResult.Valid>(result)
    }

    // ==================== StatePropertyDelegate via helper class ====================

    private class TestDelegateHolder(initialState: String) {
        private val _state = MutableStateFlow(initialState)
        val stateValue: String by StatePropertyDelegate(
            _state,
            getter = { it },
            setter = { _, value -> value }
        )

        fun getState(): String = _state.value
    }

    @Test
    fun testStatePropertyDelegateGetValue() {
        val holder = TestDelegateHolder("initial")
        assertEquals("initial", holder.stateValue)
    }

    @Test
    fun testStatePropertyDelegateSetValue() {
        val holder = TestDelegateHolder("initial")
        assertEquals("initial", holder.stateValue)
    }

    // ==================== stateProperty function ====================

    @Test
    fun testStatePropertyFunction() {
        val stateFlow = MutableStateFlow(StringState("test"))
        stateProperty(
            stateFlow,
            getter = { it.value },
            setter = { state, name -> state.copy(value = name) }
        )

        assertEquals("test", stateFlow.value.value)
        stateFlow.value = stateFlow.value.copy(value = "updated")
        assertEquals("updated", stateFlow.value.value)
    }

    // ==================== typedProp extension ====================

    @Test
    fun testTypedPropExtensionFunction() {
        val stateFlow = MutableStateFlow(BooleanState(false))

        assertEquals(false, stateFlow.value.flag)
        stateFlow.value = stateFlow.value.copy(flag = true)
        assertEquals(true, stateFlow.value.flag)
    }

    // ==================== ViewModelStateHolder ====================

    @Test
    fun testViewModelStateHolderInitialState() {
        val holder = ViewModelStateHolder(StringState("initial"))

        assertEquals("initial", holder.state.value)
        assertEquals("initial", holder.uiState.value.value)
    }

    @Test
    fun testViewModelStateHolderUpdateState() {
        val holder = ViewModelStateHolder(StringState("initial"))

        holder.updateState { it.copy(value = "updated") }

        assertEquals("updated", holder.state.value)
    }

    @Test
    fun testViewModelStateHolderUpdateField() {
        val holder = ViewModelStateHolder(IntState(0))

        holder.updateField(
            getter = { it.counter },
            setter = { state, value -> state.copy(counter = value) },
            value = 42
        )

        assertEquals(42, holder.state.counter)
    }

    @Test
    fun testViewModelStateHolderUpdateFieldMultipleTimes() {
        val holder = ViewModelStateHolder(StringState("a"))

        holder.updateField(
            getter = { it.value },
            setter = { state, value -> state.copy(value = value) },
            value = "b"
        )
        holder.updateField(
            getter = { it.value },
            setter = { state, value -> state.copy(value = value) },
            value = "c"
        )

        assertEquals("c", holder.state.value)
    }

    @Test
    fun testViewModelStateHolderCreateProperty() {
        val holder = ViewModelStateHolder(BooleanState(false))

        holder.createProperty(
            getter = { it.flag },
            setter = { state, flag -> state.copy(flag = flag) }
        )

        holder.updateField(
            getter = { it.flag },
            setter = { state, flag -> state.copy(flag = flag) },
            value = true
        )
        assertEquals(true, holder.state.flag)
    }

    @Test
    fun testViewModelStateHolderUiStateIsReadOnly() {
        val holder = ViewModelStateHolder(StringState("initial"))

        val uiState = holder.uiState
        assertEquals("initial", uiState.value.value)

        holder.updateState { it.copy(value = "changed") }
        assertEquals("changed", uiState.value.value)
    }

    // ==================== MutableStateFlow Extension Functions ====================

    @Test
    fun testMutableStateFlowUpdateField() {
        val stateFlow = MutableStateFlow(IntState(0))

        stateFlow.updateField { it.copy(counter = 10) }

        assertEquals(10, stateFlow.value.counter)
    }

    @Test
    fun testMutableStateFlowToggleBoolean() {
        val stateFlow = MutableStateFlow(BooleanState(false))

        stateFlow.toggleBoolean(
            getter = { it.flag },
            setter = { state, enabled -> state.copy(flag = enabled) }
        )

        assertTrue(stateFlow.value.flag)

        stateFlow.toggleBoolean(
            getter = { it.flag },
            setter = { state, enabled -> state.copy(flag = enabled) }
        )

        assertFalse(stateFlow.value.flag)
    }

    @Test
    fun testMutableStateFlowToggleBooleanMultipleToggles() {
        val stateFlow = MutableStateFlow(BooleanState(false))

        repeat(5) {
            stateFlow.toggleBoolean(
                getter = { it.flag },
                setter = { state, flag -> state.copy(flag = flag) }
            )
        }

        assertTrue(stateFlow.value.flag)
    }

    @Test
    fun testMutableStateFlowUpdateNested() {
        val stateFlow = MutableStateFlow(InnerState(StringState("initial")))

        stateFlow.updateNested(
            nestedGetter = { it.inner },
            nestedSetter = { outer, inner -> outer.copy(inner = inner) },
            block = { it.copy(value = "updated") }
        )

        assertEquals("updated", stateFlow.value.inner.value)
    }

    @Test
    fun testMutableStateFlowUpdateNestedPreservesOtherFields() {
        data class ComplexInner(val value: String, val other: Int)
        data class ComplexOuter(val inner: ComplexInner, val extra: String)
        val stateFlow = MutableStateFlow(ComplexOuter(ComplexInner("initial", 42), "extra"))

        stateFlow.updateNested(
            nestedGetter = { it.inner },
            nestedSetter = { outer, inner -> outer.copy(inner = inner) },
            block = { it.copy(value = "updated") }
        )

        assertEquals("updated", stateFlow.value.inner.value)
        assertEquals(42, stateFlow.value.inner.other)
        assertEquals("extra", stateFlow.value.extra)
    }

    @Test
    fun testMutableStateFlowUpdateNestedDeepNesting() {
        data class Level3(val data: Int)
        data class Level2(val level3: Level3)
        data class Level1(val level2: Level2)
        val stateFlow = MutableStateFlow(Level1(Level2(Level3(0))))

        stateFlow.updateNested(
            nestedGetter = { it.level2 },
            nestedSetter = { level1, level2 -> level1.copy(level2 = level2) },
            block = { it.copy(level3 = Level3(99)) }
        )

        assertEquals(99, stateFlow.value.level2.level3.data)
    }

    @Test
    fun testMutableStateFlowUpdateNestedWithList() {
        val stateFlow = MutableStateFlow(ListState(emptyList()))

        stateFlow.updateNested(
            nestedGetter = { it.items },
            nestedSetter = { state, items -> state.copy(items = items) },
            block = { it + "item1" + "item2" }
        )

        assertEquals(listOf("item1", "item2"), stateFlow.value.items)
    }
}
