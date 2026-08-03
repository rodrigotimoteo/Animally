package com.github.rodrigotimoteo.animally.presentation.common.state

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Centered loading indicator.
 *
 * Wraps [CircularProgressIndicator] with accessibility semantics so screen readers
 * announce that content is loading.
 *
 * @param modifier Optional modifier.
 * @param loadingDescription Semantic label announced by screen readers.
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    loadingDescription: String = "Loading",
) {
    Box(
        modifier = modifier.fillMaxSize().semantics { contentDescription = loadingDescription },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
