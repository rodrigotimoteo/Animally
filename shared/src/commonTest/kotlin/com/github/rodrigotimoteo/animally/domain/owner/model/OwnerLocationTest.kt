package com.github.rodrigotimoteo.animally.domain.owner.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class OwnerLocationTest {
    @Test
    fun validCoordinatesAreAccepted() {
        val location = OwnerLocation(latitude = 38.7223, longitude = -9.1393)

        assertEquals(38.7223, location.latitude)
        assertEquals(-9.1393, location.longitude)
    }

    @Test
    fun latitudeOutsideValidRangeIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            OwnerLocation(latitude = 90.1, longitude = 0.0)
        }
    }

    @Test
    fun longitudeOutsideValidRangeIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            OwnerLocation(latitude = 0.0, longitude = -180.1)
        }
    }

    @Test
    fun incompleteNullablePairIsTreatedAsNoLocation() {
        assertNull(OwnerLocation.fromNullable(latitude = 38.0, longitude = null))
        assertNull(OwnerLocation.fromNullable(latitude = null, longitude = -9.0))
    }

    @Test
    fun invalidNullablePairIsTreatedAsNoLocation() {
        assertNull(OwnerLocation.fromNullable(latitude = 91.0, longitude = 0.0))
        assertNull(OwnerLocation.fromNullable(latitude = 0.0, longitude = Double.NaN))
    }
}
