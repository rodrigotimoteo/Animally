package com.github.rodrigotimoteo.animally.di.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Dispatcher for IO operations on Android */
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

/** Dispatcher for UI operations on Android */
actual val mainDispatcher: CoroutineDispatcher = Dispatchers.Main

/** Dispatcher for CPU operations on Android */
actual val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
