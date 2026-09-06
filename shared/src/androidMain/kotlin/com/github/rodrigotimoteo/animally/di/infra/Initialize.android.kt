package com.github.rodrigotimoteo.animally.di.infra

import android.content.Context
import com.github.rodrigotimoteo.animally.di.AndroidDatabaseModule
import com.github.rodrigotimoteo.animally.di.database.QueriesModule
import com.github.rodrigotimoteo.animally.di.dispatchers.DispatchersModule
import com.github.rodrigotimoteo.animally.di.navigation.navigationEntryModule
import com.github.rodrigotimoteo.animally.di.presentation.PresentationModule
import com.github.rodrigotimoteo.animally.di.presentation.cloudLlmModule
import com.github.rodrigotimoteo.animally.di.presentation.reminderPreferenceModule
import com.github.rodrigotimoteo.animally.domain.notification.ensureReminderChannel
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import com.github.rodrigotimoteo.animally.di.dispatchers.module as dispatchersModule
import com.github.rodrigotimoteo.animally.di.infra.module as appModule

/**
 * Application context captured during [initKoin]. Used by platform services
 * such as [com.github.rodrigotimoteo.animally.domain.export.shareFile].
 */
internal lateinit var appContext: Context

actual fun initKoin(context: Any?): KoinApplication =
    startKoin {
        appContext = context as Context
        ensureReminderChannel(appContext)
        androidContext(appContext)
        modules(
            buildList {
                add(navigationEntryModule)
                // Generated annotation bindings contain the repositories, use cases and
                // parameterless view models used by the Android application shell.
                add(AppModule().appModule())
                add(DispatchersModule().dispatchersModule())
                add(AndroidDatabaseModule().provide())
                add(QueriesModule().provide())
                add(PresentationModule().provide())
                add(reminderPreferenceModule)
                add(cloudLlmModule)
                add(com.github.rodrigotimoteo.animally.llm.llmModule)
                add(com.github.rodrigotimoteo.animally.di.dictationModule)
                add(com.github.rodrigotimoteo.animally.di.settingsModule)
            },
        )
    }
