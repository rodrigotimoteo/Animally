package com.github.rodrigotimoteo.animally.di.presentation

import com.github.rodrigotimoteo.animally.bridge.ObjCHidden
import com.github.rodrigotimoteo.animally.presentation.reminder.ReminderPreferenceStore
import com.github.rodrigotimoteo.animally.presentation.reminder.createPlatformReminderPreferenceStore
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.dsl.module

/** Runtime binding for the persisted reminder toggle. */
val reminderPreferenceModule =
    module {
        single<ReminderPreferenceStore> { createPlatformReminderPreferenceStore() }
    }

/** Compile-time satisfier for [ReminderPreferenceStore] injection. */
@Module
@ObjCHidden
class ReminderPreferenceModule {
    @Single
    fun provideReminderPreferenceStore(): ReminderPreferenceStore = createPlatformReminderPreferenceStore()
}
