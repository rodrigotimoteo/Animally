package com.github.rodrigotimoteo.animally.presentation.customreminder.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder
import com.github.rodrigotimoteo.animally.presentation.customreminder.CustomReminderListUiState
import com.github.rodrigotimoteo.animally.presentation.customreminder.CustomReminderListViewModel

/**
 * Screen displaying the custom reminders of a patient, split into upcoming and overdue groups.
 *
 * @param viewModel The [CustomReminderListViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomReminderListScreen(
    viewModel: CustomReminderListViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onDismissError()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Reminders") },
                navigationIcon = {
                    TextButton(onClick = viewModel::onBack) {
                        Text("Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::onAddClick) {
                Text("+")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        CustomReminderListContent(
            uiState = uiState,
            modifier = Modifier.padding(innerPadding),
            onEditClick = viewModel::onEditClick,
            onDeleteClick = viewModel::onDeleteClick,
        )
    }
}

@Composable
private fun CustomReminderListContent(
    uiState: CustomReminderListUiState,
    modifier: Modifier,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when {
            uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            uiState.upcoming.isEmpty() && uiState.overdue.isEmpty() -> EmptyReminders()
            else -> ReminderList(uiState, onEditClick, onDeleteClick)
        }
    }
}

@Composable
private fun EmptyReminders() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No reminders yet", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ReminderList(
    uiState: CustomReminderListUiState,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        if (uiState.upcoming.isNotEmpty()) {
            item(key = "header-upcoming") {
                SectionHeader("Upcoming")
            }
            items(uiState.upcoming, key = { it.id }) { reminder ->
                ReminderCard(reminder, onEditClick, onDeleteClick)
            }
        }
        if (uiState.overdue.isNotEmpty()) {
            item(key = "header-overdue") {
                SectionHeader("Overdue")
            }
            items(uiState.overdue, key = { it.id }) { reminder ->
                ReminderCard(reminder, onEditClick, onDeleteClick)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ReminderCard(
    reminder: CustomReminder,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
    val cardModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    Card(onClick = { onEditClick(reminder.id) }, modifier = cardModifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                Text(reminder.dueDate.toString(), style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = { onDeleteClick(reminder.id) }) {
                Text("Delete")
            }
        }
    }
}
