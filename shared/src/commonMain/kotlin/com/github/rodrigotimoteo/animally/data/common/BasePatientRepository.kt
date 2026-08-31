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
 * and Owner/Patient (not patient-bound).
 * Subclasses supply SQL delegates and field mappings; this base handles mapping, transaction and
 * inactive boilerplate. Ordering is enforced via `ORDER BY` in each `.sq` `selectByPatient`
 * (e.g. `ORDER BY date DESC`), avoiding in-memory `sortedByDescending`.
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

    open fun getByPatient(patientId: Long): List<Domain> =
        selectByPatient(patientId).executeAsList().map { db ->
            with(mapper) { db.toDomain() }
        }

    open fun getById(id: Long): Domain? =
        selectById(id).executeAsOneOrNull()?.let {
            with(mapper) { it.toDomain() }
        }

    protected fun insertDomain(domain: Domain): Long =
        database.transactionWithResult {
            doInsert(domain)
            database.commonQueries.selectLastRowId().executeAsOne()
        }

    protected fun updateDomain(domain: Domain): Long = doUpdate(domain).value

    open fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = doSetInactive(id, updatedAt).value
}
