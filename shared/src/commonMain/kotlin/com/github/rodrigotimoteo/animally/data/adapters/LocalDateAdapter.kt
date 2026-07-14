package com.github.rodrigotimoteo.animally.data.adapters

import app.cash.sqldelight.ColumnAdapter
import kotlinx.datetime.LocalDate

/**
 * Adapter for [LocalDate] to [String] conversion for SQLDelight.
 *
 * @see LocalDate
 * @author rodrigotimoteo
 */
object LocalDateAdapter : ColumnAdapter<LocalDate, String> {
    /**
     * Decodes a [String] value from the database to an [LocalDate].
     *
     * @param databaseValue The value from the database.
     * @return The decoded [LocalDate].
     */
    override fun decode(databaseValue: String): LocalDate = LocalDate.parse(databaseValue)

    /**
     * Encodes an [LocalDate] value to a [String] for storage in the database.
     *
     * @param value The [LocalDate] to encode.
     * @return The encoded [String].
     */
    override fun encode(value: LocalDate): String = value.toString()
}
