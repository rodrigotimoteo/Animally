package com.github.rodrigotimoteo.animally.domain.owner.usecase

import org.koin.core.annotation.Single

@Single
class GetOwnerDetailUseCase {
    operator fun invoke(id: Long) = Unit
}
