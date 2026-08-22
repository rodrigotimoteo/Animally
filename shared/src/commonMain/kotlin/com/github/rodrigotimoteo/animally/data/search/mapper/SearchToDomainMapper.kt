package com.github.rodrigotimoteo.animally.data.search.mapper

import com.github.rodrigotimoteo.animally.data.search.Search
import com.github.rodrigotimoteo.animally.data.search.SearchOwners
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

/**
 * Converts an owner hit from [SearchOwners] to a domain [SearchResult].
 *
 * Owner rows carry no patient linkage: [SearchResult.patientId] mirrors the
 * owner id so consumers can navigate by a single identifier field, and
 * [SearchResult.patientName] carries the owner display name.
 *
 * @return mapped [SearchResult]
 */
fun SearchOwners.toDomain(): SearchResult =
    SearchResult(
        patientId = ownerId,
        patientName = ownerName,
        breed = null,
        microchipId = null,
        recordType = "OWNER",
        recordId = recordId,
        date = null,
        snippet = searchableText,
    )
