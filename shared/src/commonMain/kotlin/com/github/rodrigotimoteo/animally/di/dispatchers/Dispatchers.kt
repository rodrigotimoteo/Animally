package com.github.rodrigotimoteo.animally.di.dispatchers

import kotlinx.coroutines.CoroutineDispatcher

expect val ioDispatcher: CoroutineDispatcher

expect val mainDispatcher: CoroutineDispatcher

expect val defaultDispatcher: CoroutineDispatcher

/** Named qualifier value for the IO dispatcher. */
const val IO_DISPATCHER = "IO_DISPATCHER"

/** Named qualifier value for the Main dispatcher. */
const val MAIN_DISPATCHER = "MAIN_DISPATCHER"

/** Named qualifier value for the Default dispatcher. */
const val DEFAULT_DISPATCHER = "DEFAULT_DISPATCHER"
