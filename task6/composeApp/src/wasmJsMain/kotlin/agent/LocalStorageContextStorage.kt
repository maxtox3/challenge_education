@file:Suppress("SwallowedException")

package agent

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

external interface Storage {
    fun getItem(key: String): String?
    fun setItem(key: String, value: String)
    fun removeItem(key: String)
}

external val localStorage: Storage

class LocalStorageContextStorage : ContextStorage {
    private val key = "agent_context"
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    override fun load(): AgentContext = try {
        val data = localStorage.getItem(key)
        if (data != null) {
            json.decodeFromString<AgentContext>(data)
        } else {
            AgentContext()
        }
    } catch (e: SerializationException) {
        AgentContext()
    } catch (e: IllegalArgumentException) {
        AgentContext()
    }

    override fun save(context: AgentContext): Boolean = try {
        val data = json.encodeToString(context)
        localStorage.setItem(key, data)
        true
    } catch (e: SerializationException) {
        false
    } catch (e: IllegalArgumentException) {
        false
    }

    override fun clear() {
        localStorage.removeItem(key)
    }
}
