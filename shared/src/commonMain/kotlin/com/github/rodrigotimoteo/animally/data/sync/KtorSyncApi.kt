package com.github.rodrigotimoteo.animally.data.sync

import com.github.rodrigotimoteo.animally.domain.sync.SyncApi
import com.github.rodrigotimoteo.animally.domain.sync.SyncPullResponse
import com.github.rodrigotimoteo.animally.domain.sync.SyncPushRequest
import com.github.rodrigotimoteo.animally.domain.sync.SyncPushResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Base URL of the sync server. POC stub — replaced by real configuration when
 * the backend exists.
 */
private const val SYNC_BASE_URL: String = "http://localhost:8080"

/**
 * Stub auth key, accepted by the mock server until real auth lands.
 */
private const val SYNC_API_KEY: String = "mock-key"

/**
 * Fallback device id used on pull requests, which carry no request body.
 */
private const val SYNC_DEVICE_ID: String = "poc-device-id"

/**
 * HTTP implementation of [SyncApi].
 *
 * Pull: `GET {baseUrl}/sync?since=ISO8601`. Push: `POST {baseUrl}/sync/push`
 * with the request as JSON body. Both carry `X-Api-Key` and `X-Device-Id`
 * headers as stub authentication.
 */
@Single(binds = [SyncApi::class])
class KtorSyncApi(
    @Provided private val client: HttpClient,
) : SyncApi {
    override suspend fun pull(since: Instant): SyncPullResponse =
        client
            .get("$SYNC_BASE_URL/sync") {
                parameter("since", since.toString())
                header("X-Api-Key", SYNC_API_KEY)
                header("X-Device-Id", SYNC_DEVICE_ID)
            }.body()

    override suspend fun push(request: SyncPushRequest): SyncPushResponse =
        client
            .post("$SYNC_BASE_URL/sync/push") {
                contentType(ContentType.Application.Json)
                header("X-Api-Key", SYNC_API_KEY)
                header("X-Device-Id", request.deviceId)
                setBody(request)
            }.body()
}
