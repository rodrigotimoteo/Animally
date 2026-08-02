package com.github.rodrigotimoteo.animally.presentation.ownerList.view

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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.presentation.ownerList.OwnerListUiState
import com.github.rodrigotimoteo.animally.presentation.ownerList.OwnerListViewModel

/**
 * Screen displaying the list of owners.
 *
 * @param viewModel The [OwnerListViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerListScreen(
    viewModel: OwnerListViewModel,
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
        topBar = { TopAppBar(title = { Text("Owners") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::onAddClick) {
                Text("+")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        OwnerListContent(
            uiState = uiState,
            modifier = Modifier.padding(innerPadding),
            onAddClick = viewModel::onAddClick,
            onOwnerClick = viewModel::onOwnerClick,
            onDeleteClick = viewModel::onDeleteClick,
        )
    }
}

@Composable
private fun OwnerListContent(
    uiState: OwnerListUiState,
    modifier: Modifier,
    onAddClick: () -> Unit,
    onOwnerClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when {
            uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            uiState.owners.isEmpty() -> EmptyOwners(onAddClick)
            else -> OwnerList(uiState.owners, onOwnerClick, onDeleteClick)
        }
    }
}

@Composable
private fun EmptyOwners(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No owners yet", style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onAddClick, modifier = Modifier.padding(top = 12.dp)) {
            Text("Add owner")
        }
    }
}

@Composable
private fun OwnerList(
    owners: List<Owner>,
    onOwnerClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(owners, key = { it.id }) { owner ->
            OwnerCard(owner, onOwnerClick, onDeleteClick)
        }
    }
}

@Composable
private fun OwnerCard(
    owner: Owner,
    onOwnerClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
    val cardModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    Card(onClick = { onOwnerClick(owner.id) }, modifier = cardModifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(owner.name, style = MaterialTheme.typography.titleMedium)
                owner.phone?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(onClick = { onDeleteClick(owner.id) }) {
                Text("Delete")
            }
        }
    }
}
