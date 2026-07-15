package com.github.rodrigotimoteo.animally.di.dispatchers

import kotlinx.coroutines.CoroutineDispatcher

expect val ioDispatcher: CoroutineDispatcher

expect val mainDispatcher: CoroutineDispatcher

expect val defaultDispatcher: CoroutineDispatcher
