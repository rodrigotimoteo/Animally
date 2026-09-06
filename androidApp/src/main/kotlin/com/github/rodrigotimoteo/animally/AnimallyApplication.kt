package com.github.rodrigotimoteo.animally

import android.app.Application
import android.util.Log
import com.github.rodrigotimoteo.animally.di.infra.initKoin
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AnimallyApplication : Application() {
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val koinApplication = initKoin(this)
        startupScope.launch {
            runCatching {
                koinApplication.koin
                    .get<ISearchRepository>()
                    .reindexIfNeeded(ISearchRepository.SEARCH_INDEX_VERSION)
            }.onFailure { error ->
                Log.e("AnimallyApplication", "Search index healing failed", error)
            }
        }
    }
}
