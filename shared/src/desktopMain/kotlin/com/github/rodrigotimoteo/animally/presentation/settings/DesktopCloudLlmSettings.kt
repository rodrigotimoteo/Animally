package com.github.rodrigotimoteo.animally.presentation.settings

import com.github.rodrigotimoteo.animally.llm.cloud.CloudLlmConfig
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.Base64
import java.util.prefs.Preferences

/**
 * Desktop [SecureStore]: a permission-restricted (owner-only) properties file under
 * `~/.animally/`. Desktop is a development target; values are base64-obfuscated, NOT
 * cryptographically protected — mobile platforms carry the real secure storage.
 */
class DesktopFileSecureStore : SecureStore {
    private val file: Path by lazy {
        val dir = Path.of(System.getProperty("user.home"), DIR_NAME)
        Files.createDirectories(dir)
        val path = dir.resolve(FILE_NAME)
        if (Files.notExists(path)) {
            Files.createFile(path)
        }
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
        path
    }

    private val properties by lazy {
        java.util.Properties().apply {
            if (Files.exists(file)) Files.newInputStream(file).use(::load)
        }
    }

    override fun get(key: String): String? = properties.getProperty(key)?.let(::decode)

    override fun put(
        key: String,
        value: String,
    ) {
        properties[key] = encode(value)
        persist()
    }

    override fun remove(key: String) {
        properties.remove(key)
        persist()
    }

    private fun persist() {
        Files.newOutputStream(file).use { properties.store(it, null) }
    }

    private fun encode(value: String): String = Base64.getEncoder().encodeToString(value.encodeToByteArray())

    private fun decode(value: String): String = String(Base64.getDecoder().decode(value), Charsets.UTF_8)

    private companion object {
        const val DIR_NAME = ".animally"
        const val FILE_NAME = "secure.properties"
    }
}

/** Desktop [SecureStore] factory. */
actual fun createPlatformSecureStore(): SecureStore = DesktopFileSecureStore()

/**
 * Desktop [CloudLlmSettingsStore]: non-secret fields in java.util.prefs, the API key
 * via [DesktopFileSecureStore].
 */
class DesktopCloudLlmSettingsStore(
    private val secureStore: SecureStore = DesktopFileSecureStore(),
) : CloudLlmSettingsStore {
    private val prefs: Preferences = Preferences.userNodeForPackage(CloudLlmSettingsStore::class.java)

    override fun isEnabled(): Boolean = prefs.getBoolean(PREF_CLOUD_LLM_ENABLED, false)

    override fun setEnabled(enabled: Boolean) {
        prefs.putBoolean(PREF_CLOUD_LLM_ENABLED, enabled)
    }

    override fun apiKey(): String? = secureStore.get(SECURE_STORE_CLOUD_API_KEY)?.takeIf(String::isNotBlank)

    override fun setApiKey(key: String?) {
        if (key.isNullOrBlank()) {
            secureStore.remove(SECURE_STORE_CLOUD_API_KEY)
        } else {
            secureStore.put(SECURE_STORE_CLOUD_API_KEY, key)
        }
    }

    override fun model(): String = prefs.get(PREF_CLOUD_LLM_MODEL, null) ?: CloudLlmConfig.DEFAULT_MODEL

    override fun setModel(model: String) {
        prefs.put(PREF_CLOUD_LLM_MODEL, model)
    }

    override fun baseUrl(): String = prefs.get(PREF_CLOUD_LLM_BASE_URL, null) ?: CloudLlmConfig.DEFAULT_BASE_URL

    override fun setBaseUrl(url: String) {
        prefs.put(PREF_CLOUD_LLM_BASE_URL, url)
    }

    override fun presetId(): String = prefs.get(PREF_CLOUD_LLM_PRESET_ID, null) ?: ""

    override fun setPresetId(id: String) {
        prefs.put(PREF_CLOUD_LLM_PRESET_ID, id)
    }
}

/** Desktop [CloudLlmSettingsStore] factory. */
actual fun createPlatformCloudLlmSettings(): CloudLlmSettingsStore = DesktopCloudLlmSettingsStore()
