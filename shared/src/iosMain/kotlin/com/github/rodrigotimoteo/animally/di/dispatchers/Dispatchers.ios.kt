package com.github.rodrigotimoteo.animally.di.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Dispatcher for IO operations on iOS */
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default

/** Dispatcher for UI operations on iOS */
actual val mainDispatcher: CoroutineDispatcher = Dispatchers.Main

/** Dispatcher for CPU operations on iOS */
actual val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
