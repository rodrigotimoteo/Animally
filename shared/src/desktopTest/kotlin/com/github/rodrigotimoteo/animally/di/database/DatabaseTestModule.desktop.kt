@file:Suppress("ktlint:standard:filename")

package com.github.rodrigotimoteo.animally.di.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val databaseTestModule =
    module {
        single<SqlDriver> {
            JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
                AnimallyDatabase.Schema.create(it)
            }
        }
        single<AnimallyDatabase> { AnimallyDatabaseFactory.create(get()) }
    }

actual fun databaseTestModules(): List<Module> = listOf(databaseTestModule)
