package com.github.rodrigotimoteo.animally.data.common

/**
 * Maps a persistence [Db] row to its corresponding [Domain] model.
 */
fun interface DomainMapper<Db, Domain> {
    fun Db.toDomain(): Domain
}
