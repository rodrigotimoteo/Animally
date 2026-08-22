package com.github.rodrigotimoteo.animally.domain.embryotransfer

import com.github.rodrigotimoteo.animally.domain.embryotransfer.model.EmbryoTransfer
import kotlin.time.Instant

/**
 * Repository contract for accessing [EmbryoTransfer] records.
 */
interface IEmbryoTransferRepository {
    /** Returns all active records for [patientId], most recent first. */
    fun getByPatient(patientId: Long): List<EmbryoTransfer>

    /** Returns the active record with [id], or `null` when not found. */
    fun getById(id: Long): EmbryoTransfer?

    /** Inserts [record] and returns the generated identifier. */
    fun insert(record: EmbryoTransfer): Long

    /** Updates [record] and returns its identifier. */
    fun update(record: EmbryoTransfer): Long

    /** Soft-deletes the record with [id] by marking it inactive at [updatedAt]. */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long
}
