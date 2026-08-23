package com.github.rodrigotimoteo.animally.presentation.ultrasound

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.data.storage.FileStorage
import com.github.rodrigotimoteo.animally.data.storage.PickedFile
import com.github.rodrigotimoteo.animally.data.storage.sanitizeFileName
import com.github.rodrigotimoteo.animally.data.storage.splitImageUris
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.follicle.model.Follicle
import com.github.rodrigotimoteo.animally.domain.follicle.usecase.GetFolliclesByUltrasoundUseCase
import com.github.rodrigotimoteo.animally.domain.follicle.usecase.SaveFolliclesUseCase
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
import kotlin.time.Instant

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
@Suppress("TooManyFunctions", "LongParameterList")
class UltrasoundEditViewModel(
    private val patientId: Long,
    private val ultrasoundId: Long?,
    private val getUltrasoundDetailUseCase: GetUltrasoundDetailUseCase,
    private val saveUltrasoundUseCase: SaveUltrasoundUseCase,
    private val getFolliclesByUltrasoundUseCase: GetFolliclesByUltrasoundUseCase,
    private val saveFolliclesUseCase: SaveFolliclesUseCase,
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
                                leftOvaryStatus = ultrasound.leftOvaryStatus,
                                rightOvaryStatus = ultrasound.rightOvaryStatus,
                                leftFollicleSizeMm = ultrasound.leftFollicleSizeMm?.toString(),
                                rightFollicleSizeMm = ultrasound.rightFollicleSizeMm?.toString(),
                                uterineEdema = ultrasound.uterineEdema,
                                uterineLiquid = ultrasound.uterineLiquid,
                                uterineLiquidDescription = ultrasound.uterineLiquidDescription,
                                uterusDescription = ultrasound.uterusDescription,
                                findings = ultrasound.findings,
                                imageUris = ultrasound.imageUris,
                                vetName = ultrasound.vetName,
                                notes = ultrasound.notes,
                                createdAt = ultrasound.createdAt,
                            ),
                        )
                        loadFollicleRows(ultrasound.id)
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
     * Updates the [UltrasoundFormState.leftOvaryStatus].
     */
    fun onLeftOvaryStatusChange(value: String) {
        formState.value?.let { updateForm(it.copy(leftOvaryStatus = value.ifBlank { null })) }
    }

    /**
     * Updates the [UltrasoundFormState.rightOvaryStatus].
     */
    fun onRightOvaryStatusChange(value: String) {
        formState.value?.let { updateForm(it.copy(rightOvaryStatus = value.ifBlank { null })) }
    }

    /**
     * Updates the [UltrasoundFormState.leftFollicleSizeMm].
     */
    fun onLeftFollicleSizeMmChange(value: String) {
        formState.value?.let { updateForm(it.copy(leftFollicleSizeMm = value.ifBlank { null })) }
    }

    /**
     * Updates the [UltrasoundFormState.rightFollicleSizeMm].
     */
    fun onRightFollicleSizeMmChange(value: String) {
        formState.value?.let { updateForm(it.copy(rightFollicleSizeMm = value.ifBlank { null })) }
    }

    /**
     * Updates the [UltrasoundFormState.uterineEdema].
     */
    fun onUterineEdemaChange(value: String) {
        formState.value?.let { updateForm(it.copy(uterineEdema = value.ifBlank { null })) }
    }

    /**
     * Updates the [UltrasoundFormState.uterineLiquid].
     */
    fun onUterineLiquidChange(value: Boolean?) {
        formState.value?.let { updateForm(it.copy(uterineLiquid = value)) }
    }

    /**
     * Updates the [UltrasoundFormState.uterineLiquidDescription].
     */
    fun onUterineLiquidDescriptionChange(value: String) {
        formState.value?.let { updateForm(it.copy(uterineLiquidDescription = value.ifBlank { null })) }
    }

    /**
     * Updates the [UltrasoundFormState.uterusDescription].
     */
    fun onUterusDescriptionChange(value: String) {
        formState.value?.let { updateForm(it.copy(uterusDescription = value.ifBlank { null })) }
    }

    /**
     * Adds an empty follicle row on the given ovary [side] (`LEFT`/`RIGHT`).
     */
    fun onAddFollicle(side: String) {
        formState.value?.let { current ->
            updateForm(
                if (side == Follicle.SIDE_LEFT) {
                    current.copy(leftFollicles = current.leftFollicles + FollicleRow())
                } else {
                    current.copy(rightFollicles = current.rightFollicles + FollicleRow())
                },
            )
        }
    }

    /**
     * Removes the follicle row at [index] on the given ovary [side].
     */
    fun onRemoveFollicle(
        side: String,
        index: Int,
    ) {
        formState.value?.let { current ->
            updateForm(
                if (side == Follicle.SIDE_LEFT) {
                    current.copy(leftFollicles = current.leftFollicles.filterIndexed { i, _ -> i != index })
                } else {
                    current.copy(rightFollicles = current.rightFollicles.filterIndexed { i, _ -> i != index })
                },
            )
        }
    }

    /**
     * Updates the size of the follicle row at [index] on the given ovary [side].
     */
    fun onFollicleSizeChange(
        side: String,
        index: Int,
        value: String,
    ) {
        formState.value?.let { current ->
            updateForm(
                if (side == Follicle.SIDE_LEFT) {
                    current.copy(leftFollicles = current.leftFollicles.withSizeAt(index, value))
                } else {
                    current.copy(rightFollicles = current.rightFollicles.withSizeAt(index, value))
                },
            )
        }
    }

    /**
     * Updates the description of the follicle row at [index] on the given ovary [side].
     */
    fun onFollicleDescriptionChange(
        side: String,
        index: Int,
        value: String,
    ) {
        formState.value?.let { current ->
            val updated =
                if (side == Follicle.SIDE_LEFT) {
                    current.copy(leftFollicles = current.leftFollicles.withDescriptionAt(index, value))
                } else {
                    current.copy(rightFollicles = current.rightFollicles.withDescriptionAt(index, value))
                }
            updateForm(updated)
        }
    }

    private fun List<FollicleRow>.withSizeAt(
        index: Int,
        value: String,
    ): List<FollicleRow> = mapIndexed { i, r -> if (i == index) r.copy(sizeMm = value) else r }

    private fun List<FollicleRow>.withDescriptionAt(
        index: Int,
        value: String,
    ): List<FollicleRow> = mapIndexed { i, r -> if (i == index) r.copy(note = value.ifBlank { null }) else r }

    private fun loadFollicleRows(ultrasoundId: Long) {
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getFolliclesByUltrasoundUseCase(ultrasoundId) } }
                .onSuccess { follicles ->
                    formState.value?.let { current ->
                        val left = follicles.filter { it.side == Follicle.SIDE_LEFT }.map { it.toRow() }
                        val right = follicles.filter { it.side == Follicle.SIDE_RIGHT }.map { it.toRow() }
                        updateForm(current.copy(leftFollicles = left, rightFollicles = right))
                    }
                }
        }
    }

    private fun Follicle.toRow(): FollicleRow =
        FollicleRow(
            id = id,
            sizeMm = sizeMm.toString(),
            note = description,
        )

    private fun FollicleRow.toDomain(
        ultrasoundId: Long,
        side: String,
        now: Instant,
    ): Follicle =
        Follicle(
            id = id,
            ultrasoundId = ultrasoundId,
            side = side,
            sizeMm = sizeMm.toDoubleOrNull() ?: 0.0,
            description = note,
            createdAt = now,
            updatedAt = now,
        )

    /** Replaces the stored follicle set of [ultrasoundId] with the rows currently in [form]. */
    private suspend fun persistFollicles(
        form: UltrasoundFormState,
        ultrasoundId: Long,
        now: Instant,
    ) {
        val follicles =
            form.leftFollicles.map { it.toDomain(ultrasoundId, Follicle.SIDE_LEFT, now) } +
                form.rightFollicles.map { it.toDomain(ultrasoundId, Follicle.SIDE_RIGHT, now) }
        runCatching { withContext(ioDispatcher) { saveFolliclesUseCase(ultrasoundId, follicles) } }
    }

    private fun buildUltrasound(
        form: UltrasoundFormState,
        date: LocalDate,
        follicleSizeMm: Double?,
        now: Instant,
    ): Ultrasound =
        Ultrasound(
            id = form.id ?: 0L,
            patientId = patientId,
            date = date,
            ovaryStatus = form.ovaryStatus,
            uterineStatus = form.uterineStatus,
            follicleSizeMm = follicleSizeMm,
            leftOvaryStatus = form.leftOvaryStatus,
            rightOvaryStatus = form.rightOvaryStatus,
            leftFollicleSizeMm = form.leftFollicleSizeMm?.toDoubleOrNull(),
            rightFollicleSizeMm = form.rightFollicleSizeMm?.toDoubleOrNull(),
            uterineEdema = form.uterineEdema,
            uterineLiquid = form.uterineLiquid,
            uterineLiquidDescription = form.uterineLiquidDescription,
            uterusDescription = form.uterusDescription,
            findings = form.findings,
            imageUris = form.imageUris,
            vetName = form.vetName,
            notes = form.notes,
            createdAt = form.createdAt ?: now,
            updatedAt = now,
        )

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
            val ultrasound = buildUltrasound(form, date, follicleSizeMm, now)
            runCatching { withContext(ioDispatcher) { saveUltrasoundUseCase(ultrasound) } }
                .onSuccess { savedId ->
                    persistFollicles(form, savedId, now)
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    emitSaved()
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
