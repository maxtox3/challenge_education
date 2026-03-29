package chat

import model.ChatMessage

object MessageHandler {
    fun validateInput(input: String, isLoading: Boolean): Boolean = input.isNotBlank() && !isLoading

    fun formatUserMessage(content: String): ChatMessage = ChatMessage(
        role = "user",
        content = content.trim(),
    )

    fun preparePromptForApi(input: String): String = input.trim()

    fun validateAndPrepare(input: String, isLoading: Boolean,): ValidationResult {
        if (!validateInput(input, isLoading)) {
            return ValidationResult.Invalid
        }
        val prompt = preparePromptForApi(input)
        val message = formatUserMessage(input)
        return ValidationResult.Valid(prompt, message)
    }

    sealed class ValidationResult {
        data object Invalid : ValidationResult()
        data class Valid(val prompt: String, val message: ChatMessage,) : ValidationResult()
    }
}
