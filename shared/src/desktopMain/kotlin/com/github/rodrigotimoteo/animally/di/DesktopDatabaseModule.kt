package com.github.rodrigotimoteo.animally.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.github.rodrigotimoteo.animally.bridge.ObjCHidden
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.AnimallyDatabaseFactory
import org.koin.core.annotation.Module
import org.koin.dsl.module
import java.io.File

/**
 * Desktop SQLDelight driver backed by JDBC SQLite.
 *
 * The database file lives under the platform temp directory so the POC needs
 * no permissions and no user-visible storage. UI tests override the driver via
 * `databaseTestModules()` anyway.
 */
@Module
@ObjCHidden
class DesktopDatabaseModule {
    fun provide() =
        module {
            single<SqlDriver> { desktopSqlDriver() }
            single<AnimallyDatabase> { AnimallyDatabaseFactory.create(get()) }
        }
}

private fun desktopSqlDriver(): SqlDriver {
    val dir = File(System.getProperty("java.io.tmpdir"), "animally")
    dir.mkdirs()
    val dbFile = File(dir, "animally.db")
    val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
    if (!dbFile.exists() || dbFile.length() == 0L) {
        AnimallyDatabase.Schema.create(driver)
    }
    return driver
}
