package agent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import model.ChatMessage

class AgentStore(private val llmClient: LlmClient, private val storage: ContextStorage) {
    private var _state: AgentState = AgentState()
    private val _stateFlow = MutableStateFlow(_state)
    val stateFlow: StateFlow<AgentState> = _stateFlow

    val state: AgentState get() = _state

    fun dispatch(msg: AgentMsg) {
        val (newState, cmd) = update(_state, msg)
        _state = newState
        _stateFlow.value = _state

        cmd?.let { executeCmd(it) }
    }

    private fun executeCmd(cmd: AgentCmd) {
        CoroutineScope(Dispatchers.Default).launch {
            when (cmd) {
                is AgentCmd.CallApi -> {
                    val result = llmClient.call(
                        prompt = cmd.prompt,
                        context = cmd.context,
                        config = cmd.config
                    )
                    result.fold(
                        onSuccess = { response ->
                            dispatch(
                                AgentMsg.HandleApiResponse(
                                    response = response.content,
                                    tokens = response.tokensUsed
                                )
                            )
                        },
                        onFailure = { error ->
                            dispatch(AgentMsg.HandleApiError(error.message ?: "Unknown error"))
                        }
                    )
                }

                is AgentCmd.StreamApi -> {
                    llmClient.stream(
                        prompt = cmd.prompt,
                        context = cmd.context,
                        config = cmd.config
                    ).catch { error ->
                        dispatch(AgentMsg.HandleApiError(error.message ?: "Stream error"))
                    }.collect { chunk ->
                        dispatch(AgentMsg.HandleStreamChunk(chunk))
                    }
                }

                is AgentCmd.SaveContext -> {
                    val success = storage.save(cmd.context)
                    dispatch(AgentMsg.ContextSaved(success))
                }

                is AgentCmd.LoadContext -> {
                    val context = storage.load()
                    dispatch(AgentMsg.ContextLoaded(context))
                }

                is AgentCmd.ClearStorage -> {
                    storage.clear()
                }

                is AgentCmd.CompressWithLlm -> {
                    val toCompress = cmd.messages.dropLast(cmd.keepLastN)
                    if (toCompress.isNotEmpty()) {
                        val summary = compressMessages(toCompress)
                        dispatch(AgentMsg.HistoryCompressed(summary))
                    }
                }
            }
        }
    }

    private suspend fun compressMessages(messages: List<ChatMessage>): String {
        val summary = messages.joinToString("\n") { msg ->
            "${msg.role}: ${msg.content}"
        }
        return "[Summary of ${messages.size} messages]\n$summary"
    }

    fun init() {
        dispatch(AgentMsg.LoadContextFromStorage)
    }
}
