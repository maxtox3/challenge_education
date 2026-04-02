package agent

import model.ChatMessage
import model.StreamChunk

fun update(state: AgentState, msg: AgentMsg): Pair<AgentState, AgentCmd?> = when (msg) {
    is AgentMsg.SendMessage -> handleSendMessage(state, msg)
    is AgentMsg.HandleStreamChunk -> handleStreamChunk(state, msg)
    is AgentMsg.HandleApiError -> state.copy(status = AgentStatus.Error(msg.error)) to null
    is AgentMsg.LoadContextFromStorage -> state.copy(status = AgentStatus.Loading) to AgentCmd.LoadContext
    is AgentMsg.ContextLoaded -> state.copy(context = msg.context, status = AgentStatus.Idle) to null
    is AgentMsg.ContextSaved -> state to null
    is AgentMsg.CompressHistory -> handleCompressHistory(state)
    is AgentMsg.HistoryCompressed -> handleHistoryCompressed(state, msg)
    is AgentMsg.ClearContext -> handleClearContext(state)
    is AgentMsg.UpdateConfig -> state.copy(config = msg.config) to null
    is AgentMsg.HandleApiResponse -> handleApiResponse(state, msg)
    is AgentMsg.Ui -> handleUiMessage(state, msg)
}

private fun handleSendMessage(state: AgentState, msg: AgentMsg.SendMessage): Pair<AgentState, AgentCmd?> {
    val userMessage = ChatMessage(
        role = "user",
        content = msg.prompt
    )
    val newMessages = state.messages + userMessage
    val newState = state.copy(
        messages = newMessages,
        status = AgentStatus.Loading
    )
    val cmd = AgentCmd.StreamApi(
        prompt = msg.prompt,
        context = state.context,
        config = state.config
    )
    return newState to cmd
}

private fun handleStreamChunk(state: AgentState, msg: AgentMsg.HandleStreamChunk): Pair<AgentState, AgentCmd?> =
    when (msg.chunk) {
        is StreamChunk.Content -> handleStreamContent(state, msg.chunk)
        is StreamChunk.Done -> handleStreamDone(state)
        is StreamChunk.Reasoning -> state to null
    }

private fun handleStreamContent(state: AgentState, chunk: StreamChunk.Content): Pair<AgentState, AgentCmd?> {
    val lastMessage = state.messages.lastOrNull()
    val isStreamingAssistant = lastMessage?.role == "assistant" && lastMessage.isStreaming

    val updatedMessage = if (isStreamingAssistant) {
        lastMessage.copy(content = lastMessage.content + chunk.text)
    } else {
        ChatMessage(
            role = "assistant",
            content = chunk.text,
            isStreaming = true
        )
    }

    val newMessages = if (isStreamingAssistant) {
        state.messages.dropLast(1) + updatedMessage
    } else {
        state.messages + updatedMessage
    }

    return state.copy(messages = newMessages) to null
}

private fun handleStreamDone(state: AgentState): Pair<AgentState, AgentCmd?> {
    val lastMessage = state.messages.lastOrNull()
    val finalMessage = lastMessage?.copy(isStreaming = false)

    val newMessages = if (lastMessage != null && finalMessage != null) {
        state.messages.dropLast(1) + finalMessage
    } else {
        state.messages
    }

    val userMessage = state.messages.lastOrNull { it.role == "user" }
    val newContext = if (userMessage != null && finalMessage != null) {
        state.context.addExchange(user = userMessage, assistant = finalMessage)
    } else {
        state.context
    }

    val newState = state.copy(
        messages = newMessages,
        context = newContext,
        status = AgentStatus.Idle
    )

    val cmd = if (state.config.enablePersistence) {
        AgentCmd.SaveContext(newContext)
    } else {
        null
    }

    return newState to cmd
}

private fun handleCompressHistory(state: AgentState): Pair<AgentState, AgentCmd?> =
    if (state.messages.size > state.config.keepLastN * 2) {
        state.copy(status = AgentStatus.Loading) to AgentCmd.CompressWithLlm(
            messages = state.messages,
            keepLastN = state.config.keepLastN
        )
    } else {
        state to null
    }

private fun handleHistoryCompressed(state: AgentState, msg: AgentMsg.HistoryCompressed): Pair<AgentState, AgentCmd?> {
    val recentMessages = state.messages.takeLast(state.config.keepLastN)
    val summaryMessage = ChatMessage(
        role = "system",
        content = "[Previous conversation summary]\n${msg.summary}"
    )
    val newContext = state.context.copy(
        messages = listOf(summaryMessage) + recentMessages,
        summary = msg.summary
    )
    return state.copy(context = newContext) to null
}

private fun handleClearContext(state: AgentState): Pair<AgentState, AgentCmd?> = state.copy(
    context = AgentContext(),
    messages = emptyList(),
    metrics = emptyList(),
    metricCounter = 0,
    agentMetrics = AgentMetrics()
) to AgentCmd.ClearStorage

private fun handleApiResponse(state: AgentState, msg: AgentMsg.HandleApiResponse): Pair<AgentState, AgentCmd?> {
    val assistantMessage = ChatMessage(
        role = "assistant",
        content = msg.response,
        tokensUsed = msg.tokens
    )
    val userMessage = state.messages.lastOrNull { it.role == "user" }
    val newMessages = state.messages + assistantMessage
    val newContext = if (userMessage != null) {
        state.context.addExchange(user = userMessage, assistant = assistantMessage)
    } else {
        state.context
    }
    return state.copy(
        messages = newMessages,
        context = newContext,
        status = AgentStatus.Idle
    ) to null
}

private fun handleUiMessage(state: AgentState, msg: AgentMsg.Ui): Pair<AgentState, AgentCmd?> = when (msg) {
    is AgentMsg.Ui.UpdateInputText -> state.copy(inputText = msg.text) to null

    is AgentMsg.Ui.ToggleSettings -> state.copy(showSettings = msg.show) to null

    is AgentMsg.Ui.ToggleMetrics -> state.copy(showMetrics = msg.show) to null

    is AgentMsg.Ui.ToggleReasoning -> state.copy(showReasoning = msg.show) to null

    is AgentMsg.Ui.UpdateReasoningComparison -> state.copy(reasoningComparison = msg.comparison) to null

    is AgentMsg.Ui.SetReasoningLoading -> state.copy(isReasoningLoading = msg.loading) to null

    is AgentMsg.Ui.AddMetric -> state.copy(
        metrics = state.metrics + msg.metric,
        metricCounter = state.metricCounter + 1
    ) to null
}
