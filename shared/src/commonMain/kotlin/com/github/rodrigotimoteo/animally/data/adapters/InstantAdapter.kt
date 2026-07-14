package com.github.rodrigotimoteo.animally.data.adapters

import app.cash.sqldelight.ColumnAdapter
import kotlin.time.Instant

/**
 * Adapter for [Instant] to [Long] conversion for SQLDelight.
 *
 * @see Instant
 * @author rodrigotimoteo
 */
object InstantAdapter : ColumnAdapter<Instant, Long> {
    /**
     * Decodes a [Long] value from the database to an [Instant].
     *
     * @param databaseValue The value from the database.
     * @return The decoded [Instant].
     */
    override fun decode(databaseValue: Long): Instant {
        return Instant.fromEpochMilliseconds(databaseValue)
    }

    /**
     * Encodes an [Instant] value to a [Long] for storage in the database.
     *
     * @param value The [Instant] to encode.
     * @return The encoded [Long].
     */
    override fun encode(value: Instant): Long {
        return value.toEpochMilliseconds()
    }
}
