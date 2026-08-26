package com.github.rodrigotimoteo.animally.presentation.common.list

import com.github.rodrigotimoteo.animally.presentation.common.state.ListErrorHandlers

/** Bundles callbacks shared by the embedded record-list composables. */
data class RecordListActions(
    val onAddClick: () -> Unit,
    val onItemClick: (Long) -> Unit,
    val displayActions: ListDisplayActions,
    val errorHandlers: ListErrorHandlers,
)
