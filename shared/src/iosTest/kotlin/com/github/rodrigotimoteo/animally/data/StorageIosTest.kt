package com.github.rodrigotimoteo.animally.data

import com.github.rodrigotimoteo.animally.data.backup.BackupStorage
import com.github.rodrigotimoteo.animally.data.storage.FileStorage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.posix.memcpy
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * On-device (simulator) tests for the iOS file-backed storage actuals:
 * backup JSON writing, raw database copying, and attachment byte round-trips.
 */
@OptIn(ExperimentalForeignApi::class)
class StorageIosTest {
    @Test
    fun writeTextFileWritesReadableContent() {
        val content = """{"schemaVersion":1,"patients":["Charlie"]}"""

        val path = BackupStorage.writeTextFile("storage-test-backup.json", content)

        assertTrue(path.endsWith("/Documents/backups/storage-test-backup.json"), "unexpected path: $path")
        val readBack = NSString.create(contentsOfFile = path)?.toString()
        assertEquals(content, readBack)
    }

    @Test
    fun copyDatabaseFileProducesNonEmptyCopyAtReturnedPath() {
        seedSourceDatabase()

        val copyPath = BackupStorage.copyDatabaseFile()

        assertTrue(copyPath.endsWith("/Documents/backups/animally.db"), "unexpected path: $copyPath")
        val copiedData = NSData.create(contentsOfFile = copyPath)
        assertNotNull(copiedData)
        assertTrue(copiedData.length > 0uL, "database copy should not be empty")
    }

    @Test
    fun saveBytesRoundTrips() {
        val bytes = "attachment-bytes-123".encodeToByteArray()

        val path = FileStorage.saveBytes("storage-test-attachment.bin", bytes)
        val readBack = requireNotNull(NSData.create(contentsOfFile = path)).toByteArray()

        assertContentEquals(bytes, readBack)
    }

    /** Writes a dummy SQLite-like payload where the live driver would put it. */
    private fun seedSourceDatabase() {
        @Suppress("UNCHECKED_CAST")
        val appSupportDirs =
            NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
                as List<String>
        val databasesDir = "${appSupportDirs.first()}/databases"
        NSFileManager.defaultManager.createDirectoryAtPath(
            databasesDir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        val payload = "SQLite format 3 test payload".encodeToByteArray()
        payload.usePinned { pinned ->
            val data = NSData.create(bytes = pinned.addressOf(0), length = payload.size.toULong())
            check(data.writeToFile("$databasesDir/animally.db", atomically = true))
        }
    }

    private fun NSData.toByteArray(): ByteArray {
        val size = length.toInt()
        val result = ByteArray(size)
        if (size > 0) {
            result.usePinned { pinned ->
                memcpy(pinned.addressOf(0), bytes, length)
            }
        }
        return result
    }
}
