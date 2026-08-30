package com.github.rodrigotimoteo.animally.presentation.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.domain.assistant.model.AssistantChatTurn
import com.github.rodrigotimoteo.animally.domain.assistant.model.AssistantRecordSource
import com.github.rodrigotimoteo.animally.domain.assistant.model.conversationKey
import com.github.rodrigotimoteo.animally.domain.assistant.usecase.GetRecentAssistantChatHistoryUseCase
import com.github.rodrigotimoteo.animally.domain.assistant.usecase.SaveAssistantChatTurnUseCase
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.domain.vetreference.model.VeterinaryWebSource
import com.github.rodrigotimoteo.animally.llm.AssistantStrings
import com.github.rodrigotimoteo.animally.llm.GenerateRagResponseUseCase
import com.github.rodrigotimoteo.animally.llm.LlmAvailability
import com.github.rodrigotimoteo.animally.llm.LlmEngine
import com.github.rodrigotimoteo.animally.llm.RagHistoryEntry
import com.github.rodrigotimoteo.animally.llm.RagStreamEvent
import com.github.rodrigotimoteo.animally.llm.assistantStrings
import com.github.rodrigotimoteo.animally.llm.cloud.EngineSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * One turn of the assistant conversation.
 *
 * @property interrupted True when the generation stream failed mid-emission;
 *   [text] carries the partial reply and the UI offers a retry.
 * @property sources Retrieved records cited in [text] (source-card chips).
 * @property followUps Deterministic follow-up suggestions for this answer.
 * @property source Which engine produced this turn (drives the cloud badge).
 */
data class AssistantChatMessage(
    val role: AssistantChatMessageRole,
    val text: String,
    val interrupted: Boolean = false,
    val sources: List<SearchResult> = emptyList(),
    val webSources: List<VeterinaryWebSource> = emptyList(),
    val followUps: List<String> = emptyList(),
    val source: EngineSource = EngineSource.ON_DEVICE,
) {
    /** Source chips consolidated to one navigable item per patient. */
    val sourceGroups: List<AssistantSourceGroup>
        get() = sourceGroupsForDisplay(sources)
}

/** Author of an [AssistantChatMessage]. */
@kotlinx.serialization.Serializable
enum class AssistantChatMessageRole {
    USER,
    ASSISTANT,
}

/** Observable state of the AI assistant screen. */
data class AssistantUiState(
    val availability: LlmAvailability =
        LlmAvailability.Loading(
            com.github.rodrigotimoteo.animally.llm.EngineType.FOUNDATION_MODELS,
        ),
    val messages: List<AssistantChatMessage> = emptyList(),
    val history: List<AssistantChatTurn> = emptyList(),
    val isGenerating: Boolean = false,
    val error: String? = null,
    val isHistoryLoading: Boolean = true,
    val historyError: String? = null,
)

/** Transform applied to one chat message when updating state immutably. */
private typealias MessageTransform = (AssistantChatMessage) -> AssistantChatMessage

/** Maximum UI publication rate for cumulative provider stream snapshots. */
private const val STREAM_UI_UPDATE_INTERVAL_MILLIS = 80L

private class ReplyAccumulator(
    var reply: String = "",
    var lastPublishedAt: Long = Long.MIN_VALUE,
)

private data class AssistantReplyContext(
    val generateRagResponse: GenerateRagResponseUseCase,
    val state: MutableStateFlow<AssistantUiState>,
    val strings: AssistantStrings,
    val currentSource: () -> EngineSource,
)

private suspend fun streamAssistantReply(
    context: AssistantReplyContext,
    question: String,
    history: List<RagHistoryEntry>,
) {
    val accumulator = ReplyAccumulator()
    try {
        // The collector stays off the iOS main dispatcher. Cloud providers
        // may emit one cumulative snapshot per token; a bounded publish
        // cadence prevents a fast stream from flooding SwiftUI.
        context.generateRagResponse(question, history).collect { event ->
            accumulator.reply = event.replyText(accumulator.reply)
            if (event.shouldPublish(accumulator.lastPublishedAt, context.strings)) {
                context.state.update { current ->
                    applyAssistantEvent(current, event, context.strings, context.currentSource())
                }
                accumulator.lastPublishedAt = Clock.System.now().toEpochMilliseconds()
            }
        }
        context.state.update { current ->
            applyCompletedAssistantReply(
                current,
                accumulator.reply,
                context.strings,
                context.currentSource(),
            )
        }
    } catch (ce: CancellationException) {
        throw ce
    } catch (t: Exception) {
        // Engine failures normally arrive as Interrupted events; this path
        // covers anything thrown outside the stream. A blank reply must never
        // render as an empty bubble.
        context.state.update { current ->
            applyAssistantFailure(
                current,
                accumulator.reply,
                t,
                context.strings,
                context.currentSource(),
            )
        }
    }
}

private fun applyAssistantEvent(
    state: AssistantUiState,
    event: RagStreamEvent,
    i18n: AssistantStrings,
    source: EngineSource,
): AssistantUiState {
    val patched =
        when (event) {
            is RagStreamEvent.Chunk -> state.messages.upsertLast(source) { it.copy(source = source, text = event.text) }
            is RagStreamEvent.Sources -> {
                val citedTypes = event.sources.map(SearchResult::recordType)
                state.messages.upsertLast(source) { message ->
                    message.copy(
                        source = source,
                        sources = event.sources,
                        followUps = FollowUpSuggestions.forCitations(citedTypes, i18n),
                    )
                }
            }
            is RagStreamEvent.WebSources ->
                state.messages.upsertLast(source) { it.copy(source = source, webSources = event.sources) }
            is RagStreamEvent.Interrupted ->
                state.messages.upsertLast(source) {
                    it.copy(source = source, text = event.partialText, interrupted = true)
                }
        }
    return state.copy(messages = patched, error = (event as? RagStreamEvent.Interrupted)?.error ?: state.error)
}

private fun applyCompletedAssistantReply(
    state: AssistantUiState,
    reply: String,
    strings: AssistantStrings,
    source: EngineSource,
): AssistantUiState {
    val completed =
        if (reply.isBlank() || reply == strings.searchingPlaceholder) {
            applyAssistantBlank(state, strings.blankReplyFallback, source)
        } else {
            // A throttled stream may not have published its last cumulative
            // chunk. Commit the exact final reply before persistence.
            state.copy(messages = state.messages.upsertLast(source) { it.copy(source = source, text = reply) })
        }
    return ensureAssistantFollowUps(completed, strings)
}

internal fun applyAssistantFailure(
    state: AssistantUiState,
    reply: String,
    error: Exception,
    strings: AssistantStrings,
    source: EngineSource,
): AssistantUiState {
    val text = reply.ifBlank { strings.blankReplyFallback }
    val message = error.message ?: strings.blankReplyFallback
    // Keep failures thrown outside the typed stream on the same recovery path
    // as RagStreamEvent.Interrupted. This makes provider/network failures
    // retryable in the iOS bubble instead of leaving a dead, dismiss-only turn.
    val patched = state.messages.upsertLast(source) { it.copy(source = source, text = text, interrupted = true) }
    return state.copy(messages = patched, error = message)
}

/**
 * Converts the visible transcript into prompt history without trusting
 * incomplete provider output. Interrupted turns remain persisted for audit and
 * replay in the history screen, but a partial answer must never become context
 * that can make a later model response sound factual.
 */
internal fun assistantRagHistory(messages: List<AssistantChatMessage>): List<RagHistoryEntry> {
    val entries = mutableListOf<RagHistoryEntry>()
    var index = 0
    while (index < messages.lastIndex) {
        val current = messages[index]
        val next = messages[index + 1]
        val isUserAssistantPair =
            current.role == AssistantChatMessageRole.USER &&
                next.role == AssistantChatMessageRole.ASSISTANT
        val isCompleteReply = !next.interrupted && next.text.isNotBlank()
        if (isUserAssistantPair && isCompleteReply) {
            entries.add(RagHistoryEntry(current.text, next.text))
            index += 2
        } else {
            index += 1
        }
    }
    return entries
}

private fun applyAssistantBlank(
    state: AssistantUiState,
    text: String,
    source: EngineSource,
): AssistantUiState {
    val patched = state.messages.upsertLast(source) { it.copy(source = source, text = text) }
    return state.copy(messages = patched)
}

/** Gives deterministic exploration prompts to short/fallback answers too. */
private fun ensureAssistantFollowUps(
    state: AssistantUiState,
    i18n: AssistantStrings,
): AssistantUiState {
    val last = state.messages.lastOrNull()
    if (last?.role != AssistantChatMessageRole.ASSISTANT || last.interrupted || last.followUps.isNotEmpty()) {
        return state
    }
    val followUps = FollowUpSuggestions.forCitations(emptyList(), i18n)
    return state.copy(messages = state.messages.dropLast(1) + last.copy(followUps = followUps))
}

/** Replaces the trailing assistant turn or appends the first assistant turn. */
private fun List<AssistantChatMessage>.upsertLast(
    source: EngineSource,
    transform: MessageTransform,
): List<AssistantChatMessage> =
    when (lastOrNull()?.role) {
        AssistantChatMessageRole.ASSISTANT -> dropLast(1) + transform(last())
        else ->
            this +
                transform(
                    AssistantChatMessage(
                        role = AssistantChatMessageRole.ASSISTANT,
                        text = "",
                        source = source,
                    ),
                )
    }

private fun RagStreamEvent.replyText(previous: String): String =
    when (this) {
        is RagStreamEvent.Chunk -> text
        is RagStreamEvent.Interrupted -> partialText
        is RagStreamEvent.Sources, is RagStreamEvent.WebSources -> previous
    }

private fun RagStreamEvent.shouldPublish(
    lastPublishedAt: Long,
    strings: AssistantStrings,
): Boolean {
    if (this !is RagStreamEvent.Chunk) return true
    if (text == strings.searchingPlaceholder || lastPublishedAt == Long.MIN_VALUE) return true
    return Clock.System.now().toEpochMilliseconds() - lastPublishedAt >= STREAM_UI_UPDATE_INTERVAL_MILLIS
}

/**
 * ViewModel behind the AI assistant screen. Answers free-text questions about patient
 * records via retrieval-augmented generation ([GenerateRagResponseUseCase]) on top of
 * the platform [LlmEngine].
 *
 * Constructed manually by the iOS bridge (not via Koin annotations) following the
 * established store pattern.
 */
@OptIn(ExperimentalUuidApi::class)
class AssistantViewModel(
    private val generateRagResponse: GenerateRagResponseUseCase,
    private val llmEngine: LlmEngine,
    private val getRecentAssistantChatHistory: GetRecentAssistantChatHistoryUseCase,
    private val saveAssistantChatTurn: SaveAssistantChatTurnUseCase,
    private val ioDispatcher: CoroutineDispatcher,
    private val strings: AssistantStrings = assistantStrings(),
    engineSourceEvents: Flow<EngineSource> = emptyFlow(),
    private val isCloudReady: () -> Boolean = { false },
) : ViewModel() {
    private val _uiState = MutableStateFlow(AssistantUiState())

    /** Observable state of the assistant screen. */
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    /**
     * Engine chosen for the CURRENT turn. The routing engine announces its choice
     * before streaming chunks, but through a separate flow — so the value is kept
     * here and stamped onto every message created until the next ask() resets it.
     */
    private var currentTurnSource: EngineSource = EngineSource.ON_DEVICE
    private var currentConversationId: String = Uuid.random().toString()

    init {
        refreshAvailability()
        viewModelScope.launch {
            engineSourceEvents.collect { source ->
                currentTurnSource = source
                if (source == EngineSource.CLOUD) {
                    // Patch any assistant turn already on screen so a late event
                    // still badges it (ordering across flows is not guaranteed).
                    // Do not create a message here: sourceEvents replays the last
                    // routing decision to newly created views, and a replay before
                    // the next question would otherwise create a blank bubble.
                    _uiState.update { state ->
                        if (!state.isGenerating) return@update state
                        when (state.messages.lastOrNull()?.role) {
                            AssistantChatMessageRole.ASSISTANT ->
                                state.copy(messages = state.messages.upsertLast(source) { it.copy(source = source) })
                            AssistantChatMessageRole.USER ->
                                // The source event and the first provider chunk
                                // are independent flows. If the source wins the
                                // race, create the same placeholder that the
                                // first chunk would create so the cloud badge
                                // cannot be lost before the reply is rendered.
                                state.copy(
                                    messages =
                                        state.messages +
                                            AssistantChatMessage(
                                                role = AssistantChatMessageRole.ASSISTANT,
                                                text = strings.searchingPlaceholder,
                                                source = source,
                                            ),
                                )
                            null -> state
                        }
                    }
                }
            }
        }
        loadHistory(populateMessages = true)
    }

    private fun loadHistory(
        showLoading: Boolean = false,
        populateMessages: Boolean = false,
    ) {
        if (showLoading) {
            _uiState.update { it.copy(isHistoryLoading = true, historyError = null) }
        }
        viewModelScope.launch {
            try {
                val history = withContext(ioDispatcher) { getRecentAssistantChatHistory() }
                _uiState.update { state ->
                    val latestConversationId = history.lastOrNull()?.conversationKey()
                    if (populateMessages && state.messages.isEmpty() && latestConversationId != null) {
                        currentConversationId = latestConversationId
                    }
                    state.copy(
                        messages =
                            if (populateMessages && state.messages.isEmpty()) {
                                history
                                    .filter { it.conversationKey() == currentConversationId }
                                    .flatMap { it.toMessages() }
                            } else {
                                state.messages
                            },
                        history = history,
                        isHistoryLoading = false,
                        historyError = null,
                    )
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isHistoryLoading = false,
                        historyError = t.message ?: "Could not load assistant history.",
                    )
                }
            }
        }
    }

    /** Reloads the persisted turn list without replacing an active transcript. */
    fun refreshHistory() {
        if (_uiState.value.isGenerating) return
        loadHistory(showLoading = true)
    }

    /**
     * Starts a blank visible conversation without deleting the bounded history
     * archive. The next request therefore has no previous-turn context, while
     * the history screen remains available for review and reuse.
     */
    fun startNewChat() {
        if (_uiState.value.isGenerating) return
        currentConversationId = Uuid.random().toString()
        _uiState.update { it.copy(messages = emptyList(), error = null) }
    }

    /**
     * Re-checks which engine can serve the assistant (call after settings changes
     * or app resume). On-device availability is the primary signal, but a configured
     * cloud engine (enabled + keyed) also makes the assistant usable — the routing
     * engine falls back to it when the platform LLM is unavailable. The Apple
     * Intelligence warning only shows when NO working engine exists.
     */
    fun refreshAvailability() {
        viewModelScope.launch {
            val fmAvailability = llmEngine.availability()
            val usable = fmAvailability is LlmAvailability.Available || isCloudReady()
            _uiState.update {
                it.copy(availability = if (usable) LlmAvailability.Available else fmAvailability)
            }
        }
    }

    /**
     * Asks [question] against the record corpus. Appends the user turn immediately and
     * streams the assistant reply into state as it arrives. Completed prior turns are
     * passed as conversation history so follow-ups ("How old is she?") keep context.
     */
    fun ask(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty() || _uiState.value.isGenerating || _uiState.value.isHistoryLoading) return

        val history = assistantRagHistory(_uiState.value.messages)
        val conversationId = currentConversationId
        currentTurnSource = EngineSource.ON_DEVICE
        _uiState.update {
            it.copy(
                // Create the response bubble before retrieval starts. This
                // keeps the UI responsive during slow cloud/model turns and
                // gives the stream a stable target for its first event.
                messages =
                    it.messages +
                        AssistantChatMessage(AssistantChatMessageRole.USER, trimmed) +
                        AssistantChatMessage(
                            role = AssistantChatMessageRole.ASSISTANT,
                            text = strings.searchingPlaceholder,
                        ),
                isGenerating = true,
                error = null,
            )
        }

        // Keep retrieval, parsing, provider streaming, and event reduction on
        // the injected background dispatcher. The StateFlow remains safe to
        // observe from SwiftUI, while the iOS main actor only receives the
        // throttled state snapshots below.
        viewModelScope.launch(ioDispatcher) {
            try {
                streamAssistantReply(
                    AssistantReplyContext(
                        generateRagResponse = generateRagResponse,
                        state = _uiState,
                        strings = strings,
                        currentSource = { currentTurnSource },
                    ),
                    question = trimmed,
                    history = history,
                )
                persistLatestTurn(trimmed, conversationId)
            } finally {
                // The provider and persistence layers are both external to the
                // SwiftUI view. If either is cancelled or throws unexpectedly,
                // never leave the input permanently disabled for this VM.
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.trimToHistoryLimit(),
                        isGenerating = false,
                    )
                }
            }
        }
    }

    /** Clears the current error message. */
    fun dismissError() {
        _uiState.update { it.copy(error = null, historyError = null) }
    }

    private suspend fun persistLatestTurn(
        question: String,
        conversationId: String,
    ) {
        val state = _uiState.value
        val assistant = state.messages.lastOrNull()
        if (assistant?.role != AssistantChatMessageRole.ASSISTANT || assistant.text.isBlank()) return

        try {
            val recentHistory =
                withContext(ioDispatcher) {
                    saveAssistantChatTurn(
                        AssistantChatTurn(
                            question = question,
                            answer = assistant.text,
                            source = assistant.source.name,
                            interrupted = assistant.interrupted,
                            createdAt = Clock.System.now(),
                            conversationId = conversationId,
                            webSources = assistant.webSources,
                            recordSources = assistant.sources.map { source -> source.toAssistantRecordSource() },
                        ),
                    )
                    getRecentAssistantChatHistory()
                }
            _uiState.update {
                it.copy(
                    history = recentHistory,
                    historyError = null,
                )
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            _uiState.update {
                it.copy(historyError = t.message ?: "Could not save assistant history.")
            }
        }
    }

    private fun List<AssistantChatMessage>.trimToHistoryLimit(): List<AssistantChatMessage> {
        val maxMessages = SaveAssistantChatTurnUseCase.MAX_HISTORY_TURNS * 2
        if (size <= maxMessages) return this
        val tail = takeLast(maxMessages)
        return tail.dropWhile { it.role == AssistantChatMessageRole.ASSISTANT }
    }

    private fun AssistantChatTurn.toMessages(): List<AssistantChatMessage> {
        val engineSource =
            runCatching { EngineSource.valueOf(source) }
                .getOrDefault(EngineSource.ON_DEVICE)
        return listOf(
            AssistantChatMessage(
                role = AssistantChatMessageRole.USER,
                text = question,
            ),
            AssistantChatMessage(
                role = AssistantChatMessageRole.ASSISTANT,
                text = answer,
                interrupted = interrupted,
                source = engineSource,
                webSources = webSources,
                sources = recordSources.map { source -> source.toSearchResult() },
            ),
        )
    }

    private fun SearchResult.toAssistantRecordSource(): AssistantRecordSource =
        AssistantRecordSource(
            patientId = patientId,
            patientName = patientName,
            recordType = recordType,
            recordId = recordId,
            date = date?.toString(),
        )

    private fun AssistantRecordSource.toSearchResult(): SearchResult =
        SearchResult(
            patientId = patientId,
            patientName = patientName,
            breed = null,
            microchipId = null,
            recordType = recordType,
            recordId = recordId,
            date = date?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() },
            snippet = "",
        )
}
