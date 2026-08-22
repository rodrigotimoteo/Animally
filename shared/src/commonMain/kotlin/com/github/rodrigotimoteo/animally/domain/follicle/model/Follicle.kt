package com.github.rodrigotimoteo.animally.domain.follicle.model

import kotlin.time.Instant

/**
 * Domain model representing a single follicle observed on one ovary during
 * an ultrasound examination.
 *
 * A mare commonly has several follicles per ovary, so each follicle is its
 * own record tied to its [Ultrasound][com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound]
 * and ovary side.
 *
 * @property id Unique identifier for the follicle record.
 * @property ultrasoundId Identifier of the ultrasound this follicle belongs to.
 * @property side Ovary side, `LEFT` or `RIGHT`.
 * @property sizeMm Follicle size in millimeters.
 * @property description Optional free-form description of the follicle.
 * @property isActive Indicates whether the record is active. Defaults to `true`.
 * @property createdAt Timestamp when the record was created.
 * @property updatedAt Timestamp when the record was last modified.
 */
data class Follicle(
    val id: Long,
    val ultrasoundId: Long,
    val side: String,
    val sizeMm: Double,
    val description: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val serverId: String? = null,
) {
    companion object {
        const val SIDE_LEFT = "LEFT"
        const val SIDE_RIGHT = "RIGHT"
    }
}
