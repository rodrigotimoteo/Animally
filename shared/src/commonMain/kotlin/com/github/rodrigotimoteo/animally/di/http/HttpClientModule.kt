package com.github.rodrigotimoteo.animally.di.http

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * Provides the shared [HttpClient] used by all network-backed data sources.
 *
 * Engine selection is deferred to the runtime: `HttpClient {}` picks the
 * platform engine from the classpath (Android/Darwin/CIO). Content negotiation
 * wires kotlinx.serialization for JSON bodies; unknown keys are ignored so the
 * client tolerates server-side additions.
 */
@Module
class HttpClientModule {
    @Single
    fun provideHttpClient(): HttpClient =
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
}
