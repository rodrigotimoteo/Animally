package com.github.rodrigotimoteo.animally.data.search.mapper

import com.github.rodrigotimoteo.animally.data.search.Search
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult

/**
 * Converts a persistence [Search] row to a domain [SearchResult].
 *
 * @return mapped [SearchResult]
 */
fun Search.toDomain(): SearchResult =
    SearchResult(
        patientId = patientId,
        patientName = patientName,
        breed = breed,
        microchipId = microchipId,
        recordType = recordType,
        recordId = recordId,
        date = date,
        snippet = searchableText,
    )
