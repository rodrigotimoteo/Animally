package com.github.rodrigotimoteo.animally.presentation.settings

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.edit
import com.github.rodrigotimoteo.animally.di.infra.appContext
import com.github.rodrigotimoteo.animally.llm.cloud.CloudLlmConfig
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android [SecureStore]: secrets are AES-GCM encrypted with a non-exportable
 * AndroidKeyStore key before landing in an app-private SharedPreferences file.
 * (Equivalent guarantee to EncryptedSharedPreferences without the extra
 * dependency; the Keystore key is destroyed on uninstall, so stale ciphertexts
 * decrypt to nothing and read as absent.)
 *
 * Stored format: base64(iv):base64(ciphertext).
 */
class AndroidKeystoreSecureStore(
    context: Context,
) : SecureStore {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun get(key: String): String? {
        val raw = prefs.getString(key, null) ?: return null
        val parts = raw.split(SEPARATOR, limit = 2)
        if (parts.size != 2) return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, decode(parts[0])))
            String(cipher.doFinal(decode(parts[1])), Charsets.UTF_8)
        } catch (_: Exception) {
            // Unreadable entry (e.g. Keystore reset by reinstall) counts as absent.
            remove(key)
            null
        }
    }

    override fun put(
        key: String,
        value: String,
    ) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(value.encodeToByteArray())
        prefs.edit { putString(key, encode(iv) + SEPARATOR + encode(ciphertext)) }
    }

    override fun remove(key: String) {
        prefs.edit { remove(key) }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec
                .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)

    private companion object {
        const val PREFS_NAME = "animally_secure_prefs"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "animally_secure_store"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val KEY_SIZE_BITS = 256
        const val SEPARATOR = ":"
    }
}

/** Android [SecureStore] factory backed by [AndroidKeystoreSecureStore]. */
actual fun createPlatformSecureStore(): SecureStore = AndroidKeystoreSecureStore(appContext)

/**
 * Android [CloudLlmSettingsStore]: non-secret fields in `animally_preferences`,
 * the API key via [AndroidKeystoreSecureStore].
 */
class AndroidCloudLlmSettingsStore(
    context: Context,
    private val secureStore: SecureStore = AndroidKeystoreSecureStore(context),
) : CloudLlmSettingsStore {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun isEnabled(): Boolean = prefs.getBoolean(PREF_CLOUD_LLM_ENABLED, false)

    override fun setEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(PREF_CLOUD_LLM_ENABLED, enabled) }
    }

    override fun apiKey(): String? = secureStore.get(SECURE_STORE_CLOUD_API_KEY)?.takeIf(String::isNotBlank)

    override fun setApiKey(key: String?) {
        if (key.isNullOrBlank()) {
            secureStore.remove(SECURE_STORE_CLOUD_API_KEY)
        } else {
            secureStore.put(SECURE_STORE_CLOUD_API_KEY, key)
        }
    }

    override fun model(): String = prefs.getString(PREF_CLOUD_LLM_MODEL, null) ?: CloudLlmConfig.DEFAULT_MODEL

    override fun setModel(model: String) {
        prefs.edit { putString(PREF_CLOUD_LLM_MODEL, model) }
    }

    override fun baseUrl(): String = prefs.getString(PREF_CLOUD_LLM_BASE_URL, null) ?: CloudLlmConfig.DEFAULT_BASE_URL

    override fun setBaseUrl(url: String) {
        prefs.edit { putString(PREF_CLOUD_LLM_BASE_URL, url) }
    }

    override fun presetId(): String = prefs.getString(PREF_CLOUD_LLM_PRESET_ID, null) ?: ""

    override fun setPresetId(id: String) {
        prefs.edit { putString(PREF_CLOUD_LLM_PRESET_ID, id) }
    }

    private companion object {
        const val PREFS_NAME = "animally_preferences"
    }
}

/** Android [CloudLlmSettingsStore] factory. */
actual fun createPlatformCloudLlmSettings(): CloudLlmSettingsStore = AndroidCloudLlmSettingsStore(appContext)
