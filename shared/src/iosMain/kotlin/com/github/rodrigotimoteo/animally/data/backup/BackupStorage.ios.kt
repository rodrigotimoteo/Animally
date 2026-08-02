package com.github.rodrigotimoteo.animally.data.backup

/**
 * iOS stub for the POC — real `NSDocumentDirectory`-backed storage is deferred
 * to the Settings lane. Returns deterministic paths so callers still work.
 */
actual object BackupStorage {
    actual fun writeTextFile(
        fileName: String,
        content: String,
    ): String = "$BACKUP_DIR/$fileName"

    actual fun copyDatabaseFile(): String = "$BACKUP_DIR/$DATABASE_COPY_NAME"

    private const val BACKUP_DIR = "backups"
    private const val DATABASE_COPY_NAME = "animally.db"
}
