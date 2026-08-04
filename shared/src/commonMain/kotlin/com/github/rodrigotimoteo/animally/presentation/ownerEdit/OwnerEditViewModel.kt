package com.github.rodrigotimoteo.animally.presentation.ownerEdit

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.owner.usecase.GetOwnerDetailUseCase
import com.github.rodrigotimoteo.animally.domain.owner.usecase.SaveOwnerUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the owner add/edit form.
 *
 * @param ownerId The id of the owner being edited, or `null` when creating a new one.
 * @param getOwnerDetailUseCase Use case for loading an existing owner.
 * @param saveOwnerUseCase Use case for persisting the owner.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class OwnerEditViewModel(
    private val ownerId: Long?,
    private val getOwnerDetailUseCase: GetOwnerDetailUseCase,
    private val saveOwnerUseCase: SaveOwnerUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : BaseAddEditViewModel<OwnerFormState>(animallyNavigator) {
    init {
        if (ownerId != null) {
            loadOwner(ownerId)
        } else {
            updateForm(OwnerFormState())
        }
    }

    private fun loadOwner(id: Long) {
        updateForm(OwnerFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getOwnerDetailUseCase(id) } }
                .onSuccess { owner ->
                    if (owner == null) {
                        updateForm(OwnerFormState(id = id, nameError = "Owner not found"))
                    } else {
                        updateForm(
                            OwnerFormState(
                                id = owner.id,
                                name = owner.name,
                                phone = owner.phone,
                                email = owner.email,
                                address = owner.address,
                                createdAt = owner.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(OwnerFormState(id = id, nameError = error.message ?: "Failed to load owner"))
                }
        }
    }

    /**
     * Updates the [OwnerFormState.name].
     */
    fun onNameChange(name: String) {
        formState.value?.let { updateForm(it.copy(name = name, nameError = null)) }
    }

    /**
     * Updates the [OwnerFormState.phone].
     */
    fun onPhoneChange(phone: String) {
        formState.value?.let { updateForm(it.copy(phone = phone.ifBlank { null })) }
    }

    /**
     * Updates the [OwnerFormState.email].
     */
    fun onEmailChange(email: String) {
        formState.value?.let { updateForm(it.copy(email = email.ifBlank { null })) }
    }

    /**
     * Updates the [OwnerFormState.address].
     */
    fun onAddressChange(address: String) {
        formState.value?.let { updateForm(it.copy(address = address.ifBlank { null })) }
    }

    /**
     * Validates and persists the current form.
     */
    override fun save() {
        val form = formState.value ?: return
        if (form.name.isBlank()) {
            updateForm(form.copy(nameError = "Name is required"))
            return
        }
        viewModelScope.launch {
            updateForm(form.copy(isSaving = true))
            val now = Clock.System.now()
            val owner =
                Owner(
                    id = form.id ?: 0L,
                    name = form.name.trim(),
                    phone = form.phone,
                    email = form.email,
                    address = form.address,
                    createdAt = form.createdAt ?: now,
                    updatedAt = now,
                )
            runCatching { withContext(ioDispatcher) { saveOwnerUseCase(owner) } }
                .onSuccess {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    emitSaved()
                }.onFailure { error ->
                    formState.value?.let {
                        updateForm(it.copy(isSaving = false, nameError = error.message ?: "Failed to save owner"))
                    }
                }
        }
    }
}
