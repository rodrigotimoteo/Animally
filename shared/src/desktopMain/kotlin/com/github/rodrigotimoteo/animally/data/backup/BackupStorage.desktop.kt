@file:Suppress("ktlint:standard:filename")

package com.github.rodrigotimoteo.animally.data.backup

import java.io.File

/**
 * Desktop implementation: writes into `<tmpdir>/animally/backups` and copies
 * the `animally.db` file produced by [com.github.rodrigotimoteo.animally.di.DesktopDatabaseModule].
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
        val target = File(backupDir(), DATABASE_COPY_NAME)
        val source = File(storageRoot(), DATABASE_NAME)
        if (source.exists()) {
            source.copyTo(target, overwrite = true)
        }
        return target.absolutePath
    }

    private fun backupDir(): File =
        File(storageRoot(), BACKUP_DIR).apply {
            mkdirs()
        }

    private fun storageRoot(): File = File(System.getProperty("java.io.tmpdir"), "animally")

    private const val BACKUP_DIR = "backups"
    private const val DATABASE_NAME = "animally.db"
    private const val DATABASE_COPY_NAME = "animally.db"
}
