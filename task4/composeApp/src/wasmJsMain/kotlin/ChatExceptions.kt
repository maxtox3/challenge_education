/**
 * Base exception for all chat-related errors.
 */
open class ChatException(message: String, cause: Throwable? = null,) : Exception(message, cause)

/**
 * Network-related errors (connection, timeout, HTTP transport issues).
 */
open class ChatNetworkException(message: String, cause: Throwable? = null,) : ChatException(message, cause)

/**
 * API errors (HTTP error responses, API-specific errors with status codes).
 */
open class ChatApiException(message: String, val code: Int? = null, cause: Throwable? = null,) :
    ChatException(message, cause)

/**
 * Serialization/parsing errors (JSON decode/encode issues).
 */
open class ChatSerializationException(message: String, cause: Throwable? = null,) : ChatException(message, cause)

/**
 * Streaming/SSE errors (connection drops, stream parsing failures).
 */
open class ChatStreamingException(message: String, cause: Throwable? = null,) : ChatException(message, cause)
