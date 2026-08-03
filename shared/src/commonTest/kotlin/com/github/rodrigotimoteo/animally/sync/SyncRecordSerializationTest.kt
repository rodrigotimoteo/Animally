package com.github.rodrigotimoteo.animally.sync

import com.github.rodrigotimoteo.animally.domain.sync.SyncEntityType
import com.github.rodrigotimoteo.animally.domain.sync.SyncPushRequest
import com.github.rodrigotimoteo.animally.domain.sync.SyncRecord
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Proves the sync DTOs are wire-stable: the exact Patient sample from the
 * Phase 6 design round-trips through JSON unchanged, and every entity wire
 * name resolves back to its enum entry.
 */
class SyncRecordSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val designSample: SyncPushRequest =
        SyncPushRequest(
            deviceId = "device-42",
            records =
                listOf(
                    SyncRecord(
                        type = SyncEntityType.PATIENT.wireName,
                        clientId = 7L,
                        updatedAt = Instant.parse("2026-08-03T10:00:00Z"),
                        parentServerIds = mapOf("ownerId" to "srv-1"),
                        payload =
                            buildJsonObject {
                                put("name", JsonPrimitive("Charlie"))
                                put("breed", JsonPrimitive("Hanoverian"))
                            },
                    ),
                ),
        )

    @Test
    fun `whenEncodingDesignSample thenDecodesBackToEqualValue`() {
        val encoded = json.encodeToString(SyncPushRequest.serializer(), designSample)
        val decoded = json.decodeFromString(SyncPushRequest.serializer(), encoded)

        assertEquals(designSample, decoded)
    }

    @Test
    fun `whenEncodingDesignSample thenParentReferencesAndPayloadSurvive`() {
        val encoded = json.encodeToString(SyncPushRequest.serializer(), designSample)

        assertTrue(encoded.contains("\"parentServerIds\":{\"ownerId\":\"srv-1\"}"))
        assertTrue(encoded.contains("\"name\":\"Charlie\""))
        assertTrue(encoded.contains("\"deviceId\":\"device-42\""))
    }

    @Test
    fun `whenMappingEveryWireName thenAllEntityTypesResolve`() {
        SyncEntityType.entries.forEach { entry -> assertEquals(entry, SyncEntityType.fromWireName(entry.wireName)) }
    }

    @Test
    fun `whenMappingUnknownWireName thenReturnsNull`() {
        assertNull(SyncEntityType.fromWireName("Spaceship"))
    }
}
