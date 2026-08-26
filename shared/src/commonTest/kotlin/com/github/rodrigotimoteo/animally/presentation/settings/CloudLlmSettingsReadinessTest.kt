package com.github.rodrigotimoteo.animally.presentation.settings

import com.github.rodrigotimoteo.animally.llm.cloud.CloudLlmProviderPreset
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CloudLlmSettingsReadinessTest {
    @Test
    fun `hosted provider requires a key and complete endpoint settings`() {
        val store = FakeCloudSettings()
        store.storedEnabled = true
        store.storedPresetId = CloudLlmProviderPreset.OPENAI.id
        store.storedModel = "gpt-4o-mini"
        store.storedBaseUrl = CloudLlmProviderPreset.OPENAI.baseUrl

        assertFalse(store.isReadyForCloudRouting())

        store.storedApiKey = "key"

        assertTrue(store.isReadyForCloudRouting())
    }

    @Test
    fun `local provider does not require an api key`() {
        val store = FakeCloudSettings()
        store.storedEnabled = true
        store.storedPresetId = CloudLlmProviderPreset.OLLAMA.id
        store.storedModel = "llama3.2"
        store.storedBaseUrl = CloudLlmProviderPreset.OLLAMA.baseUrl

        assertTrue(store.isReadyForCloudRouting())
    }

    @Test
    fun `missing model or endpoint keeps cloud routing disabled`() {
        val store = FakeCloudSettings()
        store.storedEnabled = true
        store.storedPresetId = CloudLlmProviderPreset.ZEN.id
        store.storedApiKey = "key"

        assertFalse(store.isReadyForCloudRouting())

        store.storedModel = "some-model"
        assertFalse(store.isReadyForCloudRouting())

        store.storedBaseUrl = CloudLlmProviderPreset.ZEN.baseUrl
        assertTrue(store.isReadyForCloudRouting())
    }
}

private class FakeCloudSettings : CloudLlmSettingsStore {
    var storedEnabled = false
    var storedApiKey: String? = null
    var storedModel = ""
    var storedBaseUrl = ""
    var storedPresetId = ""

    override fun isEnabled(): Boolean = storedEnabled

    override fun setEnabled(enabled: Boolean) {
        storedEnabled = enabled
    }

    override fun apiKey(): String? = storedApiKey

    override fun setApiKey(key: String?) {
        storedApiKey = key
    }

    override fun model(): String = storedModel

    override fun setModel(model: String) {
        storedModel = model
    }

    override fun baseUrl(): String = storedBaseUrl

    override fun setBaseUrl(url: String) {
        storedBaseUrl = url
    }

    override fun presetId(): String = storedPresetId

    override fun setPresetId(id: String) {
        storedPresetId = id
    }
}
