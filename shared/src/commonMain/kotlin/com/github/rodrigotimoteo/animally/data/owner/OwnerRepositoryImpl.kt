package com.github.rodrigotimoteo.animally.data.owner

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.owner.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

@Single(binds = [IOwnerRepository::class])
class OwnerRepositoryImpl(
    @Provided private val ownerQueries: OwnerQueries,
    @Provided private val database: AnimallyDatabase,
) : IOwnerRepository {
    override fun getOwnerList(): List<Owner> = ownerQueries.selectAll().executeAsList().map { it.toDomain() }

    override fun getOwnerById(id: Long): Owner? = ownerQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insertOwner(owner: Owner): Long =
        database.transactionWithResult {
            ownerQueries
                .insert(
                    name = owner.name,
                    email = owner.email,
                    phone = owner.phone,
                    address = owner.address,
                    isActive = owner.isActive,
                    createdAt = owner.createdAt,
                    updatedAt = owner.updatedAt,
                )
            database.commonQueries.selectLastRowId().executeAsOne()
        }

    override fun updateOwner(owner: Owner): Long =
        ownerQueries
            .update(
                id = owner.id,
                name = owner.name,
                email = owner.email,
                phone = owner.phone,
                address = owner.address,
                isActive = owner.isActive,
                updatedAt = owner.updatedAt,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        ownerQueries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
