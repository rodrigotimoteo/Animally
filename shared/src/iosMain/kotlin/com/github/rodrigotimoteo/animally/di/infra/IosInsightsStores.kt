@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.di.infra

import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDrillDown
import com.github.rodrigotimoteo.animally.presentation.insights.InsightsRecordsViewModel
import com.github.rodrigotimoteo.animally.presentation.insights.InsightsViewModel
import com.github.rodrigotimoteo.animally.presentation.ios.InsightsRecordsStore
import com.github.rodrigotimoteo.animally.presentation.ios.InsightsStore
import org.koin.core.parameter.parametersOf
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing factories for the Insights dashboard.
 *
 * Follows [IosSettingsStores.timelineStore] and delegates store
 * construction to Koin. The [InsightsViewModel] is scoped with an
 * optional [patientId] parameter; null means global scope.
 * Swift passes `KotlinLong?` (via `KotlinLong(longLong:)`) which bridges to
 * `Long?`; [InsightsPresentationModule.extractPatientId] handles both
 * `Long` and `Number`/`KotlinLong` to avoid degrading to global.
 */
@ObjCName("IosInsightsStores")
object IosInsightsStores {
    /**
     * Returns a store exposing the Insights dashboard for [patientId],
     * or the global dashboard when `null`.
     * Patient-scoped call must not degrade to global; bridging always resolves
     * via [com.github.rodrigotimoteo.animally.di.presentation.InsightsPresentationModule].
     */
    fun insightsStore(patientId: Long?): InsightsStore {
        // Swift's KotlinLong(longLong:) bridges to Long?; Koin stores as Long or Number.
        // extractPatientId resolves both, so patient scope never silently becomes null.
        val viewModel: InsightsViewModel = IosAppBridge.koin.get { parametersOf(patientId) }
        return InsightsStore(viewModel)
    }

    /**
     * Returns a store exposing the filtered record list for [drillDown].
     */
    fun insightsRecordsStore(drillDown: InsightsDrillDown): InsightsRecordsStore {
        val viewModel: InsightsRecordsViewModel = IosAppBridge.koin.get { parametersOf(drillDown) }
        return InsightsRecordsStore(viewModel)
    }
}
