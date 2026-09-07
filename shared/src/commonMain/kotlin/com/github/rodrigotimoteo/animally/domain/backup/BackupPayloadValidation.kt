package com.github.rodrigotimoteo.animally.domain.backup

/**
 * Validates a decoded backup before the destructive restore transaction starts.
 *
 * SQLDelight intentionally leaves relationship enforcement to the application
 * layer. Keeping these checks on the staged payload means malformed input is
 * rejected without changing database rows, files, sync cursors, or FTS state.
 */
internal fun BackupPayload.validateForRestore() {
    require(exportedAt.isNotBlank()) { "Backup exportedAt must not be blank" }
    validateCollectionIds()
    validateRelationships()
    validateDomainValues()
    validateAudioPaths()
}

private fun BackupPayload.validateCollectionIds() {
    owners.map { it.id }.validateIds("owners")
    patients.map { it.id }.validateIds("patients")
    anamnese.map { it.id }.validateIds("anamnese")
    consultations.map { it.id }.validateIds("consultations")
    vaccinations.map { it.id }.validateIds("vaccinations")
    weights.map { it.id }.validateIds("weights")
    dewormings.map { it.id }.validateIds("dewormings")
    dentistry.map { it.id }.validateIds("dentistry")
    lameness.map { it.id }.validateIds("lameness")
    surgeries.map { it.id }.validateIds("surgeries")
    medications.map { it.id }.validateIds("medications")
    labResults.map { it.id }.validateIds("labResults")
    imaging.map { it.id }.validateIds("imaging")
    farrierVisits.map { it.id }.validateIds("farrierVisits")
    reproductionEvents.map { it.id }.validateIds("reproductionEvents")
    ultrasounds.map { it.id }.validateIds("ultrasounds")
    gestations.map { it.id }.validateIds("gestations")
    reproMedications.map { it.id }.validateIds("reproMedications")
    substances.map { it.id }.validateIds("substances")
    follicles.map { it.id }.validateIds("follicles")
    embryoTransfers.map { it.id }.validateIds("embryoTransfers")
    icsi.map { it.id }.validateIds("icsi")
    customReminders.map { it.id }.validateIds("customReminders")
    assistantChatHistory.map { it.id }.validateIds("assistantChatHistory")
    dictationCaptures.map { it.id }.validateIds("dictationCaptures")
}

private fun BackupPayload.validateRelationships() {
    val ownerIds = owners.map { it.id }.toSet()
    val patientIds = patients.map { it.id }.toSet()
    val ultrasoundIds = ultrasounds.map { it.id }.toSet()

    patients.forEach { patient ->
        require(patient.ownerId == null || patient.ownerId in ownerIds) {
            "patients contains unknown ownerId ${patient.ownerId} for row ${patient.id}"
        }
    }

    anamnese.requirePatientReferences(patientIds, "anamnese") { it.patientId }
    consultations.requirePatientReferences(patientIds, "consultations") { it.patientId }
    vaccinations.requirePatientReferences(patientIds, "vaccinations") { it.patientId }
    weights.requirePatientReferences(patientIds, "weights") { it.patientId }
    dewormings.requirePatientReferences(patientIds, "dewormings") { it.patientId }
    dentistry.requirePatientReferences(patientIds, "dentistry") { it.patientId }
    lameness.requirePatientReferences(patientIds, "lameness") { it.patientId }
    surgeries.requirePatientReferences(patientIds, "surgeries") { it.patientId }
    medications.requirePatientReferences(patientIds, "medications") { it.patientId }
    labResults.requirePatientReferences(patientIds, "labResults") { it.patientId }
    imaging.requirePatientReferences(patientIds, "imaging") { it.patientId }
    farrierVisits.requirePatientReferences(patientIds, "farrierVisits") { it.patientId }
    reproductionEvents.requirePatientReferences(patientIds, "reproductionEvents") { it.patientId }
    ultrasounds.requirePatientReferences(patientIds, "ultrasounds") { it.patientId }
    gestations.requirePatientReferences(patientIds, "gestations") { it.patientId }
    reproMedications.requirePatientReferences(patientIds, "reproMedications") { it.patientId }
    substances.requirePatientReferences(patientIds, "substances") { it.patientId }
    embryoTransfers.requirePatientReferences(patientIds, "embryoTransfers") { it.patientId }
    icsi.requirePatientReferences(patientIds, "icsi") { it.patientId }
    customReminders.requirePatientReferences(patientIds, "customReminders") { it.patientId }
    follicles.forEach { follicle ->
        require(follicle.ultrasoundId in ultrasoundIds) {
            "follicles contains unknown ultrasoundId ${follicle.ultrasoundId} for row ${follicle.id}"
        }
    }
}

private fun BackupPayload.validateDomainValues() {
    medications.forEach { medication ->
        require(medication.startDate == null || medication.endDate == null || medication.endDate >= medication.startDate) {
            "medications has an endDate before startDate for row ${medication.id}"
        }
    }
    weights.forEach { weight ->
        require(weight.weightKg >= 0.0) { "weights has a negative weight for row ${weight.id}" }
    }
    follicles.forEach { follicle ->
        require(follicle.sizeMm >= 0.0) { "follicles has a negative size for row ${follicle.id}" }
    }
    embryoTransfers.forEach { transfer ->
        require(transfer.embryoCount >= 0L) { "embryoTransfers has a negative count for row ${transfer.id}" }
    }
    icsi.forEach { session ->
        require(session.folliclesRecovered >= 0L) { "icsi has a negative follicle count for row ${session.id}" }
    }
    customReminders.forEach { reminder ->
        require((reminder.linkedRecordType == null) == (reminder.linkedRecordId == null)) {
            "customReminders must provide linkedRecordType and linkedRecordId together for row ${reminder.id}"
        }
    }
}

private fun BackupPayload.validateAudioPaths() {
    dictationCaptures.forEach { capture ->
        val path = capture.audioPath ?: return@forEach
        require(path.isNotBlank() && !path.endsWith('/')) {
            "dictationCaptures contains an invalid audio path for row ${capture.id}"
        }
    }
}

private fun List<Long>.validateIds(collectionName: String) {
    require(all { it > 0L }) { "Backup collection $collectionName contains a non-positive id" }
    require(size == toSet().size) { "Backup collection $collectionName contains duplicate ids" }
}

private inline fun <T> Iterable<T>.requirePatientReferences(
    patientIds: Set<Long>,
    collectionName: String,
    patientId: (T) -> Long,
) {
    for (row in this) {
        val id = patientId(row)
        require(id in patientIds) {
            "Backup collection $collectionName contains unknown patientId $id"
        }
    }
}
