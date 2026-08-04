package com.github.rodrigotimoteo.animally.di.infra

import com.github.rodrigotimoteo.animally.bridge.ObjCHidden
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan("com.github.rodrigotimoteo.animally")
@ObjCHidden
class AppModule
