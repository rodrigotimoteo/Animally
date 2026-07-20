package com.github.rodrigotimoteo.animally.di.database

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase

actual fun createTestDatabase(): AnimallyDatabase {
    val driver = NativeSqliteDriver(AnimallyDatabase.Schema, ":memory:")
    return AnimallyDatabaseFactory.create(driver)
}
