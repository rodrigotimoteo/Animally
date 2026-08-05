package com.github.rodrigotimoteo.animally.presentation.common.state

/**
 * Bundles the click callbacks for top-level list screens so *ListContent
 * composables stay under the detekt LongParameterList threshold.
 *
 * @param onAddClick Callback invoked when the add button is pressed.
 * @param onItemClick Callback invoked when a list item is pressed.
 * @param onDeleteClick Callback invoked when a list item is deleted.
 */
data class ListActions(
    val onAddClick: () -> Unit,
    val onItemClick: (Long) -> Unit,
    val onDeleteClick: (Long) -> Unit,
)
