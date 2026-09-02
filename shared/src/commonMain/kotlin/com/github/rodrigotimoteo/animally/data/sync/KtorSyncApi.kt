package com.github.rodrigotimoteo.animally.data.sync

import com.github.rodrigotimoteo.animally.domain.sync.SyncApi
import com.github.rodrigotimoteo.animally.domain.sync.SyncPullResponse
import com.github.rodrigotimoteo.animally.domain.sync.SyncPushRequest
import com.github.rodrigotimoteo.animally.domain.sync.SyncPushResponse
import io.ktor.client.HttpClient
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Remote sync is intentionally disabled until a real authenticated backend is
 * configured. Keeping this implementation bound allows the production graph
 * to remain complete without giving the POC permission to send clinical data.
 */
@Suppress("UnusedPrivateProperty")
@Single(binds = [SyncApi::class])
class KtorSyncApi(
    @Provided private val client: HttpClient,
) : SyncApi {
    override suspend fun pull(since: Instant): SyncPullResponse = disabled()

    override suspend fun push(request: SyncPushRequest): SyncPushResponse = disabled()

    private fun disabled(): Nothing = error(SYNC_DISABLED_MESSAGE)
}

internal const val SYNC_DISABLED_MESSAGE =
    "Remote sync is disabled until a real authenticated backend is configured."
