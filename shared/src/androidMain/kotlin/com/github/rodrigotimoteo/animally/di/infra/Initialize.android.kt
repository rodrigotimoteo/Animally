package com.github.rodrigotimoteo.animally.di.infra

import android.content.Context
import com.github.rodrigotimoteo.animally.di.AndroidDatabaseModule
import com.github.rodrigotimoteo.animally.di.database.QueriesModule
import com.github.rodrigotimoteo.animally.di.navigation.navigationEntryModule
import com.github.rodrigotimoteo.animally.di.presentation.PresentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

/**
 * Application context captured during [initKoin]. Used by platform services
 * such as [com.github.rodrigotimoteo.animally.domain.export.shareFile].
 */
internal lateinit var appContext: Context

actual fun initKoin(context: Any?): KoinApplication =
    startKoin {
        appContext = context as Context
        androidContext(appContext)
        modules(
            navigationEntryModule,
            AndroidDatabaseModule().provide(),
            QueriesModule().provide(),
            PresentationModule().provide(),
        )
    }
