package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.backup.BackupStorage
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Result of an export, with the paths of both artifacts.
 *
 * @property backupPath absolute path of the written JSON backup file.
 * @property dbCopyPath absolute path of the raw database copy.
 * @property fileName the backup file name, e.g. `backup_2026-08-02.json`.
 */
data class BackupResult(
    val backupPath: String,
    val dbCopyPath: String,
    val fileName: String,
)

/**
 * Reads the full database state and writes a dual-format backup: a
 * pretty-printed JSON payload (via [BackupSerializer]) plus a raw copy of the
 * SQLDelight database file.
 *
 * @param database the database to snapshot.
 * @param writeFile writes the JSON document and returns its absolute path;
 * defaults to [BackupStorage.writeTextFile].
 * @param copyDatabase copies the raw database file and returns its absolute
 * path; defaults to [BackupStorage.copyDatabaseFile].
 */
@Single
class ExportBackupUseCase(
    @Provided private val database: AnimallyDatabase,
    private val writeFile: (fileName: String, content: String) -> String =
        { fileName, content -> BackupStorage.writeTextFile(fileName, content) },
    private val copyDatabase: () -> String = { BackupStorage.copyDatabaseFile() },
) {
    /**
     * Exports the current database state and returns the artifact paths.
     */
    operator fun invoke(): BackupResult {
        val payload = buildPayload()
        val json = BackupSerializer.encode(payload)
        val fileName = "backup_${dateStamp()}.json"
        val backupPath = writeFile(fileName, json)
        val dbCopyPath = copyDatabase()
        return BackupResult(backupPath = backupPath, dbCopyPath = dbCopyPath, fileName = fileName)
    }

    private fun buildPayload(): BackupPayload =
        BackupPayload(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            exportedAt = Clock.System.now().toString(),
            patients = database.patientRows(),
            owners = database.ownerRows(),
            anamnese = database.anamneseRows(),
            consultations = database.consultationRows(),
            vaccinations = database.vaccinationRows(),
            weights = database.weightRows(),
            dewormings = database.dewormingRows(),
            dentistry = database.dentistryRows(),
            lameness = database.lamenessRows(),
            surgeries = database.surgeryRows(),
            medications = database.medicationRows(),
            labResults = database.labResultRows(),
            imaging = database.imagingRows(),
            farrierVisits = database.farrierVisitRows(),
            reproductionEvents = database.reproductionRows(),
            ultrasounds = database.ultrasoundRows(),
            gestations = database.gestationRows(),
            reproMedications = database.reproMedicationRows(),
            substances = database.substanceRows(),
        )

    private fun dateStamp(): String = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
}
