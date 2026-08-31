package com.github.rodrigotimoteo.animally.domain.reproduction.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ReproductionEventTypeTest {
    @Test
    fun pregnancyCheckVariantsMapToOneCategory() {
        val variants =
            listOf(
                "PregnancyCheck",
                "Pregnancy Check",
                "pregnancy_check",
                "pregnancy-check",
                "PREGNANCYCHECK",
                "Pregnancy_Check",
                "  pregnancy check  ",
            )

        variants.forEach { raw ->
            assertEquals(
                ReproductionEventType.PregnancyCheck,
                ReproductionEventType.from(raw),
                "variant '$raw' should map to PregnancyCheck",
            )
        }
    }

    @Test
    fun initialExamVariantsMapToOneCategory() {
        val variants =
            listOf(
                "InitialExam",
                "Initial Exam",
                "initial_exam",
                "initial-exam",
                "INITIALEXAM",
            )

        variants.forEach { raw ->
            assertEquals(ReproductionEventType.InitialExam, ReproductionEventType.from(raw))
        }
    }

    @Test
    fun knownPickerValuesRoundTrip() {
        val known =
            listOf(
                ReproductionEventType.Heat,
                ReproductionEventType.Breeding,
                ReproductionEventType.PregnancyCheck,
                ReproductionEventType.Foaling,
                ReproductionEventType.InitialExam,
            )

        known.forEach { type ->
            assertEquals(type, ReproductionEventType.from(type.storageLabel))
            assertEquals(type, ReproductionEventType.from(type.displayLabel))
            assertEquals(type.storageLabel, type.displayLabel)
            assertEquals(type.storageLabel, ReproductionEventType.from(type.storageLabel).storageLabel)
        }

        // also verify knownEntries helper
        assertEquals(known.toSet(), ReproductionEventType.knownEntries.toSet())
    }

    @Test
    fun tolerantParsingIsCaseInsensitiveAndIgnoresSeparators() {
        assertEquals(ReproductionEventType.Heat, ReproductionEventType.from("heat"))
        assertEquals(ReproductionEventType.Heat, ReproductionEventType.from("HEAT"))
        assertEquals(ReproductionEventType.Heat, ReproductionEventType.from(" HeAt "))
        assertEquals(ReproductionEventType.Breeding, ReproductionEventType.from("breeding"))
        assertEquals(ReproductionEventType.Breeding, ReproductionEventType.from("BREEDING"))
        assertEquals(ReproductionEventType.Foaling, ReproductionEventType.from("foaling"))
        assertEquals(ReproductionEventType.Foaling, ReproductionEventType.from("Foaling"))
        assertEquals(ReproductionEventType.Foaling, ReproductionEventType.from("foaling_check".replace("foaling_check", "foaling")))
    }

    @Test
    fun unknownMapsToOtherWhileRawSurvives() {
        val raw = "CustomType"
        val parsed = ReproductionEventType.from(raw)

        assertEquals(ReproductionEventType.Other, parsed)
        assertEquals("Other", parsed.storageLabel)
        assertEquals("Other", parsed.displayLabel)
        // raw survives in original model field, not overwritten
        assertEquals("CustomType", raw)
        assertNotEquals(raw, parsed.storageLabel)

        assertEquals(ReproductionEventType.Other, ReproductionEventType.from("unknown_value"))
        assertEquals(ReproductionEventType.Other, ReproductionEventType.from("  something-else "))
        assertEquals(ReproductionEventType.Other, ReproductionEventType.from(""))
    }

    @Test
    fun storageAndDisplayLabelsAreCanonical() {
        assertEquals("Heat", ReproductionEventType.Heat.storageLabel)
        assertEquals("Heat", ReproductionEventType.Heat.displayLabel)
        assertEquals("Breeding", ReproductionEventType.Breeding.storageLabel)
        assertEquals("Pregnancy Check", ReproductionEventType.PregnancyCheck.storageLabel)
        assertEquals("Pregnancy Check", ReproductionEventType.PregnancyCheck.displayLabel)
        assertEquals("Foaling", ReproductionEventType.Foaling.storageLabel)
        assertEquals("Initial Exam", ReproductionEventType.InitialExam.storageLabel)
        assertEquals("Other", ReproductionEventType.Other.storageLabel)
    }
}
