package com.github.rodrigotimoteo.animally.data.backup

/**
 * Platform file storage for backups.
 *
 * Writes artifacts into an app-private backup directory and returns absolute
 * paths, so callers never need platform APIs.
 */
expect object BackupStorage {
    /**
     * Writes [content] to a file named [fileName] inside the backup directory
     * and returns its absolute path.
     */
    fun writeTextFile(
        fileName: String,
        content: String,
    ): String

    /**
     * Copies the raw SQLDelight database file into the backup directory and
     * returns the copy's absolute path.
     */
    fun copyDatabaseFile(): String
}
