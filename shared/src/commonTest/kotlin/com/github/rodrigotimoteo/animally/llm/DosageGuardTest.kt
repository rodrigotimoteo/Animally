package com.github.rodrigotimoteo.animally.llm

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DosageGuardTest {
    @Test
    fun `given dosage phrasing when detected then dosage intent true`() {
        assertTrue(DosageGuard.isDosageIntent("How much metronidazole should I give?"))
        assertTrue(DosageGuard.isDosageIntent("What dose of detomidine?"))
        assertTrue(DosageGuard.isDosageIntent("500 mg twice daily?"))
        assertTrue(DosageGuard.isDosageIntent("How many ml of sedative?"))
        assertTrue(DosageGuard.isDosageIntent("When was the last dose administered?"))
    }

    @Test
    fun `given weight or temperature question when detected then dosage intent false`() {
        // "how much" is legitimate for measurements - the guardrail must not
        // refuse "How much does Thunder weigh?".
        assertFalse(DosageGuard.isDosageIntent("How much does Thunder weigh?"))
        assertFalse(DosageGuard.isDosageIntent("What is her temperature?"))
        assertFalse(DosageGuard.isDosageIntent("How has her weight changed?"))
    }

    @Test
    fun `given unrelated question when detected then dosage intent false`() {
        assertFalse(DosageGuard.isDosageIntent("When was the last farrier visit?"))
        assertFalse(DosageGuard.isDosageIntent("Tell me about Thunder"))
    }
}
