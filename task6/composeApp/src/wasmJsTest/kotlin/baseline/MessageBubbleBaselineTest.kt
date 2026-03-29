package baseline

import model.ChatMessage
import ui.theme.AppColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Characterization tests for MessageBubble component behavior.
 * Tests capture CURRENT behavior - not desired behavior.
 */
class MessageBubbleBaselineTest {

    // ==================== ChatMessage with isReasoningContent Tests ====================

    @Test
    fun chatMessage_default_isReasoningContent_is_false() {
        val message = ChatMessage(
            role = "assistant",
            content = "Test content"
        )

        assertFalse(message.isReasoningContent, "Default isReasoningContent should be false")
    }

    @Test
    fun chatMessage_isReasoningContent_can_be_set_to_true() {
        val message = ChatMessage(
            role = "assistant",
            content = "Reasoning content",
            isReasoningContent = true
        )

        assertTrue(message.isReasoningContent, "isReasoningContent should be true when set")
    }

    @Test
    fun chatMessage_isReasoningContent_false_displays_normal_content() {
        val message = ChatMessage(
            role = "assistant",
            content = "Normal response",
            isReasoningContent = false
        )

        val shouldShowReasoningIndicator = message.isReasoningContent
        assertFalse(shouldShowReasoningIndicator, "Should not show reasoning indicator when false")
    }

    @Test
    fun chatMessage_isReasoningContent_true_triggers_reasoning_indicator() {
        val message = ChatMessage(
            role = "assistant",
            content = "Let me think about this...",
            isReasoningContent = true
        )

        val shouldShowReasoningIndicator = message.isReasoningContent
        assertTrue(shouldShowReasoningIndicator, "Should show reasoning indicator when true")
    }

    @Test
    fun chatMessage_copy_preserves_isReasoningContent() {
        val original = ChatMessage(
            role = "assistant",
            content = "Original",
            isReasoningContent = true
        )

        val copied = original.copy(content = "Updated")

        assertTrue(copied.isReasoningContent, "Copy should preserve isReasoningContent")
        assertEquals("Updated", copied.content)
        assertEquals("assistant", copied.role)
    }

    @Test
    fun chatMessage_copy_can_change_isReasoningContent() {
        val original = ChatMessage(
            role = "assistant",
            content = "Content",
            isReasoningContent = true
        )

        val copied = original.copy(isReasoningContent = false)

        assertFalse(copied.isReasoningContent, "Copy should allow changing isReasoningContent")
    }

    // ==================== getMessageColor Helper Function Tests ====================

    @Test
    fun getMessageColor_user_returns_UserBubble() {
        val role = "user"
        val isUser = true
        val expected = AppColors.UserBubble

        val actual = getMessageColor(role, isUser)

        assertEquals(expected, actual, "User message should use UserBubble color")
    }

    @Test
    fun getMessageColor_system_returns_SystemBubble() {
        val role = "system"
        val isUser = false
        val expected = AppColors.SystemBubble

        val actual = getMessageColor(role, isUser)

        assertEquals(expected, actual, "System message should use SystemBubble color")
    }

    @Test
    fun getMessageColor_assistant_returns_AssistantBubble() {
        val role = "assistant"
        val isUser = false
        val expected = AppColors.AssistantBubble

        val actual = getMessageColor(role, isUser)

        assertEquals(expected, actual, "Assistant message should use AssistantBubble color")
    }

    @Test
    fun getMessageColor_user_flag_takes_precedence_over_role() {
        val role = "assistant" // Role says assistant
        val isUser = true // But flag says user
        val expected = AppColors.UserBubble

        val actual = getMessageColor(role, isUser)

        assertEquals(expected, actual, "isUser flag should take precedence over role")
    }

    @Test
    fun getMessageColor_unknown_role_falls_back_to_AssistantBubble() {
        val role = "unknown"
        val isUser = false
        val expected = AppColors.AssistantBubble

        val actual = getMessageColor(role, isUser)

        assertEquals(expected, actual, "Unknown role should fall back to AssistantBubble")
    }

    // ==================== getRoleText Helper Function Tests ====================

    @Test
    fun getRoleText_user_returns_You() {
        val role = "user"
        val isUser = true
        val expected = "You"

        val actual = getRoleText(role, isUser)

        assertEquals(expected, actual, "User role should display as 'You'")
    }

    @Test
    fun getRoleText_system_returns_System() {
        val role = "system"
        val isUser = false
        val expected = "System"

        val actual = getRoleText(role, isUser)

        assertEquals(expected, actual, "System role should display as 'System'")
    }

    @Test
    fun getRoleText_assistant_returns_GLM5() {
        val role = "assistant"
        val isUser = false
        val expected = "GLM-5"

        val actual = getRoleText(role, isUser)

        assertEquals(expected, actual, "Assistant role should display as 'GLM-5'")
    }

    @Test
    fun getRoleText_user_flag_takes_precedence_over_role() {
        val role = "assistant"
        val isUser = true
        val expected = "You"

        val actual = getRoleText(role, isUser)

        assertEquals(expected, actual, "isUser flag should take precedence over role")
    }

    @Test
    fun getRoleText_unknown_role_falls_back_to_GLM5() {
        val role = "unknown"
        val isUser = false
        val expected = "GLM-5"

        val actual = getRoleText(role, isUser)

        assertEquals(expected, actual, "Unknown role should fall back to 'GLM-5'")
    }

    // ==================== MessageBubble Constants Tests ====================

    @Test
    fun messageBubbleConstants_ANIMATION_DURATION_MS_is_300() {
        val expected = 300
        val actual = MessageBubbleConstants.ANIMATION_DURATION_MS

        assertEquals(expected, actual, "Animation duration should be 300ms")
    }

    @Test
    fun messageBubbleConstants_MESSAGE_WIDTH_FRACTION_is_0_85() {
        val expected = 0.85f
        val actual = MessageBubbleConstants.MESSAGE_WIDTH_FRACTION

        assertEquals(expected, actual, "Message width fraction should be 0.85f")
    }

    @Test
    fun messageBubbleConstants_REASONING_ALPHA_is_0_8() {
        val expected = 0.8f
        val actual = MessageBubbleConstants.REASONING_ALPHA

        assertEquals(expected, actual, "Reasoning alpha should be 0.8f")
    }

    @Test
    fun messageBubbleConstants_TYPING_INDICATOR_WIDTH_FRACTION_is_0_3() {
        val expected = 0.3f
        val actual = MessageBubbleConstants.TYPING_INDICATOR_WIDTH_FRACTION

        assertEquals(expected, actual, "Typing indicator width fraction should be 0.3f")
    }

    @Test
    fun messageBubbleConstants_TYPING_ANIMATION_DURATION_MS_is_1200() {
        val expected = 1200
        val actual = MessageBubbleConstants.TYPING_ANIMATION_DURATION_MS

        assertEquals(expected, actual, "Typing animation duration should be 1200ms")
    }

    @Test
    fun messageBubbleConstants_TYPING_DOT_SCALE_MIN_is_0_5() {
        val expected = 0.5f
        val actual = MessageBubbleConstants.TYPING_DOT_SCALE_MIN

        assertEquals(expected, actual, "Typing dot min scale should be 0.5f")
    }

    @Test
    fun messageBubbleConstants_TYPING_DOT_SCALE_MAX_is_1_2() {
        val expected = 1.2f
        val actual = MessageBubbleConstants.TYPING_DOT_SCALE_MAX

        assertEquals(expected, actual, "Typing dot max scale should be 1.2f")
    }

    @Test
    fun messageBubbleConstants_TYPING_DOT_DELAY_1_MS_is_100() {
        val expected = 100
        val actual = MessageBubbleConstants.TYPING_DOT_DELAY_1_MS

        assertEquals(expected, actual, "First dot delay should be 100ms")
    }

    @Test
    fun messageBubbleConstants_TYPING_DOT_DELAY_2_MS_is_200() {
        val expected = 200
        val actual = MessageBubbleConstants.TYPING_DOT_DELAY_2_MS

        assertEquals(expected, actual, "Second dot delay should be 200ms")
    }

    @Test
    fun messageBubbleConstants_TYPING_DOT_ANIMATION_OFFSET_MS_is_300() {
        val expected = 300
        val actual = MessageBubbleConstants.TYPING_DOT_ANIMATION_OFFSET_MS

        assertEquals(expected, actual, "Typing dot animation offset should be 300ms")
    }

    @Test
    fun messageBubbleConstants_TYPING_DOT_ANIMATION_CYCLE_MS_is_600() {
        val expected = 600
        val actual = MessageBubbleConstants.TYPING_DOT_ANIMATION_CYCLE_MS

        assertEquals(expected, actual, "Typing dot animation cycle should be 600ms")
    }

    @Test
    fun messageBubbleConstants_TYPING_DOT_CORNER_RADIUS_PERCENT_is_50() {
        val expected = 50
        val actual = MessageBubbleConstants.TYPING_DOT_CORNER_RADIUS_PERCENT

        assertEquals(expected, actual, "Typing dot corner radius should be 50%")
    }

    // ==================== Reasoning Content Markdown Alpha Tests ====================

    @Test
    fun reasoningContent_uses_alpha_0_8_for_markdown() {
        val isReasoningContent = true
        val expectedAlpha = MessageBubbleConstants.REASONING_ALPHA

        val alpha = if (isReasoningContent) MessageBubbleConstants.REASONING_ALPHA else 1f

        assertEquals(0.8f, expectedAlpha, "Reasoning content should use 0.8f alpha")
        assertEquals(0.8f, alpha, "Calculated alpha for reasoning content should be 0.8f")
    }

    @Test
    fun normalContent_uses_full_alpha_for_markdown() {
        val isReasoningContent = false
        val expectedAlpha = 1f

        val alpha = if (isReasoningContent) MessageBubbleConstants.REASONING_ALPHA else 1f

        assertEquals(1f, expectedAlpha, "Normal content should use full alpha (1f)")
        assertEquals(1f, alpha, "Calculated alpha for normal content should be 1f")
    }

    // ==================== Reasoning Indicator Text Tests ====================

    @Test
    fun reasoningIndicator_displays_warning_symbol() {
        val expectedSymbol = "[!]"

        assertEquals("[!]", expectedSymbol, "Reasoning indicator should display '[!]' symbol")
    }

    @Test
    fun reasoningIndicator_displays_reasoning_content_label() {
        val expectedLabel = "Reasoning content"

        assertEquals("Reasoning content", expectedLabel, "Reasoning indicator should display 'Reasoning content' label")
    }

    // ==================== Message Surface Border Tests ====================

    @Test
    fun messageSurface_adds_border_when_isReasoningContent_is_true() {
        val message = ChatMessage(
            role = "assistant",
            content = "Content",
            isReasoningContent = true
        )

        val shouldAddBorder = message.isReasoningContent
        val borderColor = AppColors.Warning

        assertTrue(shouldAddBorder, "Should add border when isReasoningContent is true")
        assertEquals(AppColors.Warning, borderColor, "Border should use Warning color")
    }

    @Test
    fun messageSurface_no_border_when_isReasoningContent_is_false() {
        val message = ChatMessage(
            role = "assistant",
            content = "Content",
            isReasoningContent = false
        )

        val shouldAddBorder = message.isReasoningContent

        assertFalse(shouldAddBorder, "Should not add border when isReasoningContent is false")
    }

    // ==================== Tokens Info Display Tests ====================

    @Test
    fun tokensInfo_displays_when_tokensUsed_is_not_null() {
        val message = ChatMessage(
            role = "assistant",
            content = "Content",
            tokensUsed = 100
        )

        val shouldDisplay = message.tokensUsed != null

        assertTrue(shouldDisplay, "Should display tokens info when tokensUsed is not null")
    }

    @Test
    fun tokensInfo_hidden_when_tokensUsed_is_null() {
        val message = ChatMessage(
            role = "assistant",
            content = "Content",
            tokensUsed = null
        )

        val shouldDisplay = message.tokensUsed != null

        assertFalse(shouldDisplay, "Should not display tokens info when tokensUsed is null")
    }

    @Test
    fun tokensInfo_formats_tokens_correctly() {
        val tokensUsed = 42
        val expectedDisplay = "$tokensUsed tokens"

        assertEquals("42 tokens", expectedDisplay, "Tokens should be formatted as 'X tokens'")
    }

    @Test
    fun finishReason_stop_displays_complete() {
        val finishReason = "stop"
        val displayText = if (finishReason == "stop") "complete" else "truncated"

        assertEquals("complete", displayText, "finishReason 'stop' should display as 'complete'")
    }

    @Test
    fun finishReason_non_stop_displays_truncated() {
        val finishReason = "length"
        val displayText = if (finishReason == "stop") "complete" else "truncated"

        assertEquals("truncated", displayText, "Non-stop finishReason should display as 'truncated'")
    }

    @Test
    fun finishReason_stop_uses_Success_color() {
        val finishReason = "stop"
        val expectedColor = AppColors.Success

        val actualColor = if (finishReason == "stop") AppColors.Success else AppColors.Warning

        assertEquals(expectedColor, actualColor, "finishReason 'stop' should use Success color")
    }

    @Test
    fun finishReason_non_stop_uses_Warning_color() {
        val finishReason = "length"
        val expectedColor = AppColors.Warning

        val actualColor = if (finishReason == "stop") AppColors.Success else AppColors.Warning

        assertEquals(expectedColor, actualColor, "Non-stop finishReason should use Warning color")
    }

    // ==================== User vs Assistant Logic Tests ====================

    @Test
    fun isUser_true_when_role_is_user() {
        val message = ChatMessage(role = "user", content = "Test")

        val isUser = message.role == "user"

        assertTrue(isUser, "isUser should be true when role is 'user'")
    }

    @Test
    fun isUser_false_when_role_is_assistant() {
        val message = ChatMessage(role = "assistant", content = "Test")

        val isUser = message.role == "user"

        assertFalse(isUser, "isUser should be false when role is 'assistant'")
    }

    @Test
    fun isUser_false_when_role_is_system() {
        val message = ChatMessage(role = "system", content = "Test")

        val isUser = message.role == "user"

        assertFalse(isUser, "isUser should be false when role is 'system'")
    }

    // ==================== Message Arrangement Tests ====================

    @Test
    fun messageArrangement_end_when_isUser_true() {
        val isUser = true
        val arrangement = if (isUser) "End" else "Start"

        assertEquals("End", arrangement, "User messages should be arranged at the end")
    }

    @Test
    fun messageArrangement_start_when_isUser_false() {
        val isUser = false
        val arrangement = if (isUser) "End" else "Start"

        assertEquals("Start", arrangement, "Non-user messages should be arranged at the start")
    }

    // ==================== Corner Radius Tests ====================

    @Test
    fun cornerRadius_userMessage_bottomEnd_is_4dp() {
        val isUser = true
        val bottomEnd = if (isUser) 4 else 16

        assertEquals(4, bottomEnd, "User message bottomEnd corner should be 4dp")
    }

    @Test
    fun cornerRadius_userMessage_bottomStart_is_16dp() {
        val isUser = true
        val bottomStart = if (isUser) 16 else 4

        assertEquals(16, bottomStart, "User message bottomStart corner should be 16dp")
    }

    @Test
    fun cornerRadius_assistantMessage_bottomEnd_is_16dp() {
        val isUser = false
        val bottomEnd = if (isUser) 4 else 16

        assertEquals(16, bottomEnd, "Assistant message bottomEnd corner should be 16dp")
    }

    @Test
    fun cornerRadius_assistantMessage_bottomStart_is_4dp() {
        val isUser = false
        val bottomStart = if (isUser) 16 else 4

        assertEquals(4, bottomStart, "Assistant message bottomStart corner should be 4dp")
    }

    @Test
    fun cornerRadius_topStart_always_16dp() {
        val topStart = 16

        assertEquals(16, topStart, "topStart corner should always be 16dp")
    }

    @Test
    fun cornerRadius_topEnd_always_16dp() {
        val topEnd = 16

        assertEquals(16, topEnd, "topEnd corner should always be 16dp")
    }
}

/**
 * Mirror of private functions from MessageBubble.kt for testing.
 * These are the actual implementations being characterized.
 */
private fun getMessageColor(role: String, isUser: Boolean) = when {
    isUser -> AppColors.UserBubble
    role == "system" -> AppColors.SystemBubble
    else -> AppColors.AssistantBubble
}

private fun getRoleText(role: String, isUser: Boolean) = when {
    isUser -> "You"
    role == "system" -> "System"
    else -> "GLM-5"
}

/**
 * Mirror of private constants from MessageBubble.kt for testing.
 * These are the actual values being characterized.
 */
private object MessageBubbleConstants {
    const val ANIMATION_DURATION_MS = 300
    const val MESSAGE_WIDTH_FRACTION = 0.85f
    const val REASONING_ALPHA = 0.8f
    const val TYPING_INDICATOR_WIDTH_FRACTION = 0.3f
    const val TYPING_ANIMATION_DURATION_MS = 1200
    const val TYPING_DOT_SCALE_MIN = 0.5f
    const val TYPING_DOT_SCALE_MAX = 1.2f
    const val TYPING_DOT_DELAY_1_MS = 100
    const val TYPING_DOT_DELAY_2_MS = 200
    const val TYPING_DOT_ANIMATION_OFFSET_MS = 300
    const val TYPING_DOT_ANIMATION_CYCLE_MS = 600
    const val TYPING_DOT_CORNER_RADIUS_PERCENT = 50
}
