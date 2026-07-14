package com.github.rodrigotimoteo.animally.di

import org.koin.core.KoinApplication

expect fun initKoin(context: Any? = null): KoinApplication
