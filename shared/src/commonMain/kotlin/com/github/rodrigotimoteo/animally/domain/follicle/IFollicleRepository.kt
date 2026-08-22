package com.github.rodrigotimoteo.animally.domain.follicle

import com.github.rodrigotimoteo.animally.domain.follicle.model.Follicle
import kotlin.time.Instant

/**
 * Repository contract for accessing [Follicle] records.
 */
interface IFollicleRepository {
    /** Returns all active follicles recorded for [ultrasoundId], left side first. */
    fun getByUltrasound(ultrasoundId: Long): List<Follicle>

    /** Returns the active follicle with [id], or `null` when not found. */
    fun getById(id: Long): Follicle?

    /** Inserts [follicle] and returns the generated identifier. */
    fun insert(follicle: Follicle): Long

    /** Updates [follicle] and returns its identifier. */
    fun update(follicle: Follicle): Long

    /** Soft-deletes the follicle with [id] by marking it inactive at [updatedAt]. */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long

    /** Soft-deletes every follicle of [ultrasoundId] at [updatedAt]. */
    fun setInactiveForUltrasound(
        ultrasoundId: Long,
        updatedAt: Instant,
    )
}
