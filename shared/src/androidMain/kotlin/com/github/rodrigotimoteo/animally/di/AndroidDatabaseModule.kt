package com.github.rodrigotimoteo.animally.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.AnimallyDatabaseFactory
import org.koin.core.annotation.Module
import org.koin.dsl.module

@Module
class AndroidDatabaseModule {

    fun provide() = module {
        single<SqlDriver> {
            AndroidSqliteDriver(
                schema = AnimallyDatabase.Schema,
                context = get<Context>(),
                name = "animally.db",
            )
        }
        single<AnimallyDatabase> { AnimallyDatabaseFactory.create(get()) }
    }
}
