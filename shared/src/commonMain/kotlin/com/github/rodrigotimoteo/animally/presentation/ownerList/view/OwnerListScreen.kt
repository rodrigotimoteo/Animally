package com.github.rodrigotimoteo.animally.presentation.ownerList.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.presentation.common.glass.GlassTopAppBar
import com.github.rodrigotimoteo.animally.presentation.common.glass.LocalHazeState
import com.github.rodrigotimoteo.animally.presentation.common.glass.hazeSourceFrom
import com.github.rodrigotimoteo.animally.presentation.common.glass.rememberHazeState
import com.github.rodrigotimoteo.animally.presentation.common.layout.WindowSizeClass
import com.github.rodrigotimoteo.animally.presentation.common.layout.withWindowSizeClass
import com.github.rodrigotimoteo.animally.presentation.common.state.EmptyState
import com.github.rodrigotimoteo.animally.presentation.common.state.ErrorState
import com.github.rodrigotimoteo.animally.presentation.common.state.LoadingState
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
                    title = { Text("Owners") },
                    hazeState = hazeState,
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = viewModel::onAddClick,
                    modifier = Modifier.semantics { contentDescription = "Add owner" },
                ) {
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
}

@Composable
private fun OwnerListContent(
    uiState: OwnerListUiState,
    modifier: Modifier,
    onAddClick: () -> Unit,
    onOwnerClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
    when {
        uiState.isLoading -> LoadingState(modifier = modifier)
        uiState.errorMessage != null && uiState.owners.isEmpty() ->
            ErrorState(
                message = uiState.errorMessage.orEmpty(),
                onRetry = { onAddClick() },
                modifier = modifier,
            )
        uiState.owners.isEmpty() ->
            EmptyState(
                title = "No owners yet",
                message = "Add an owner to link patients to their people.",
                symbol = "👤",
                onActionLabel = "Add owner",
                onAction = onAddClick,
                modifier = modifier,
            )
        else ->
            withWindowSizeClass { sizeClass ->
                OwnerList(
                    owners = uiState.owners,
                    sizeClass = sizeClass,
                    modifier = modifier,
                    onOwnerClick = onOwnerClick,
                    onDeleteClick = onDeleteClick,
                )
            }
    }
}

@Composable
private fun OwnerList(
    owners: List<Owner>,
    sizeClass: WindowSizeClass,
    modifier: Modifier,
    onOwnerClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
    val listModifier = modifier.fillMaxSize().hazeSourceFrom(LocalHazeState.current)
    when (sizeClass) {
        WindowSizeClass.Compact, WindowSizeClass.Medium ->
            LazyColumn(listModifier) {
                items(owners, key = { it.id }) { owner ->
                    OwnerCard(owner, onOwnerClick, onDeleteClick)
                }
            }
        WindowSizeClass.Expanded ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = listModifier,
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(owners, key = { it.id }) { owner ->
                    OwnerCard(owner, onOwnerClick, onDeleteClick)
                }
            }
    }
}

@Composable
private fun OwnerCard(
    owner: Owner,
    onOwnerClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
    val cardModifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Owner ${owner.name}"
            }
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
            Button(
                onClick = { onDeleteClick(owner.id) },
                modifier = Modifier.semantics { contentDescription = "Delete ${owner.name}" },
            ) {
                Text("Delete")
            }
        }
    }
}
