package model

/**
 * Sealed class representing streaming events from the API.
 */
sealed class StreamChunk {
    /**
     * Regular content chunk from the assistant.
     */
    data class Content(val text: String) : StreamChunk()

    /**
     * Reasoning/thinking content chunk (for models with thinking mode).
     */
    data class Reasoning(val text: String) : StreamChunk()

    /**
     * Signals the end of the stream.
     */
    data object Done : StreamChunk()
}
