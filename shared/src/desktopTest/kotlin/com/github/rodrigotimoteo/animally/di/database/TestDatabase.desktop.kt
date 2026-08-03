@file:Suppress("ktlint:standard:filename")

package com.github.rodrigotimoteo.animally.di.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase

actual fun createTestDatabase(): AnimallyDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    AnimallyDatabase.Schema.create(driver)
    return AnimallyDatabaseFactory.create(driver)
}
