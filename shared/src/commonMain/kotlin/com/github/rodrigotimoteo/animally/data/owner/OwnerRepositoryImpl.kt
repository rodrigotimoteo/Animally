package com.github.rodrigotimoteo.animally.data.owner

import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import org.koin.core.annotation.Single

@Single(binds = [IOwnerRepository::class])
class OwnerRepositoryImpl : IOwnerRepository
