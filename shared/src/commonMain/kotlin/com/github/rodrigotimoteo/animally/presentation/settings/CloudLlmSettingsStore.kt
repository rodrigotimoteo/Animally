package com.github.rodrigotimoteo.animally.presentation.settings

import com.github.rodrigotimoteo.animally.llm.cloud.CloudLlmProviderPreset
import com.github.rodrigotimoteo.animally.llm.cloud.isValidCloudBaseUrl as isValidCloudEndpoint

/**
 * User-facing cloud LLM settings. Non-secret fields (enabled flag, model, base URL)
 * live in platform preferences; the API key goes through [SecureStore] and is only
 * ever written when the user types one.
 */
interface CloudLlmSettingsStore {
    /** True when the user opted into routing assistant answers to a cloud model. */
    fun isEnabled(): Boolean

    fun setEnabled(enabled: Boolean)

    /** The stored API key, or null when the user never entered one. */
    fun apiKey(): String?

    /** Stores [key] as the cloud API key, or clears it when null/blank. */
    fun setApiKey(key: String?)

    fun model(): String

    fun setModel(model: String)

    fun baseUrl(): String

    fun setBaseUrl(url: String)

    /** The selected [com.github.rodrigotimoteo.animally.llm.cloud.CloudLlmProviderPreset] id. */
    fun presetId(): String

    fun setPresetId(id: String)
}

/**
 * True when the current settings can be used for a cloud fallback request.
 * Local runtimes deliberately do not require an API key; hosted providers do.
 * Keeping this check beside the settings contract prevents platform bridges
 * from silently drifting away from the routing rules.
 */
fun CloudLlmSettingsStore.isReadyForCloudRouting(): Boolean {
    val settings = snapshot()
    val provider = CloudLlmProviderPreset.fromId(settings.presetId)
    return settings.enabled &&
        (!provider.requiresApiKey || !settings.apiKey.isNullOrBlank()) &&
        settings.model.isNotBlank() &&
        isValidCloudBaseUrl(settings.baseUrl, provider.isLocalRuntime)
}

/** True when model discovery may contact the selected endpoint; a model is not required yet. */
fun CloudLlmSettingsStore.isReadyForCloudModelDiscovery(): Boolean {
    val settings = snapshot()
    val provider = CloudLlmProviderPreset.fromId(settings.presetId)
    return settings.enabled &&
        (!provider.requiresApiKey || !settings.apiKey.isNullOrBlank()) &&
        isValidCloudBaseUrl(settings.baseUrl, provider.isLocalRuntime)
}

/**
 * Checks the endpoint before routing a request. The field accepts either a
 * provider API root or a full chat-completions URL, so validation deliberately
 * checks only the scheme and host and leaves path normalization to the cloud
 * transport.
 */
internal fun isValidCloudBaseUrl(
    value: String,
    allowInsecureLocalEndpoint: Boolean = false,
): Boolean = isValidCloudEndpoint(value, allowInsecureLocalEndpoint)

/** Preference key for the cloud toggle. */
const val PREF_CLOUD_LLM_ENABLED = "llm_cloud_enabled"

/** Preference key for the cloud model name. */
const val PREF_CLOUD_LLM_MODEL = "llm_cloud_model"

/** Preference key for the cloud API root or chat-completions endpoint URL. */
const val PREF_CLOUD_LLM_BASE_URL = "llm_cloud_base_url"

/** Preference key for the selected provider preset id. */
const val PREF_CLOUD_LLM_PRESET_ID = "llm_cloud_provider_preset"

/** Immutable snapshot of the cloud LLM settings for UI state. */
data class CloudLlmSnapshot(
    val enabled: Boolean = false,
    val apiKey: String? = null,
    val model: String = "",
    val baseUrl: String = "",
    val presetId: String = "",
)

/** Reads all fields in one consistent view. */
fun CloudLlmSettingsStore.snapshot(): CloudLlmSnapshot =
    CloudLlmSnapshot(
        enabled = isEnabled(),
        apiKey = apiKey(),
        model = model(),
        baseUrl = baseUrl(),
        presetId = presetId(),
    )

/**
 * Creates the platform-specific [CloudLlmSettingsStore].
 *
 * Android actual backs non-secret fields with SharedPreferences; iOS actual with
 * NSUserDefaults; desktop actual with java.util.prefs.
 */
expect fun createPlatformCloudLlmSettings(): CloudLlmSettingsStore
