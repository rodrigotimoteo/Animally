package com.github.rodrigotimoteo.animally.di.infra

import org.koin.core.KoinApplication

expect fun initKoin(context: Any? = null): KoinApplication
