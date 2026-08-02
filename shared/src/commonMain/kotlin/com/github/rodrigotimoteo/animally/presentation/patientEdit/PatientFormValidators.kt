package com.github.rodrigotimoteo.animally.presentation.patientEdit

import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/** Number of digits in a valid UELN. */
internal const val UELN_LENGTH = 15

/**
 * Validates a UELN value.
 *
 * @param ueln the trimmed UELN, or `null` when empty.
 * @return an error message when invalid, or `null` when valid.
 */
internal fun validateUeln(ueln: String?): String? =
    when {
        ueln == null -> null
        ueln.length != UELN_LENGTH || ueln.any { !it.isDigit() } -> "UELN must be $UELN_LENGTH digits"
        else -> null
    }

/**
 * Parses a date-of-birth display string, or `null` when blank or malformed.
 *
 * @param value the ISO `yyyy-MM-dd` string, or `null` when empty.
 * @return the parsed [LocalDate], or `null`.
 */
internal fun parseDateOrNull(value: String?): LocalDate? {
    if (value == null) return null
    return runCatching { LocalDate.parse(value) }.getOrNull()
}

/**
 * Builds a [Patient] from the given [form] state.
 *
 * @param form the validated form state.
 * @param now the current timestamp used for new records and the update timestamp.
 * @return the patient to persist.
 */
internal fun buildPatient(
    form: PatientFormState,
    now: Instant,
): Patient =
    Patient(
        id = form.id ?: 0L,
        name = form.name.trim(),
        species = form.species.ifBlank { "Equine" },
        breed = form.breed,
        dateOfBirth = parseDateOrNull(form.dateOfBirth),
        gender = form.gender,
        microchipId = form.microchipId,
        ueln = form.ueln?.trim(),
        registrationNumber = form.registrationNumber,
        stableLocation = form.stableLocation,
        photoUri = form.photoUri,
        notes = form.notes,
        cogginsTestDate = parseDateOrNull(form.cogginsTestDate),
        cogginsResult = form.cogginsResult,
        cogginsExpiryDate = parseDateOrNull(form.cogginsExpiryDate),
        ownerId = form.ownerId,
        createdAt = form.createdAt ?: now,
        updatedAt = now,
    )
