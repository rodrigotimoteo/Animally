package com.github.rodrigotimoteo.animally.di.dispatchers

import com.github.rodrigotimoteo.animally.bridge.ObjCHidden
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * Module that provides all the dispatchers based on the platform
 */
@Module
@ObjCHidden
class DispatchersModule {
    @Single
    @Named(IO_DISPATCHER)
    fun provideIoDispatcher() = ioDispatcher

    @Single
    @Named(MAIN_DISPATCHER)
    fun provideMainDispatcher() = mainDispatcher

    @Single
    @Named(DEFAULT_DISPATCHER)
    fun provideDefaultDispatcher() = defaultDispatcher

    companion object {
        /** Dispatcher IO named constant */
        const val IO_DISPATCHER = "IO_DISPATCHER"

        /** Dispatcher MAIN named constant */
        const val MAIN_DISPATCHER = "MAIN_DISPATCHER"

        /** Dispatcher DEFAULT named constant */
        const val DEFAULT_DISPATCHER = "DEFAULT_DISPATCHER"
    }
}
