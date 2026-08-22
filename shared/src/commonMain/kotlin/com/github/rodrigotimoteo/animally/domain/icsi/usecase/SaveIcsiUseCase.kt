package com.github.rodrigotimoteo.animally.domain.icsi.usecase

import com.github.rodrigotimoteo.animally.domain.icsi.IIcsiRepository
import com.github.rodrigotimoteo.animally.domain.icsi.model.Icsi
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated ICSI record.
 *
 * Records with `id == 0L` are inserted, all others are updated.
 */
@Single
class SaveIcsiUseCase(
    @Provided private val repository: IIcsiRepository,
) {
    /**
     * Persists the given [record] and returns the generated identifier for new records.
     */
    operator fun invoke(record: Icsi): Long =
        if (record.id == 0L) {
            repository.insert(record)
        } else {
            repository.update(record)
            record.id
        }
}
