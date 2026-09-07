package com.github.rodrigotimoteo.animally.sync.cloudkit

import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CloudKitExportCursorTest {
    @Test
    fun `failed older row pins cursor so it is retried`() {
        val staged = listOf("older" to 100L, "newer" to 200L)

        assertEquals(
            0L,
            safeExportCursor(
                staged = staged,
                cursorMs = 0L,
                confirmedNames = setOf("newer"),
                failedNames = setOf("older"),
            ),
        )
    }

    @Test
    fun `confirmed rows before failure can advance the cursor`() {
        val staged = listOf("first" to 100L, "failed" to 200L, "last" to 300L)

        assertEquals(
            100L,
            safeExportCursor(
                staged = staged,
                cursorMs = 0L,
                confirmedNames = setOf("first", "last"),
                failedNames = setOf("failed"),
            ),
        )
    }

    @Test
    fun `equal timestamp failure retries the entire timestamp`() {
        val staged = listOf("failed" to 100L, "confirmed" to 100L)

        assertEquals(
            0L,
            safeExportCursor(
                staged = staged,
                cursorMs = 0L,
                confirmedNames = setOf("confirmed"),
                failedNames = setOf("failed"),
            ),
        )
    }

    @Test
    fun `explicitly cleared parent is preserved in cloud envelope`() {
        val envelope =
            CloudKitEnvelope.from(
                recordType = "Patient",
                recordName = "patient-1",
                updatedAtMs = 100L,
                isActive = true,
                parents = mapOf("ownerId" to null),
                payload = JsonObject(emptyMap()),
            )

        val parents = envelope.toSyncRecord().parentServerIds
        assertTrue(parents.containsKey("ownerId"))
        assertEquals(null, parents["ownerId"])
    }

    @Test
    fun `account change bridge event is recognized as a reset signal`() {
        val event = parseSyncBridgeEvent("""{"type":"accountChange","available":true}""")

        val accountChange = assertIs<SyncBridgeEvent.AccountChange>(event)
        assertTrue(accountChange.available)
    }
}
