package com.github.rodrigotimoteo.animally.domain.settings

/**
 * Domain port for wiping all persisted data.
 *
 * The data layer provides the SQLDelight implementation so
 * [com.github.rodrigotimoteo.animally.domain.settings.usecase.WipeAllDataUseCase]
 * no longer depends on `data.AnimallyDatabase` directly.
 */
interface DatabaseWipePort {
    /**
     * Clears every persisted table and both halves of the FTS index inside one
     * transaction and returns the dictation audio paths that existed before the
     * wipe. Callers should best-effort delete the returned files afterwards.
     */
    fun clearAll(): Set<String>
}
