package com.github.rodrigotimoteo.animally.di.database

import app.cash.sqldelight.driver.native.inMemoryDriver
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase

/**
 * iOS test database.
 *
 * Uses sqldelight's true in-memory driver. Note that `NativeSqliteDriver(schema, ":memory:")`
 * is NOT ephemeral on native — it opens a file literally named `:memory:` that all tests share.
 */
actual fun createTestDatabase(): AnimallyDatabase {
    val driver = inMemoryDriver(AnimallyDatabase.Schema)
    return AnimallyDatabaseFactory.create(driver)
}
