package com.github.rodrigotimoteo.animally.data.backup

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile

/**
 * iOS implementation: writes backup artifacts into `Documents/backups` (so
 * they are user-visible in the Files app) and copies the live SQLDelight
 * database out of `Application Support/databases`.
 */
@OptIn(ExperimentalForeignApi::class)
actual object BackupStorage {
    actual fun writeTextFile(
        fileName: String,
        content: String,
    ): String {
        val targetPath = "${ensureBackupDir()}/$fileName"
        val written =
            NSString
                .create(string = content)
                .writeToFile(path = targetPath, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        check(written) { "Failed to write backup file to $targetPath" }
        return targetPath
    }

    actual fun copyDatabaseFile(): String {
        val sourcePath = databaseFilePath()
        val fileManager = NSFileManager.defaultManager
        check(fileManager.fileExistsAtPath(sourcePath)) {
            "Database file not found at $sourcePath"
        }
        val targetPath = "${ensureBackupDir()}/$DATABASE_COPY_NAME"
        if (fileManager.fileExistsAtPath(targetPath)) {
            fileManager.removeItemAtPath(targetPath, error = null)
        }
        val copied = fileManager.copyItemAtPath(sourcePath, toPath = targetPath, error = null)
        check(copied) { "Failed to copy database from $sourcePath to $targetPath" }
        return targetPath
    }

    /** Creates `Documents/backups` when missing and returns its absolute path. */
    private fun ensureBackupDir(): String {
        val documentsPath =
            requireNotNull(documentsUrl().path) { "Failed to resolve Documents directory" }
        val backupDirPath = "$documentsPath/$BACKUP_DIR"
        NSFileManager.defaultManager.createDirectoryAtPath(
            backupDirPath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        return backupDirPath
    }

    private fun documentsUrl(): NSURL =
        requireNotNull(
            NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null,
            ),
        )

    /**
     * Resolves the live database path the same way SQLDelight's native driver
     * does: `<Application Support>/databases/<name>`.
     */
    private fun databaseFilePath(): String {
        @Suppress("UNCHECKED_CAST")
        val appSupportDirs =
            NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
                as List<String>
        val appSupportPath = requireNotNull(appSupportDirs.firstOrNull())
        return "$appSupportPath/databases/$DATABASE_COPY_NAME"
    }

    private const val BACKUP_DIR = "backups"
    private const val DATABASE_COPY_NAME = "animally.db"
}
