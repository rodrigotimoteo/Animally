package com.github.rodrigotimoteo.animally.presentation.settings

import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataGetTypeID
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFGetTypeID
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Foundation.NSLock
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

/**
 * iOS [SecureStore] backed by the Keychain (generic password items) through the
 * Security framework cinterop — pure Kotlin, no Swift shim required. The item's
 * account attribute carries the [SecureStore] key; values are UTF-8 data.
 */
@OptIn(ExperimentalForeignApi::class)
class IosKeychainSecureStore : SecureStore {
    // Key names are a fixed handful of constants (currently exactly one), so the
    // CFStrings created for them are cached instead of released per call.
    private val keyNameCache = mutableMapOf<String, CFStringRef>()
    private val keyNameCacheLock = NSLock()

    // An unsigned simulator build cannot access the Keychain (errSecMissingEntitlement).
    // Keep a process-local value so settings edits remain usable without ever falling
    // back to plaintext preferences. This fallback is shared because the settings
    // screen and cloud engine may receive different SecureStore instances from DI.
    // A signed app persists through the Keychain.
    private companion object {
        val sessionFallback = mutableMapOf<String, String>()
        val sessionFallbackLock = NSLock()
    }

    override fun get(key: String): String? {
        val query = query(account = cachedKeyName(key)) { it.setValue(kSecReturnData, kCFBooleanTrue) }
        return memScoped {
            val result = alloc<CFTypeRefVar>()
            if (SecItemCopyMatching(query, result.ptr) != errSecSuccess) {
                return@memScoped sessionFallbackValue(key)
            }
            val value = result.value ?: return@memScoped null
            try {
                if (CFGetTypeID(value) != CFDataGetTypeID()) return@memScoped null
                val data: CFDataRef = value.reinterpret()
                val length = CFDataGetLength(data).toInt()
                if (length <= 0) {
                    sessionFallbackValue(key)
                } else {
                    val out = ByteArray(length)
                    memcpy(out.refTo(0), CFDataGetBytePtr(data), length.convert())
                    out.decodeToString()
                }
            } finally {
                CFRelease(value)
            }
        }
    }

    override fun put(
        key: String,
        value: String,
    ) {
        setSessionFallbackValue(key, value)
        try {
            memScoped {
                val bytes = value.encodeToByteArray()
                if (bytes.isEmpty()) return@memScoped
                val data =
                    bytes.toUByteArray().usePinned { pinned ->
                        CFDataCreate(null, pinned.addressOf(0), bytes.size.convert())
                    } ?: return@memScoped
                try {
                    val base = query(account = cachedKeyName(key))
                    // Update first so repeated saves don't duplicate items; when the item
                    // does not exist yet, clear any stale entry and add fresh.
                    if (
                        SecItemUpdate(base, attributes { it.setValue(kSecValueData, data) }) == errSecSuccess
                    ) {
                        return@memScoped
                    }
                    SecItemDelete(base)
                    SecItemAdd(base.withValue(kSecValueData, data), null)
                } finally {
                    CFRelease(data)
                }
            }
        } catch (_: Throwable) {
            // Keychain access can fail in unsigned simulator/test builds. The
            // process-local value above keeps the current session functional.
        }
    }

    override fun remove(key: String) {
        removeSessionFallbackValue(key)
        SecItemDelete(query(account = cachedKeyName(key)))
    }

    private fun sessionFallbackValue(key: String): String? {
        sessionFallbackLock.lock()
        return try {
            sessionFallback[key]
        } finally {
            sessionFallbackLock.unlock()
        }
    }

    private fun setSessionFallbackValue(
        key: String,
        value: String,
    ) {
        sessionFallbackLock.lock()
        try {
            sessionFallback[key] = value
        } finally {
            sessionFallbackLock.unlock()
        }
    }

    private fun removeSessionFallbackValue(key: String) {
        sessionFallbackLock.lock()
        try {
            sessionFallback.remove(key)
        } finally {
            sessionFallbackLock.unlock()
        }
    }

    /** Builds the standard class+account query dictionary, extended by [configure]. */
    private inline fun query(
        account: CFStringRef?,
        configure: (CFMutableDictionaryRef) -> Unit = {},
    ): CFDictionaryRef {
        val dict =
            CFDictionaryCreateMutable(null, 0, null, null)
                ?: error("Keychain: failed to create query dictionary")
        configure(dict)
        // No retain/copy callbacks were installed, so only immortal constants
        // (kSecClass*, cached key names, kCFBooleanTrue) are ever stored.
        CFDictionarySetValue(dict, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(dict, kSecAttrAccount, account)
        return dict
    }

    private inline fun attributes(configure: (CFMutableDictionaryRef) -> Unit): CFDictionaryRef {
        val dict =
            CFDictionaryCreateMutable(null, 0, null, null)
                ?: error("Keychain: failed to create attributes dictionary")
        configure(dict)
        return dict
    }

    private fun CFMutableDictionaryRef.setValue(
        key: CFStringRef?,
        value: CValuesRef<*>?,
    ) {
        CFDictionarySetValue(this, key, value)
    }

    private fun CFDictionaryRef.withValue(
        key: CFStringRef?,
        value: CValuesRef<*>?,
    ): CFDictionaryRef {
        CFDictionarySetValue(this, key, value)
        return this
    }

    private fun cachedKeyName(key: String): CFStringRef {
        keyNameCacheLock.lock()
        return try {
            keyNameCache.getOrPut(key) {
                CFStringCreateWithCString(null, key, kCFStringEncodingUTF8)
                    ?: error("Keychain: failed to create key name")
            }
        } finally {
            keyNameCacheLock.unlock()
        }
    }
}
