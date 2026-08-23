package com.github.rodrigotimoteo.animally.presentation.search

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.domain.search.usecase.SearchUseCase
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigationViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.navigation.Route
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named

/**
 * View model for the global search screen.
 *
 * @param searchUseCase Use case for querying the FTS5 index.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 * @param debounceMillis Debounce window for query changes, in milliseconds.
 */
@KoinViewModel
class SearchViewModel(
    private val searchUseCase: SearchUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
    private val debounceMillis: Long = SEARCH_DEBOUNCE_MS,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    /**
     * Updates the query text and re-runs the search.
     */
    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        search()
    }

    /**
     * Toggles the given [recordType] filter chip.
     */
    fun toggleRecordType(recordType: String) {
        _uiState.update { state ->
            val types = state.recordTypes.toMutableSet()
            if (!types.add(recordType)) {
                types.remove(recordType)
            }
            state.copy(recordTypes = types)
        }
        search()
    }

    /**
     * Updates the optional lower bound for the record date.
     */
    fun setFromDate(date: LocalDate?) {
        _uiState.update { it.copy(fromDate = date) }
        search()
    }

    /**
     * Updates the optional upper bound for the record date.
     */
    fun setToDate(date: LocalDate?) {
        _uiState.update { it.copy(toDate = date) }
        search()
    }

    /**
     * Navigates to the detail screen for the patient with the given [patientId].
     */
    fun onResultClick(patientId: Long) = navigateTo(Route.PatientDetail(patientId))

    /**
     * Clears the current error message.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun search() {
        searchJob?.cancel()
        if (_uiState.value.query
                .trim()
                .length < 2
        ) {
            _uiState.update { it.copy(isLoading = false, results = emptyList()) }
            return
        }
        searchJob =
            viewModelScope.launch {
                delay(debounceMillis)
                val state = _uiState.value
                _uiState.update { it.copy(isLoading = true) }
                runCatching {
                    withContext(ioDispatcher) {
                        searchUseCase(
                            state.query,
                            state.fromDate,
                            state.toDate,
                            state.recordTypes.toList().ifEmpty { null },
                        )
                    }
                }.onSuccess { results ->
                    _uiState.update { it.copy(results = results, isLoading = false) }
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}

/**
 * UI state for the global search screen.
 *
 * @param query The current query text.
 * @param results The current search results.
 * @param isLoading Whether a search is in flight.
 * @param errorMessage Message of the last error, or `null` when none.
 * @param recordTypes The selected record-type filters, empty when all types are included.
 * @param fromDate Optional lower bound for the record date.
 * @param toDate Optional upper bound for the record date.
 */
data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val recordTypes: Set<String> = emptySet(),
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
) {
    /**
     * The record-type filter chips and their display labels.
     */
    val recordTypeOptions: List<Pair<String, String>>
        get() =
            listOf(
                RecordType.Patient,
                RecordType.Consultation,
                RecordType.Medication,
            ).map { it.wireName to it.displayName }
}
