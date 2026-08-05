package com.github.rodrigotimoteo.animally.presentation.common.state

/**
 * Bundles the error-state callbacks for list screens so *ListContent
 * composables stay under the detekt LongParameterList threshold.
 *
 * @param onRetry Callback invoked when the error-state retry button is pressed.
 * @param onDismiss Callback invoked when the error-state dismiss button is pressed.
 */
data class ListErrorHandlers(
    val onRetry: () -> Unit,
    val onDismiss: () -> Unit,
)
