package com.github.rodrigotimoteo.animally.presentation.settings

import platform.Foundation.NSUserDefaults

/**
 * iOS [CloudLlmSettingsStore]: non-secret fields in [NSUserDefaults], the API key
 * via the Keychain ([IosKeychainSecureStore]).
 */
class IosCloudLlmSettingsStore(
    private val secureStore: SecureStore = IosKeychainSecureStore(),
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : CloudLlmSettingsStore {
    override fun isEnabled(): Boolean = defaults.boolForKey(PREF_CLOUD_LLM_ENABLED)

    override fun setEnabled(enabled: Boolean) {
        defaults.setBool(enabled, forKey = PREF_CLOUD_LLM_ENABLED)
    }

    override fun apiKey(): String? = secureStore.get(SECURE_STORE_CLOUD_API_KEY)?.takeIf(String::isNotBlank)

    override fun setApiKey(key: String?) {
        if (key.isNullOrBlank()) {
            secureStore.remove(SECURE_STORE_CLOUD_API_KEY)
        } else {
            secureStore.put(SECURE_STORE_CLOUD_API_KEY, key)
        }
    }

    override fun model(): String = defaults.stringForKey(PREF_CLOUD_LLM_MODEL) ?: CloudLlmConfig.DEFAULT_MODEL

    override fun setModel(model: String) {
        defaults.setObject(model, forKey = PREF_CLOUD_LLM_MODEL)
    }

    override fun baseUrl(): String = defaults.stringForKey(PREF_CLOUD_LLM_BASE_URL) ?: CloudLlmConfig.DEFAULT_BASE_URL

    override fun setBaseUrl(url: String) {
        defaults.setObject(url, forKey = PREF_CLOUD_LLM_BASE_URL)
    }
}

/** iOS [SecureStore] factory backed by the Keychain. */
actual fun createPlatformSecureStore(): SecureStore = IosKeychainSecureStore()

/** iOS [CloudLlmSettingsStore] factory. */
actual fun createPlatformCloudLlmSettings(): CloudLlmSettingsStore = IosCloudLlmSettingsStore()
