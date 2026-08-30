package com.github.rodrigotimoteo.animally.data.common

/**
 * Maps a persistence [Db] row to its corresponding [Domain] model.
 */
fun interface DomainMapper<Db, Domain> {
    fun Db.toDomain(): Domain
}

/**
 * Maps each [Db] element to [Domain] using [mapper].
 */
@Suppress("MaxLineLength", "Wrapping")
fun <Db, Domain> List<Db>.mapWith(mapper: DomainMapper<Db, Domain>): List<Domain> = map { db -> with(mapper) { db.toDomain() } }
