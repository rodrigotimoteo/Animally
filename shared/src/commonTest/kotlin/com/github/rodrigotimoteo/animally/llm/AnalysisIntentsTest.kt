package com.github.rodrigotimoteo.animally.llm

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnalysisIntentsTest {
    @Test
    fun `simple record questions do not request cloud tools`() {
        assertFalse(AnalysisIntents.requiresTools("When was Bella's last vaccination?"))
        assertFalse(AnalysisIntents.requiresTools("Is Bella pregnant?"))
    }

    @Test
    fun `broader analysis questions request cloud tools`() {
        assertTrue(AnalysisIntents.requiresTools("Analyze the average weight over time"))
        assertTrue(AnalysisIntents.requiresTools("Compare vaccinations by month"))
        assertTrue(AnalysisIntents.requiresTools("Faz uma análise dos dados de peso"))
    }

    @Test
    fun `natural patient inventory questions request a deterministic census`() {
        assertTrue(AnalysisIntents.isAnalysisQuery("What horses do I have?"))
        assertTrue(AnalysisIntents.isAnalysisQuery("List my horses"))
        assertTrue(AnalysisIntents.isAnalysisQuery("Que cavalos tenho?"))
    }
}
