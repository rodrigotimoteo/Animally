package com.github.rodrigotimoteo.animally.di.presentation

import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.presentation.ownerDetail.OwnerDetailViewModel
import com.github.rodrigotimoteo.animally.presentation.ownerEdit.OwnerEditViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Module providing navigation-parameterized view models.
 */
@Module
internal class PresentationModule {
    fun provide() =
        module {
            viewModel { (ownerId: Long) ->
                OwnerDetailViewModel(
                    ownerId = ownerId,
                    getOwnerDetailUseCase = get(),
                    patientRepository = get(),
                    animallyNavigator = get(),
                    ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
                )
            }
            viewModel { (ownerId: Long?) ->
                OwnerEditViewModel(
                    ownerId = ownerId,
                    getOwnerDetailUseCase = get(),
                    saveOwnerUseCase = get(),
                    animallyNavigator = get(),
                    ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
                )
            }
        }
}
