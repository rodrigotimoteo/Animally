package com.github.rodrigotimoteo.animally

import com.github.rodrigotimoteo.animally.llm.cloud.CloudLlmConfig
import com.github.rodrigotimoteo.animally.llm.cloud.CloudLlmProviderPreset
import com.github.rodrigotimoteo.animally.presentation.settings.IosCloudLlmSettingsStore
import com.github.rodrigotimoteo.animally.presentation.settings.SECURE_STORE_CLOUD_API_KEY
import com.github.rodrigotimoteo.animally.presentation.settings.SecureStore
import platform.Foundation.NSUserDefaults
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosCloudLlmSettingsStoreTest {
    private lateinit var defaults: NSUserDefaults
    private lateinit var secureStore: InMemorySecureStore

    @BeforeTest
    fun setUp() {
        defaults = NSUserDefaults(suiteName = SUITE_NAME)
        defaults.removePersistentDomainForName(SUITE_NAME)
        secureStore = InMemorySecureStore()
    }

    @AfterTest
    fun tearDown() {
        defaults.removePersistentDomainForName(SUITE_NAME)
    }

    @Test
    fun `cloud settings round trip every non-secret field`() {
        val store = IosCloudLlmSettingsStore(secureStore, defaults)

        assertFalse(store.isEnabled())
        assertEquals(CloudLlmConfig.DEFAULT_MODEL, store.model())
        assertEquals(CloudLlmConfig.DEFAULT_BASE_URL, store.baseUrl())
        assertEquals("", store.presetId())

        store.setEnabled(true)
        store.setModel("test/model")
        store.setBaseUrl(CloudLlmProviderPreset.OPENROUTER.baseUrl)
        store.setPresetId(CloudLlmProviderPreset.OPENROUTER.id)

        val restored = IosCloudLlmSettingsStore(secureStore, defaults)
        assertTrue(restored.isEnabled())
        assertEquals("test/model", restored.model())
        assertEquals(CloudLlmProviderPreset.OPENROUTER.baseUrl, restored.baseUrl())
        assertEquals(CloudLlmProviderPreset.OPENROUTER.id, restored.presetId())
    }

    @Test
    fun `api key uses secure storage and blank input clears it`() {
        val store = IosCloudLlmSettingsStore(secureStore, defaults)

        store.setApiKey("test-key")

        assertEquals("test-key", store.apiKey())
        assertEquals("test-key", secureStore.get(SECURE_STORE_CLOUD_API_KEY))

        store.setApiKey("  ")

        assertNull(store.apiKey())
        assertNull(secureStore.get(SECURE_STORE_CLOUD_API_KEY))
    }

    private class InMemorySecureStore : SecureStore {
        private val values = mutableMapOf<String, String>()

        override fun get(key: String): String? = values[key]

        override fun put(
            key: String,
            value: String,
        ) {
            values[key] = value
        }

        override fun remove(key: String) {
            values.remove(key)
        }
    }

    private companion object {
        const val SUITE_NAME = "com.github.rodrigotimoteo.animally.IosCloudLlmSettingsStoreTest"
    }
}
