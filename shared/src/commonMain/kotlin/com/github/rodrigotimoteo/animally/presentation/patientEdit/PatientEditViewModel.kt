package com.github.rodrigotimoteo.animally.presentation.patientEdit

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.owner.usecase.GetOwnerListUseCase
import com.github.rodrigotimoteo.animally.domain.patient.usecase.GetPatientDetailUseCase
import com.github.rodrigotimoteo.animally.domain.patient.usecase.SavePatientUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the patient add/edit form.
 *
 * @param patientId The id of the patient being edited, or `null` when creating a new one.
 * @param getPatientDetailUseCase Use case for loading an existing patient.
 * @param savePatientUseCase Use case for persisting the patient.
 * @param getOwnerListUseCase Use case for loading owners for the owner selector.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class PatientEditViewModel(
    private val patientId: Long?,
    private val getPatientDetailUseCase: GetPatientDetailUseCase,
    private val savePatientUseCase: SavePatientUseCase,
    private val getOwnerListUseCase: GetOwnerListUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : BaseAddEditViewModel<PatientFormState>(animallyNavigator) {
    private val _owners = MutableStateFlow<List<Owner>>(emptyList())
    val owners: StateFlow<List<Owner>> = _owners.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getOwnerListUseCase() } }
                .onSuccess { _owners.value = it }
        }
        if (patientId != null) {
            loadPatient(patientId)
        } else {
            updateForm(PatientFormState())
        }
    }

    private fun loadPatient(id: Long) {
        updateForm(PatientFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getPatientDetailUseCase(id) } }
                .onSuccess { patient ->
                    if (patient == null) {
                        updateForm(PatientFormState(id = id, nameError = "Patient not found"))
                    } else {
                        updateForm(
                            PatientFormState(
                                id = patient.id,
                                name = patient.name,
                                species = patient.species,
                                breed = patient.breed,
                                dateOfBirth = patient.dateOfBirth?.toString(),
                                gender = patient.gender,
                                microchipId = patient.microchipId,
                                ueln = patient.ueln,
                                registrationNumber = patient.registrationNumber,
                                stableLocation = patient.stableLocation,
                                photoUri = patient.photoUri,
                                notes = patient.notes,
                                ownerId = patient.ownerId,
                                createdAt = patient.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(PatientFormState(id = id, nameError = error.message ?: "Failed to load patient"))
                }
        }
    }

    /**
     * Updates the [PatientFormState.name].
     */
    fun onNameChange(name: String) {
        formState.value?.let { updateForm(it.copy(name = name, nameError = null)) }
    }

    /**
     * Updates the [PatientFormState.species].
     */
    fun onSpeciesChange(species: String) {
        formState.value?.let { updateForm(it.copy(species = species)) }
    }

    /**
     * Updates the [PatientFormState.breed].
     */
    fun onBreedChange(breed: String) {
        formState.value?.let { updateForm(it.copy(breed = breed.ifBlank { null })) }
    }

    /**
     * Updates the [PatientFormState.dateOfBirth].
     */
    fun onDateOfBirthChange(dateOfBirth: String) {
        formState.value?.let { updateForm(it.copy(dateOfBirth = dateOfBirth.ifBlank { null })) }
    }

    /**
     * Updates the [PatientFormState.gender].
     */
    fun onGenderChange(gender: String) {
        formState.value?.let { updateForm(it.copy(gender = gender.ifBlank { null })) }
    }

    /**
     * Updates the [PatientFormState.microchipId].
     */
    fun onMicrochipIdChange(microchipId: String) {
        formState.value?.let { updateForm(it.copy(microchipId = microchipId.ifBlank { null })) }
    }

    /**
     * Updates the [PatientFormState.ueln].
     */
    fun onUelnChange(ueln: String) {
        formState.value?.let { updateForm(it.copy(ueln = ueln.ifBlank { null }, uelnError = null)) }
    }

    /**
     * Updates the [PatientFormState.registrationNumber].
     */
    fun onRegistrationNumberChange(registrationNumber: String) {
        formState.value?.let { updateForm(it.copy(registrationNumber = registrationNumber.ifBlank { null })) }
    }

    /**
     * Updates the [PatientFormState.stableLocation].
     */
    fun onStableLocationChange(stableLocation: String) {
        formState.value?.let { updateForm(it.copy(stableLocation = stableLocation.ifBlank { null })) }
    }

    /**
     * Updates the [PatientFormState.photoUri].
     */
    fun onPhotoUriChange(photoUri: String) {
        formState.value?.let { updateForm(it.copy(photoUri = photoUri.ifBlank { null })) }
    }

    /**
     * Updates the [PatientFormState.notes].
     */
    fun onNotesChange(notes: String) {
        formState.value?.let { updateForm(it.copy(notes = notes.ifBlank { null })) }
    }

    /**
     * Updates the [PatientFormState.ownerId].
     */
    fun onOwnerChange(ownerId: Long?) {
        formState.value?.let { updateForm(it.copy(ownerId = ownerId)) }
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
        val uelnError = validateUeln(form.ueln?.trim())
        if (uelnError != null) {
            updateForm(form.copy(uelnError = uelnError))
            return
        }
        viewModelScope.launch {
            updateForm(form.copy(isSaving = true))
            val patient = buildPatient(form, Clock.System.now())
            runCatching { withContext(ioDispatcher) { savePatientUseCase(patient) } }
                .onSuccess {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    popBackStack()
                }.onFailure { error ->
                    formState.value?.let {
                        updateForm(it.copy(isSaving = false, nameError = error.message ?: "Failed to save patient"))
                    }
                }
        }
    }
}
