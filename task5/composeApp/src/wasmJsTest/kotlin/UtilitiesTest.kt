import chat.MessageHandler
import core.util.StatePropertyDelegate
import core.util.stateProperty
import core.util.toggleBoolean
import core.util.updateField
import core.util.updateNested
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
        assertEquals("Hello", result.prompt)
        assertEquals("Hello", result.message.content)
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
        private val _stateValue = MutableStateFlow(initialState)
        val stateValue: String by StatePropertyDelegate(
            _stateValue,
            getter = { it },
            setter = { _, value -> value }
        )
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
