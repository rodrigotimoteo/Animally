package com.github.rodrigotimoteo.animally.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.github.rodrigotimoteo.animally.bridge.ObjCHidden
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.AnimallyDatabaseFactory
import org.koin.core.annotation.Module
import org.koin.dsl.module

@Module
@ObjCHidden
class IosDatabaseModule {
    fun provide() =
        module {
            single<SqlDriver> {
                NativeSqliteDriver(
                    schema = AnimallyDatabase.Schema,
                    name = "animally.db",
                )
            }
            single<AnimallyDatabase> { AnimallyDatabaseFactory.create(get()) }
        }
}
