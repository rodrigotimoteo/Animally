package com.github.rodrigotimoteo.animally.presentation.ultrasound

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.data.storage.FileStorage
import com.github.rodrigotimoteo.animally.data.storage.PickedFile
import com.github.rodrigotimoteo.animally.data.storage.sanitizeFileName
import com.github.rodrigotimoteo.animally.data.storage.splitImageUris
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound
import com.github.rodrigotimoteo.animally.domain.ultrasound.usecase.GetUltrasoundDetailUseCase
import com.github.rodrigotimoteo.animally.domain.ultrasound.usecase.SaveUltrasoundUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the ultrasound add/edit form.
 *
 * @param patientId The id of the patient this ultrasound belongs to.
 * @param ultrasoundId The id of the ultrasound being edited, or `null` when creating a new one.
 * @param getUltrasoundDetailUseCase Use case for loading an existing ultrasound.
 * @param saveUltrasoundUseCase Use case for persisting the ultrasound.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 * @param saveFile Persists file bytes to storage and returns the absolute path.
 */
class UltrasoundEditViewModel(
    private val patientId: Long,
    private val ultrasoundId: Long?,
    private val getUltrasoundDetailUseCase: GetUltrasoundDetailUseCase,
    private val saveUltrasoundUseCase: SaveUltrasoundUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
    private val saveFile: (fileName: String, bytes: ByteArray) -> String = { fileName, bytes ->
        FileStorage.saveBytes(fileName, bytes)
    },
) : BaseAddEditViewModel<UltrasoundFormState>(animallyNavigator) {
    init {
        if (ultrasoundId != null) {
            loadUltrasound(ultrasoundId)
        } else {
            updateForm(UltrasoundFormState())
        }
    }

    private fun loadUltrasound(id: Long) {
        updateForm(UltrasoundFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getUltrasoundDetailUseCase(id) } }
                .onSuccess { ultrasound ->
                    if (ultrasound == null) {
                        updateForm(UltrasoundFormState(id = id, dateError = "Ultrasound not found"))
                    } else {
                        updateForm(
                            UltrasoundFormState(
                                id = ultrasound.id,
                                date = ultrasound.date.toString(),
                                ovaryStatus = ultrasound.ovaryStatus,
                                uterineStatus = ultrasound.uterineStatus,
                                follicleSizeMm = ultrasound.follicleSizeMm?.toString(),
                                findings = ultrasound.findings,
                                imageUris = ultrasound.imageUris,
                                vetName = ultrasound.vetName,
                                notes = ultrasound.notes,
                                createdAt = ultrasound.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(
                        UltrasoundFormState(
                            id = id,
                            dateError = error.message ?: "Failed to load ultrasound",
                        ),
                    )
                }
        }
    }

    /**
     * Updates the [UltrasoundFormState.date].
     */
    fun onDateChange(date: String) {
        formState.value?.let { updateForm(it.copy(date = date, dateError = null)) }
    }

    /**
     * Updates the [UltrasoundFormState.ovaryStatus].
     */
    fun onOvaryStatusChange(value: String) {
        formState.value?.let { updateForm(it.copy(ovaryStatus = value.ifBlank { null })) }
    }

    /**
     * Updates the [UltrasoundFormState.uterineStatus].
     */
    fun onUterineStatusChange(value: String) {
        formState.value?.let { updateForm(it.copy(uterineStatus = value.ifBlank { null })) }
    }

    /**
     * Updates the [UltrasoundFormState.follicleSizeMm].
     */
    fun onFollicleSizeMmChange(value: String) {
        formState.value
            ?.let { updateForm(it.copy(follicleSizeMm = value.ifBlank { null }, follicleSizeMmError = null)) }
    }

    /**
     * Updates the [UltrasoundFormState.findings].
     */
    fun onFindingsChange(value: String) {
        formState.value?.let { updateForm(it.copy(findings = value.ifBlank { null })) }
    }

    /**
     * Updates the [UltrasoundFormState.imageUris].
     */
    fun onImageUrisChange(value: String) {
        formState.value?.let { updateForm(it.copy(imageUris = value.ifBlank { null })) }
    }

    /**
     * Saves the picked [files] to storage and appends their paths to [UltrasoundFormState.imageUris].
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
     * Removes [uri] from [UltrasoundFormState.imageUris].
     *
     * @param uri The absolute path of the attached image to detach.
     */
    fun removeImageUri(uri: String) {
        val remaining = splitImageUris(formState.value?.imageUris).filterNot { it == uri }
        val imageUris = remaining.joinToString(",").ifBlank { null }
        formState.value?.let { updateForm(it.copy(imageUris = imageUris)) }
    }

    /**
     * Updates the [UltrasoundFormState.vetName].
     */
    fun onVetNameChange(value: String) {
        formState.value?.let { updateForm(it.copy(vetName = value.ifBlank { null })) }
    }

    /**
     * Updates the [UltrasoundFormState.notes].
     */
    fun onNotesChange(value: String) {
        formState.value?.let { updateForm(it.copy(notes = value.ifBlank { null })) }
    }

    /**
     * Validates and persists the current form.
     */
    override fun save() {
        val form = formState.value ?: return
        val date = parseDateOrNull(form.date)
        if (date == null) {
            val message = if (form.date.isBlank()) "Date is required" else "Invalid date (YYYY-MM-DD)"
            updateForm(form.copy(dateError = message))
            return
        }
        var follicleSizeMm: Double? = null
        if (!form.follicleSizeMm.isNullOrBlank()) {
            val parsed = form.follicleSizeMm.toDoubleOrNull()?.takeIf { it > 0.0 }
            if (parsed == null) {
                updateForm(form.copy(follicleSizeMmError = "Follicle size must be a positive number"))
                return
            }
            follicleSizeMm = parsed
        }
        viewModelScope.launch {
            updateForm(form.copy(isSaving = true))
            val now = Clock.System.now()
            val ultrasound =
                Ultrasound(
                    id = form.id ?: 0L,
                    patientId = patientId,
                    date = date,
                    ovaryStatus = form.ovaryStatus,
                    uterineStatus = form.uterineStatus,
                    follicleSizeMm = follicleSizeMm,
                    findings = form.findings,
                    imageUris = form.imageUris,
                    vetName = form.vetName,
                    notes = form.notes,
                    createdAt = form.createdAt ?: now,
                    updatedAt = now,
                )
            runCatching { withContext(ioDispatcher) { saveUltrasoundUseCase(ultrasound) } }
                .onSuccess {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    popBackStack()
                }.onFailure { error ->
                    formState.value?.let {
                        updateForm(
                            it.copy(
                                isSaving = false,
                                dateError = error.message ?: "Failed to save ultrasound",
                            ),
                        )
                    }
                }
        }
    }

    private fun parseDateOrNull(value: String?): LocalDate? {
        if (value == null) return null
        return runCatching { LocalDate.parse(value) }.getOrNull()
    }
}
