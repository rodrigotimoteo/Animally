package com.github.rodrigotimoteo.animally.presentation.reproduction

import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEventType
import kotlin.test.Test
import kotlin.test.assertEquals

class ReproductionEventEditPickerTest {
    @Test
    fun pickerValuesEqualKnownEntriesDisplayLabels() {
        val expected = listOf("Heat", "Breeding", "Pregnancy Check", "Foaling", "Initial Exam")
        val pickerValues = ReproductionEventType.knownEntries.map { it.displayLabel }
        assertEquals(expected, pickerValues)
        assertEquals(pickerValues, ReproductionEventType.knownEntries.map { it.storageLabel })
    }

    @Test
    fun iosPickerParityWithSharedKnownEntries() {
        // iOS ReproductionEventEditView.swift hardcodes:
        val iosEventTypes = listOf("Heat", "Breeding", "Pregnancy Check", "Foaling", "Initial Exam")
        val sharedPicker = ReproductionEventType.knownEntries.map { it.displayLabel }
        assertEquals(sharedPicker, iosEventTypes)
    }
}
