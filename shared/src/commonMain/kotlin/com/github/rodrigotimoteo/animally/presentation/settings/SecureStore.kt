package com.github.rodrigotimoteo.animally.presentation.settings

/**
 * Storage for small secrets (API keys). Values must NEVER land in plaintext
 * preferences: Android backs this with an AndroidKeyStore AES-GCM cipher over
 * SharedPreferences, iOS with the Keychain (generic password items).
 *
 * Exposed as an interface so tests can substitute an in-memory fake.
 */
interface SecureStore {
    /** Returns the stored secret for [key], or null when absent/unreadable. */
    fun get(key: String): String?

    /** Stores [value] under [key], replacing any previous secret. */
    fun put(
        key: String,
        value: String,
    )

    /** Removes the secret stored under [key]. */
    fun remove(key: String)
}

/** Keychain/Keystore key under which the cloud LLM API key is stored. */
const val SECURE_STORE_CLOUD_API_KEY = "llm_cloud_key"

/**
 * Creates the platform-specific [SecureStore].
 *
 * Android actual uses AndroidKeyStore-encrypted SharedPreferences; iOS actual uses
 * the Keychain; desktop actual uses a permission-restricted file.
 */
expect fun createPlatformSecureStore(): SecureStore
