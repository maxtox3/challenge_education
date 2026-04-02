package agent

interface ContextStorage {
    fun load(): AgentContext
    fun save(context: AgentContext): Boolean
    fun clear()
}
