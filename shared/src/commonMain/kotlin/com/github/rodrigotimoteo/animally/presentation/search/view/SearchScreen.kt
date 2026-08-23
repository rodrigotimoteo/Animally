package com.github.rodrigotimoteo.animally.presentation.search.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.presentation.common.glass.GlassTopAppBar
import com.github.rodrigotimoteo.animally.presentation.common.glass.LocalHazeState
import com.github.rodrigotimoteo.animally.presentation.common.glass.hazeSourceFrom
import com.github.rodrigotimoteo.animally.presentation.common.glass.rememberHazeState
import com.github.rodrigotimoteo.animally.presentation.common.state.EmptyState
import com.github.rodrigotimoteo.animally.presentation.common.state.LoadingState
import com.github.rodrigotimoteo.animally.presentation.search.SearchUiState
import com.github.rodrigotimoteo.animally.presentation.search.SearchViewModel
import kotlinx.datetime.LocalDate

/**
 * Screen for searching across all patient records.
 *
 * @param viewModel The [SearchViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val hazeState = rememberHazeState()

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onDismissError()
        }
    }

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Scaffold(
            modifier = modifier,
            topBar = {
                GlassTopAppBar(
                    title = {
                        OutlinedTextField(
                            value = uiState.query,
                            onValueChange = viewModel::onQueryChange,
                            label = { Text("Search") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    hazeState = hazeState,
                    navigationIcon = {
                        TextButton(
                            onClick = viewModel::popBackStack,
                            modifier = Modifier.semantics { contentDescription = "Back" },
                        ) {
                            Text("Back")
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            SearchContent(
                uiState = uiState,
                modifier = Modifier.padding(innerPadding),
                onToggleRecordType = viewModel::toggleRecordType,
                onFromDateChange = viewModel::setFromDate,
                onToDateChange = viewModel::setToDate,
                onResultClick = viewModel::onResultClick,
            )
        }
    }
}

@Composable
private fun SearchContent(
    uiState: SearchUiState,
    modifier: Modifier = Modifier,
    onToggleRecordType: (String) -> Unit,
    onFromDateChange: (LocalDate?) -> Unit,
    onToDateChange: (LocalDate?) -> Unit,
    onResultClick: (Long) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().hazeSourceFrom(LocalHazeState.current)) {
        RecordTypeFilters(
            uiState = uiState,
            onToggleRecordType = onToggleRecordType,
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            DateField(
                label = "From",
                date = uiState.fromDate,
                onChange = onFromDateChange,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            DateField(
                label = "To",
                date = uiState.toDate,
                onChange = onToDateChange,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
        }
        SearchResults(
            uiState = uiState,
            onResultClick = onResultClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RecordTypeFilters(
    uiState: SearchUiState,
    onToggleRecordType: (String) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        uiState.recordTypeOptions.forEach { (recordType, label) ->
            val chipModifier =
                Modifier
                    .padding(end = 8.dp)
                    .semantics { contentDescription = "Filter by $label" }
            FilterChip(
                selected = recordType in uiState.recordTypes,
                onClick = { onToggleRecordType(recordType) },
                label = { Text(label) },
                modifier = chipModifier,
            )
        }
    }
}

@Composable
private fun DateField(
    label: String,
    date: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = date?.toString() ?: "",
        onValueChange = { text -> onChange(runCatching { LocalDate.parse(text.trim()) }.getOrNull()) },
        label = { Text(label) },
        placeholder = { Text("yyyy-mm-dd") },
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun SearchResults(
    uiState: SearchUiState,
    onResultClick: (Long) -> Unit,
    modifier: Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        when {
            uiState.isLoading -> LoadingState()
            uiState.query.trim().length < 2 ->
                EmptyState(
                    title = "Type at least 2 characters to search",
                    symbol = "🔍",
                )
            uiState.results.isEmpty() ->
                EmptyState(
                    title = "No results found",
                    message = "Try a different query or record type.",
                    symbol = "🔍",
                )
            else -> GroupedResults(uiState.results, onResultClick)
        }
    }
}

@Composable
private fun GroupedResults(
    results: List<SearchResult>,
    onResultClick: (Long) -> Unit,
) {
    val grouped = remember(results) { results.groupBy { it.patientId } }
    LazyColumn(Modifier.fillMaxSize()) {
        grouped.forEach { (patientId, patientResults) ->
            item(key = "header-$patientId") {
                PatientHeader(patientResults.first().patientName)
            }
            items(
                items = patientResults,
                key = { result -> "$patientId-${result.recordType}-${result.recordId}" },
            ) { result ->
                SearchResultCard(result = result, onClick = { onResultClick(patientId) })
            }
        }
    }
}

@Composable
private fun PatientHeader(patientName: String) {
    Text(
        text = patientName,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun SearchResultCard(
    result: SearchResult,
    onClick: () -> Unit,
) {
    val cardModifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${recordTypeLabel(result.recordType)} result"
            }
    Card(
        onClick = onClick,
        modifier = cardModifier,
    ) {
        Column(Modifier.padding(16.dp)) {
            val title = listOfNotNull(recordTypeLabel(result.recordType), result.date?.toString()).joinToString(" • ")
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (result.snippet.isNotBlank()) {
                Text(
                    result.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                )
            }
        }
    }
}

private fun recordTypeLabel(recordType: String): String = RecordType.fromWireName(recordType)?.displayName ?: recordType
