package com.github.rodrigotimoteo.animally.domain.embryotransfer.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Domain model representing an embryo transfer record for a donor mare.
 *
 * Tracks how many embryos the donor produced and which recipient mares
 * received them.
 *
 * @property id Unique identifier for the embryo transfer record.
 * @property patientId Identifier of the donor mare this record belongs to.
 * @property date Date of the embryo collection / transfer.
 * @property embryoCount Number of embryos collected from the donor.
 * @property recipientMares Names of the recipient mares that received embryos.
 * @property vetName Optional name of the veterinarian performing the procedure.
 * @property notes Optional free-form notes.
 * @property isActive Indicates whether the record is active. Defaults to `true`.
 * @property createdAt Timestamp when the record was created.
 * @property updatedAt Timestamp when the record was last modified.
 */
data class EmbryoTransfer(
    val id: Long,
    val patientId: Long,
    val date: LocalDate,
    val embryoCount: Int = 0,
    val recipientMares: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val serverId: String? = null,
)
