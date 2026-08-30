package com.github.rodrigotimoteo.animally.data.common

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import kotlin.time.Instant

/**
 * Generic scaffolding for patient-bound repositories.
 *
 * Collapses ~15 cookie-cutter repos that share getByPatient / getById / setInactive / insert / update.
 * Excluded: Anamnese, CustomReminder, Follicle (specialized queries / retention / non-standard scoping)
 * and Owner/Patient (not patient-bound). Count is ~15, not 18 — the 18 estimate double-counts excluded types.
 * Subclasses supply SQL delegates and field mappings; this base handles mapping, transaction and
 * inactive boilerplate.
 *
 * Sorting: repos that apply `sortedByDescending` after `getByPatient` (Weight by date, Medication by
 * startDate, Consultation/Gestation/Imaging/etc. by date) must override [getByPatient] in the subclass
 * or add `ORDER BY` in their `.sq` when migrated. The base returns insertion-query order (unsorted) so
 * collapsing a sorting repo without an override would silently regress ordering. Pilot rollout
 * (Vaccination, Deworming, Dentistry) is unaffected — none sort in their current `getByPatient`.
 *
 * NOTE bulk rollout: audit each sorting repo for `sortedByDescending` / `ORDER BY` before collapsing;
 * prefer `ORDER BY date DESC` in SQL over in-memory sorting when migrating.
 */
abstract class BasePatientRepository<Db : Any, Domain : Any>(
    protected val database: AnimallyDatabase,
    private val mapper: DomainMapper<Db, Domain>,
) {
    protected abstract fun selectByPatient(patientId: Long): Query<Db>

    protected abstract fun selectById(id: Long): Query<Db>

    protected abstract fun doSetInactive(
        id: Long,
        updatedAt: Instant,
    ): QueryResult<Long>

    protected abstract fun doInsert(domain: Domain): QueryResult<Long>

    protected abstract fun doUpdate(domain: Domain): QueryResult<Long>

    open fun getByPatient(patientId: Long): List<Domain> = selectByPatient(patientId).executeAsList().mapWith(mapper)

    open fun getById(id: Long): Domain? = selectById(id).executeAsOneOrNull()?.let { with(mapper) { it.toDomain() } }

    open fun insert(domain: Domain): Long =
        database.transactionWithResult {
            doInsert(domain)
            database.commonQueries.selectLastRowId().executeAsOne()
        }

    open fun update(domain: Domain): Long = doUpdate(domain).value

    open fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = doSetInactive(id, updatedAt).value
}
