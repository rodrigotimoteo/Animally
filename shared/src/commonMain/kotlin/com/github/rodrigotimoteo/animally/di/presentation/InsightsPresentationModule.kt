package com.github.rodrigotimoteo.animally.di.presentation

import com.github.rodrigotimoteo.animally.bridge.ObjCHidden
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDrillDown
import com.github.rodrigotimoteo.animally.presentation.insights.InsightsRecordsViewModel
import com.github.rodrigotimoteo.animally.presentation.insights.InsightsViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

@Module
@ObjCHidden
internal class InsightsPresentationModule {
    fun provide() =
        module {
            viewModel { params ->
                val patientId: Long? = params.getOrNull<Long>()
                InsightsViewModel(
                    getInsightsDashboardUseCase = get(),
                    ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
                    initialPatientId = patientId,
                )
            }
            viewModel { params ->
                val drillDown: InsightsDrillDown = params.get()
                InsightsRecordsViewModel(
                    repository = get(),
                    ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
                    drillDown = drillDown,
                )
            }
        }
}
