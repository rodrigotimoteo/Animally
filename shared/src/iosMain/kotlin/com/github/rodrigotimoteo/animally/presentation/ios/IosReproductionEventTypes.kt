@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEventType
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing reproduction taxonomy adapter.
 *
 * Keeps tolerant legacy parsing and the picker vocabulary in shared Kotlin so
 * native UI code never duplicates storage aliases or canonicalisation rules.
 */
@ObjCName("ReproductionEventTypes")
object IosReproductionEventTypes {
    val knownDisplayLabels: List<String>
        get() = ReproductionEventType.knownEntries.map { it.displayLabel }

    fun displayLabel(raw: String): String =
        ReproductionEventType.from(raw).let { type ->
            if (type == ReproductionEventType.Other) raw.trim() else type.displayLabel
        }

    fun isBreeding(raw: String): Boolean = ReproductionEventType.from(raw) == ReproductionEventType.Breeding

    fun isInitialExam(raw: String): Boolean = ReproductionEventType.from(raw) == ReproductionEventType.InitialExam
}
