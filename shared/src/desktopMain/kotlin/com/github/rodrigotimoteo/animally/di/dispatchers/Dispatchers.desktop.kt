@file:Suppress("ktlint:standard:filename")

package com.github.rodrigotimoteo.animally.di.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Dispatcher for IO operations on Desktop */
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

/**
 * Dispatcher for UI operations on Desktop.
 *
 * kotlinx-coroutines has no [Dispatchers.Main] on the JVM without the Swing
 * dispatcher, which requires an AWT event thread and breaks headless runs, so
 * main work falls back to [Dispatchers.Default].
 */
actual val mainDispatcher: CoroutineDispatcher = Dispatchers.Default

/** Dispatcher for CPU operations on Desktop */
actual val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
