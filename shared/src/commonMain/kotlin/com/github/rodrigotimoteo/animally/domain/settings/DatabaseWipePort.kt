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
     * transaction and returns app-owned media paths that existed before the
     * wipe. Callers should delete the returned files and report any residuals.
     */
    fun clearAll(): Set<String>
}
