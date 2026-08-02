package com.github.rodrigotimoteo.animally.data.backup

import com.github.rodrigotimoteo.animally.di.infra.appContext
import java.io.File

/**
 * Android implementation: writes into `filesDir/backups` and copies the
 * `animally.db` file from the database directory.
 */
actual object BackupStorage {
    actual fun writeTextFile(
        fileName: String,
        content: String,
    ): String {
        val file = File(backupDir(), fileName)
        file.writeText(content)
        return file.absolutePath
    }

    actual fun copyDatabaseFile(): String {
        val source = appContext.getDatabasePath(DATABASE_NAME)
        val target = File(backupDir(), DATABASE_COPY_NAME)
        source.copyTo(target, overwrite = true)
        return target.absolutePath
    }

    private fun backupDir(): File =
        File(appContext.filesDir, BACKUP_DIR).apply {
            mkdirs()
        }

    private const val BACKUP_DIR = "backups"
    private const val DATABASE_NAME = "animally.db"
    private const val DATABASE_COPY_NAME = "animally.db"
}
