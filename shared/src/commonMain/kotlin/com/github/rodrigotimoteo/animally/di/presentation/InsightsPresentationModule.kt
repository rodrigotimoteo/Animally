package com.github.rodrigotimoteo.animally.di.presentation

import com.github.rodrigotimoteo.animally.bridge.ObjCHidden
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDrillDown
import com.github.rodrigotimoteo.animally.presentation.insights.InsightsRecordsViewModel
import com.github.rodrigotimoteo.animally.presentation.insights.InsightsViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.ParametersHolder
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Dedicated presentation module for Insights dashboard.
 *
 * Exposes [InsightsViewModel] with its required dependencies and
 * patient-scope parameter, and [InsightsRecordsViewModel] for
 * drill-down lists.
 */
@Module
@ObjCHidden
internal class InsightsPresentationModule {
    fun provide() =
        module {
            viewModel { params ->
                val patientId: Long? = extractPatientId(params)
                InsightsViewModel(
                    getInsightsDashboardUseCase = get(),
                    ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
                    initialPatientId = patientId,
                    logger = get(),
                )
            }
            viewModel { params ->
                val drillDown: InsightsDrillDown = params.get()
                InsightsRecordsViewModel(
                    repository = get(),
                    ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
                    drillDown = drillDown,
                    logger = get(),
                )
            }
        }

    @Suppress("ReturnCount")
    private fun extractPatientId(params: ParametersHolder): Long? {
        params.getOrNull<Long>()?.let { return it }
        params.getOrNull<KotlinLong>()?.let { return it.longLongValue }
        params.getOrNull<Number>()?.let { return it.toLong() }
        return null
    }

    /**
     * Minimal shim for Swift's boxed Long (`KotlinLong`) bridging via Koin.
     * On JVM/Android the probe is no-op; on iOS the Swift `KotlinLong(longLong:)` boxes the value.
     * Kept explicit to avoid the brittle `Any` probe and to satisfy deterministic patient-scope resolution.
     */
    private class KotlinLong(
        val longLongValue: Long,
    ) : Number() {
        override fun toByte(): Byte = longLongValue.toByte()

        override fun toDouble(): Double = longLongValue.toDouble()

        override fun toFloat(): Float = longLongValue.toFloat()

        override fun toInt(): Int = longLongValue.toInt()

        override fun toLong(): Long = longLongValue

        override fun toShort(): Short = longLongValue.toShort()
    }
}
