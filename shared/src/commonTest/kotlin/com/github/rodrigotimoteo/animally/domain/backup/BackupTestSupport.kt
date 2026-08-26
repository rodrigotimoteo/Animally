package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.search.SearchRepositoryImpl

/** Builds the production restore boundary for backup round-trip tests. */
internal fun restoreBackupUseCase(database: AnimallyDatabase): RestoreBackupUseCase =
    RestoreBackupUseCase(
        database = database,
        searchRepository = SearchRepositoryImpl(database, database.ownerQueries),
    )
