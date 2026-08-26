package com.github.rodrigotimoteo.animally.domain.embryotransfer.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.embryotransfer.IEmbryoTransferRepository
import com.github.rodrigotimoteo.animally.domain.embryotransfer.model.EmbryoTransfer
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.SearchableText
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated embryo transfer record.
 *
 * Records with `id == 0L` are inserted, all others are updated.
 */
@Single
class SaveEmbryoTransferUseCase(
    @Provided private val repository: IEmbryoTransferRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Persists the given [record] and returns the generated identifier for new records.
     */
    operator fun invoke(record: EmbryoTransfer): Long {
        val savedId =
            if (record.id == 0L) {
                repository.insert(record)
            } else {
                repository.update(record)
                record.id
            }
        searchRepository.indexRecord(
            recordType = RecordType.EmbryoTransfer.wireName,
            patientId = record.patientId,
            recordId = savedId,
            date = record.date,
            searchableText = SearchableText.embryoTransfer(record),
        )
        return savedId
    }
}
