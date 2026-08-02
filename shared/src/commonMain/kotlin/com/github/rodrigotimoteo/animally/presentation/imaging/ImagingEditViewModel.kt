package com.github.rodrigotimoteo.animally.presentation.imaging

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.data.storage.FileStorage
import com.github.rodrigotimoteo.animally.data.storage.PickedFile
import com.github.rodrigotimoteo.animally.data.storage.sanitizeFileName
import com.github.rodrigotimoteo.animally.data.storage.splitImageUris
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
import com.github.rodrigotimoteo.animally.domain.imaging.usecase.GetImagingDetailUseCase
import com.github.rodrigotimoteo.animally.domain.imaging.usecase.SaveImagingUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the imaging add/edit form.
 *
 * @param patientId The id of the patient this imaging record belongs to.
 * @param imagingId The id of the imaging record being edited, or `null` when creating a new one.
 * @param getImagingDetailUseCase Use case for loading an existing imaging record.
 * @param saveImagingUseCase Use case for persisting the imaging record.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 * @param saveFile Persists file bytes to storage and returns the absolute path.
 */
class ImagingEditViewModel(
    private val patientId: Long,
    private val imagingId: Long?,
    private val getImagingDetailUseCase: GetImagingDetailUseCase,
    private val saveImagingUseCase: SaveImagingUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
    private val saveFile: (fileName: String, bytes: ByteArray) -> String = { fileName, bytes ->
        FileStorage.saveBytes(fileName, bytes)
    },
) : BaseAddEditViewModel<ImagingFormState>(animallyNavigator) {
    init {
        if (imagingId != null) {
            loadImaging(imagingId)
        } else {
            updateForm(ImagingFormState())
        }
    }

    private fun loadImaging(id: Long) {
        updateForm(ImagingFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getImagingDetailUseCase(id) } }
                .onSuccess { imaging ->
                    if (imaging == null) {
                        updateForm(ImagingFormState(id = id, dateError = "Imaging record not found"))
                    } else {
                        updateForm(
                            ImagingFormState(
                                id = imaging.id,
                                type = imaging.type,
                                date = imaging.date.toString(),
                                findings = imaging.findings,
                                imageUris = imaging.imageUris,
                                vetName = imaging.vetName,
                                notes = imaging.notes,
                                createdAt = imaging.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(
                        ImagingFormState(
                            id = id,
                            dateError = error.message ?: "Failed to load imaging record",
                        ),
                    )
                }
        }
    }

    /**
     * Updates the [ImagingFormState.type].
     */
    fun onTypeChange(value: String) {
        formState.value?.let { updateForm(it.copy(type = value, typeError = null)) }
    }

    /**
     * Updates the [ImagingFormState.date].
     */
    fun onDateChange(value: String) {
        formState.value?.let { updateForm(it.copy(date = value, dateError = null)) }
    }

    /**
     * Updates the [ImagingFormState.findings].
     */
    fun onFindingsChange(value: String) {
        formState.value?.let { updateForm(it.copy(findings = value.ifBlank { null })) }
    }

    /**
     * Updates the [ImagingFormState.imageUris].
     */
    fun onImageUrisChange(value: String) {
        formState.value?.let { updateForm(it.copy(imageUris = value.ifBlank { null })) }
    }

    /**
     * Saves the picked [files] to storage and appends their paths to [ImagingFormState.imageUris].
     *
     * @param files The files picked through the platform picker.
     */
    fun onFilesPicked(files: List<PickedFile>) {
        viewModelScope.launch {
            val savedPaths =
                withContext(ioDispatcher) {
                    buildList {
                        for (file in files) {
                            val bytes = file.readBytes()
                            add(saveFile(sanitizeFileName(file.name), bytes))
                        }
                    }
                }
            val merged = (splitImageUris(formState.value?.imageUris) + savedPaths).distinct()
            val imageUris = merged.joinToString(",").ifBlank { null }
            formState.value?.let { updateForm(it.copy(imageUris = imageUris)) }
        }
    }

    /**
     * Removes [uri] from [ImagingFormState.imageUris].
     *
     * @param uri The absolute path of the attached image to detach.
     */
    fun removeImageUri(uri: String) {
        val remaining = splitImageUris(formState.value?.imageUris).filterNot { it == uri }
        val imageUris = remaining.joinToString(",").ifBlank { null }
        formState.value?.let { updateForm(it.copy(imageUris = imageUris)) }
    }

    /**
     * Updates the [ImagingFormState.vetName].
     */
    fun onVetNameChange(value: String) {
        formState.value?.let { updateForm(it.copy(vetName = value.ifBlank { null })) }
    }

    /**
     * Updates the [ImagingFormState.notes].
     */
    fun onNotesChange(value: String) {
        formState.value?.let { updateForm(it.copy(notes = value.ifBlank { null })) }
    }

    /**
     * Validates and persists the current form.
     */
    override fun save() {
        val form = formState.value ?: return
        if (form.type.isBlank()) {
            updateForm(form.copy(typeError = "Type is required"))
            return
        }
        val date = parseDateOrNull(form.date)
        if (date == null) {
            val message = if (form.date.isBlank()) "Date is required" else "Invalid date (YYYY-MM-DD)"
            updateForm(form.copy(dateError = message))
            return
        }
        viewModelScope.launch {
            updateForm(form.copy(isSaving = true))
            val now = Clock.System.now()
            val imaging =
                Imaging(
                    id = form.id ?: 0L,
                    patientId = patientId,
                    type = form.type,
                    date = date,
                    findings = form.findings,
                    imageUris = form.imageUris,
                    vetName = form.vetName,
                    notes = form.notes,
                    createdAt = form.createdAt ?: now,
                    updatedAt = now,
                )
            runCatching { withContext(ioDispatcher) { saveImagingUseCase(imaging) } }
                .onSuccess {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    popBackStack()
                }.onFailure { error ->
                    formState.value?.let {
                        updateForm(
                            it.copy(
                                isSaving = false,
                                dateError = error.message ?: "Failed to save imaging record",
                            ),
                        )
                    }
                }
        }
    }

    private fun parseDateOrNull(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()
}
