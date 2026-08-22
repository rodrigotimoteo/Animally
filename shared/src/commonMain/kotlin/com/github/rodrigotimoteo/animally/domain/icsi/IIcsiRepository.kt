package com.github.rodrigotimoteo.animally.domain.icsi

import com.github.rodrigotimoteo.animally.domain.icsi.model.Icsi
import kotlin.time.Instant

/**
 * Repository contract for accessing [Icsi] records.
 */
interface IIcsiRepository {
    /** Returns all active records for [patientId], most recent first. */
    fun getByPatient(patientId: Long): List<Icsi>

    /** Returns the active record with [id], or `null` when not found. */
    fun getById(id: Long): Icsi?

    /** Inserts [record] and returns the generated identifier. */
    fun insert(record: Icsi): Long

    /** Updates [record] and returns its identifier. */
    fun update(record: Icsi): Long

    /** Soft-deletes the record with [id] by marking it inactive at [updatedAt]. */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long
}
