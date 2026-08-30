package com.github.rodrigotimoteo.animally.llm.analysis

import com.github.rodrigotimoteo.animally.domain.patient.model.Patient

internal class PatientCensusBuilder {
    fun censusBlock(patients: List<Patient>): String {
        val names = patients.take(MAX_NAMES_IN_CENSUS).joinToString(", ") { it.name }
        val overflow = if (patients.size > MAX_NAMES_IN_CENSUS) " …" else ""
        return "PATIENT CENSUS: ${patients.size} active ${pluralize("patient", patients.size)}: $names$overflow."
    }
}
