package com.github.rodrigotimoteo.animally.data.insights

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.AnimallyDatabaseFactory
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * M6 perf gate: verify insights UNION ALL queries are sargable / indexed.
 * Uses EXPLAIN QUERY PLAN via driver.executeQuery.
 * Scoped variants must SEARCH via idx_*_patient_date; global variants must not SCAN TABLE
 * on activity tables (may SCAN INDEX covering).
 */
class InsightsQueryPlanTest {
    private lateinit var driver: SqlDriver
    private lateinit var database: AnimallyDatabase

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AnimallyDatabase.Schema.create(driver)
        database = AnimallyDatabaseFactory.create(driver)
    }

    private fun plan(sql: String): List<String> {
        val out = mutableListOf<String>()
        driver.executeQuery(null, sql, { cursor ->
            while (cursor.next().value) {
                out += cursor.getString(3) ?: ""
            }
            QueryResult.Value(Unit)
        }, 0, null)
        return out
    }

    private val activityTables =
        listOf(
            "Consultation",
            "Dentistry",
            "Deworming",
            "FarrierVisit",
            "Imaging",
            "LabResult",
            "Lameness",
            "Medication",
            "Reproduction",
            "ReproMedication",
            "Surgery",
            "Substance",
            "Ultrasound",
            "Vaccination",
            "Weight",
            "EmbryoTransfer",
            "Icsi",
        )

    private fun assertNoScanTable(plan: List<String>) {
        for (t in activityTables) {
            assertFalse(
                plan.any { it.contains("SCAN TABLE $t") },
                "unexpected SCAN TABLE $t in plan:\n${plan.joinToString("\n")}",
            )
        }
    }

    private fun assertSearchForScoped(plan: List<String>) {
        // At least 17 SEARCH lines, each using our composite index (covering variant)
        assertNoScanTable(plan)
        val searchLines = plan.filter { it.contains("SEARCH") }
        assertTrue(
            searchLines.size >= 17,
            "expected >=17 SEARCH lines, got ${searchLines.size}\n${plan.joinToString("\n")}",
        )
        for (t in activityTables) {
            val expectedIdx =
                when (t) {
                    "FarrierVisit" -> "idx_farriervisit_patient_date"
                    "LabResult" -> "idx_labresult_patient_date"
                    "ReproMedication" -> "idx_repromedication_patient_date"
                    "EmbryoTransfer" -> "idx_embryotransfer_patient_date"
                    else -> "idx_${t.lowercase()}_patient_date"
                }
            assertTrue(
                plan.any { it.contains(t) && it.contains(expectedIdx) && it.contains("SEARCH") },
                "expected SEARCH $t via $expectedIdx in plan:\n${plan.joinToString("\n")}",
            )
        }
    }

    private fun bucketsScopedSql(): String =
        """
        EXPLAIN QUERY PLAN WITH activity_rows AS (
          SELECT date AS activityDate, patientId, 'CONSULTATION' AS recordType FROM Consultation WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
          UNION ALL SELECT date, patientId, 'DENTISTRY' FROM Dentistry WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
          UNION ALL SELECT dateAdministered, patientId, 'DEWORMING' FROM Deworming WHERE isActive = 1 AND dateAdministered BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
          UNION ALL SELECT date, patientId, 'FARRIER_VISIT' FROM FarrierVisit WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
          UNION ALL SELECT date, patientId, 'IMAGING' FROM Imaging WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
          UNION ALL SELECT date, patientId, 'LAB_RESULT' FROM LabResult WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
          UNION ALL SELECT date, patientId, 'LAMENESS' FROM Lameness WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
          UNION ALL SELECT startDate, patientId, 'MEDICATION' FROM Medication WHERE isActive = 1 AND startDate IS NOT NULL AND startDate BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
          UNION ALL SELECT date, patientId, 'REPRODUCTION_EVENT' FROM Reproduction WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
          UNION ALL SELECT dateAdministered, patientId, 'REPRO_MEDICATION' FROM ReproMedication WHERE isActive = 1 AND dateAdministered BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
          UNION ALL SELECT date, patientId, 'SURGERY' FROM Surgery WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
          UNION ALL SELECT date, patientId, 'CONTROLLED_SUBSTANCE' FROM Substance WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
          UNION ALL SELECT date, patientId, 'ULTRASOUND' FROM Ultrasound WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
          UNION ALL SELECT dateAdministered, patientId, 'VACCINATION' FROM Vaccination WHERE isActive = 1 AND dateAdministered BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
          UNION ALL SELECT date, patientId, 'WEIGHT' FROM Weight WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
          UNION ALL SELECT date, patientId, 'EMBRYO_TRANSFER' FROM EmbryoTransfer WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
          UNION ALL SELECT date, patientId, 'ICSI' FROM Icsi WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
        )
        SELECT activityDate AS date, activity_rows.patientId AS patientId, recordType AS recordTypeWireName, COUNT(*) AS cnt
        FROM activity_rows
        JOIN Patient ON Patient.id = activity_rows.patientId
        WHERE Patient.isActive = 1
        GROUP BY activityDate, activity_rows.patientId, recordType
        """.trimIndent()

    private fun bucketsGlobalSql(): String =
        """
        EXPLAIN QUERY PLAN WITH activity_rows AS (
          SELECT date AS activityDate, patientId, 'CONSULTATION' AS recordType FROM Consultation WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
          UNION ALL SELECT date, patientId, 'DENTISTRY' FROM Dentistry WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
          UNION ALL SELECT dateAdministered, patientId, 'DEWORMING' FROM Deworming WHERE isActive = 1 AND dateAdministered BETWEEN '2025-01-10' AND '2025-01-20'
          UNION ALL SELECT date, patientId, 'FARRIER_VISIT' FROM FarrierVisit WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
          UNION ALL SELECT date, patientId, 'IMAGING' FROM Imaging WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
          UNION ALL SELECT date, patientId, 'LAB_RESULT' FROM LabResult WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
          UNION ALL SELECT date, patientId, 'LAMENESS' FROM Lameness WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
          UNION ALL SELECT startDate, patientId, 'MEDICATION' FROM Medication WHERE isActive = 1 AND startDate IS NOT NULL AND startDate BETWEEN '2025-01-10' AND '2025-01-20'
          UNION ALL SELECT date, patientId, 'REPRODUCTION_EVENT' FROM Reproduction WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
          UNION ALL SELECT dateAdministered, patientId, 'REPRO_MEDICATION' FROM ReproMedication WHERE isActive = 1 AND dateAdministered BETWEEN '2025-01-10' AND '2025-01-20'
          UNION ALL SELECT date, patientId, 'SURGERY' FROM Surgery WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
          UNION ALL SELECT date, patientId, 'CONTROLLED_SUBSTANCE' FROM Substance WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
          UNION ALL SELECT date, patientId, 'ULTRASOUND' FROM Ultrasound WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
          UNION ALL SELECT dateAdministered, patientId, 'VACCINATION' FROM Vaccination WHERE isActive = 1 AND dateAdministered BETWEEN '2025-01-10' AND '2025-01-20'
          UNION ALL SELECT date, patientId, 'WEIGHT' FROM Weight WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
          UNION ALL SELECT date, patientId, 'EMBRYO_TRANSFER' FROM EmbryoTransfer WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
          UNION ALL SELECT date, patientId, 'ICSI' FROM Icsi WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
        )
        SELECT activityDate AS date, activity_rows.patientId AS patientId, recordType AS recordTypeWireName, COUNT(*) AS cnt
        FROM activity_rows
        JOIN Patient ON Patient.id = activity_rows.patientId
        WHERE Patient.isActive = 1
        GROUP BY activityDate, activity_rows.patientId, recordType
        """.trimIndent()

    private fun earliestScopedSql(): String =
        """
        EXPLAIN QUERY PLAN WITH activity_rows AS (
          SELECT date AS activityDate, patientId FROM Consultation WHERE isActive = 1 AND patientId = 1
          UNION ALL SELECT date, patientId FROM Dentistry WHERE isActive = 1 AND patientId = 1
          UNION ALL SELECT dateAdministered, patientId FROM Deworming WHERE isActive = 1 AND patientId = 1
          UNION ALL SELECT date, patientId FROM FarrierVisit WHERE isActive = 1 AND patientId = 1
          UNION ALL SELECT date, patientId FROM Imaging WHERE isActive = 1 AND patientId = 1
          UNION ALL SELECT date, patientId FROM LabResult WHERE isActive = 1 AND patientId = 1
          UNION ALL SELECT date, patientId FROM Lameness WHERE isActive = 1 AND patientId = 1
          UNION ALL SELECT startDate, patientId FROM Medication WHERE isActive = 1 AND startDate IS NOT NULL AND patientId = 1
          UNION ALL SELECT date, patientId FROM Reproduction WHERE isActive = 1 AND patientId = 1
          UNION ALL SELECT dateAdministered, patientId FROM ReproMedication WHERE isActive = 1 AND patientId = 1
          UNION ALL SELECT date, patientId FROM Surgery WHERE isActive = 1 AND patientId = 1
          UNION ALL SELECT date, patientId FROM Substance WHERE isActive = 1 AND patientId = 1
          UNION ALL SELECT date, patientId FROM Ultrasound WHERE isActive = 1 AND patientId = 1
          UNION ALL SELECT dateAdministered, patientId FROM Vaccination WHERE isActive = 1 AND patientId = 1
          UNION ALL SELECT date, patientId FROM Weight WHERE isActive = 1 AND patientId = 1
          UNION ALL SELECT date, patientId FROM EmbryoTransfer WHERE isActive = 1 AND patientId = 1
          UNION ALL SELECT date, patientId FROM Icsi WHERE isActive = 1 AND patientId = 1
        )
        SELECT activityDate FROM activity_rows
        JOIN Patient ON Patient.id = activity_rows.patientId
        WHERE Patient.isActive = 1
        ORDER BY activityDate ASC LIMIT 1
        """.trimIndent()

    private fun earliestGlobalSql(): String =
        """
        EXPLAIN QUERY PLAN WITH activity_rows AS (
          SELECT date AS activityDate, patientId FROM Consultation WHERE isActive = 1
          UNION ALL SELECT date, patientId FROM Dentistry WHERE isActive = 1
          UNION ALL SELECT dateAdministered, patientId FROM Deworming WHERE isActive = 1
          UNION ALL SELECT date, patientId FROM FarrierVisit WHERE isActive = 1
          UNION ALL SELECT date, patientId FROM Imaging WHERE isActive = 1
          UNION ALL SELECT date, patientId FROM LabResult WHERE isActive = 1
          UNION ALL SELECT date, patientId FROM Lameness WHERE isActive = 1
          UNION ALL SELECT startDate, patientId FROM Medication WHERE isActive = 1 AND startDate IS NOT NULL
          UNION ALL SELECT date, patientId FROM Reproduction WHERE isActive = 1
          UNION ALL SELECT dateAdministered, patientId FROM ReproMedication WHERE isActive = 1
          UNION ALL SELECT date, patientId FROM Surgery WHERE isActive = 1
          UNION ALL SELECT date, patientId FROM Substance WHERE isActive = 1
          UNION ALL SELECT date, patientId FROM Ultrasound WHERE isActive = 1
          UNION ALL SELECT dateAdministered, patientId FROM Vaccination WHERE isActive = 1
          UNION ALL SELECT date, patientId FROM Weight WHERE isActive = 1
          UNION ALL SELECT date, patientId FROM EmbryoTransfer WHERE isActive = 1
          UNION ALL SELECT date, patientId FROM Icsi WHERE isActive = 1
        )
        SELECT activityDate FROM activity_rows
        JOIN Patient ON Patient.id = activity_rows.patientId
        WHERE Patient.isActive = 1
        ORDER BY activityDate ASC LIMIT 1
        """.trimIndent()

    @Test
    fun `buckets scoped uses SEARCH via patient_date indexes`() {
        val p = plan(bucketsScopedSql())
        assertSearchForScoped(p)
    }

    @Test
    fun `buckets global does not SCAN TABLE activity`() {
        val p = plan(bucketsGlobalSql())
        assertNoScanTable(p)
        // global may SCAN INDEX (covering) – ensure not SCAN TABLE; also ensure at least uses index for some table
        assertTrue(
            p.any { it.contains("INDEX") },
            "expected index usage in global plan:\n${p.joinToString("\n")}",
        )
    }

    @Test
    fun `earliest scoped uses SEARCH via patient_date indexes`() {
        val p = plan(earliestScopedSql())
        assertSearchForScoped(p)
    }

    @Test
    fun `earliest global does not SCAN TABLE activity`() {
        val p = plan(earliestGlobalSql())
        assertNoScanTable(p)
    }

    @Test
    fun `recordRefs scoped also SEARCH`() {
        val sql =
            """
            EXPLAIN QUERY PLAN WITH activity_rows AS (
              SELECT id AS recordId, patientId, date AS activityDate, 'CONSULTATION' AS recordType FROM Consultation WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
              UNION ALL SELECT id, patientId, date, 'DENTISTRY' FROM Dentistry WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
              UNION ALL SELECT id, patientId, dateAdministered, 'DEWORMING' FROM Deworming WHERE isActive = 1 AND dateAdministered BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
              UNION ALL SELECT id, patientId, date, 'FARRIER_VISIT' FROM FarrierVisit WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
              UNION ALL SELECT id, patientId, date, 'IMAGING' FROM Imaging WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
              UNION ALL SELECT id, patientId, date, 'LAB_RESULT' FROM LabResult WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
              UNION ALL SELECT id, patientId, date, 'LAMENESS' FROM Lameness WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
              UNION ALL SELECT id, patientId, startDate, 'MEDICATION' FROM Medication WHERE isActive = 1 AND startDate IS NOT NULL AND startDate BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
              UNION ALL SELECT id, patientId, date, 'REPRODUCTION_EVENT' FROM Reproduction WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
              UNION ALL SELECT id, patientId, dateAdministered, 'REPRO_MEDICATION' FROM ReproMedication WHERE isActive = 1 AND dateAdministered BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
              UNION ALL SELECT id, patientId, date, 'SURGERY' FROM Surgery WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
              UNION ALL SELECT id, patientId, date, 'CONTROLLED_SUBSTANCE' FROM Substance WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
              UNION ALL SELECT id, patientId, date, 'ULTRASOUND' FROM Ultrasound WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
              UNION ALL SELECT id, patientId, dateAdministered, 'VACCINATION' FROM Vaccination WHERE isActive = 1 AND dateAdministered BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
              UNION ALL SELECT id, patientId, date, 'WEIGHT' FROM Weight WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
              UNION ALL SELECT id, patientId, date, 'EMBRYO_TRANSFER' FROM EmbryoTransfer WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
              UNION ALL SELECT id, patientId, date, 'ICSI' FROM Icsi WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20' AND patientId = 1
            )
            SELECT recordId, activity_rows.patientId AS patientId, Patient.name AS patientName, activityDate AS date, recordType AS recordTypeWireName
            FROM activity_rows
            JOIN Patient ON Patient.id = activity_rows.patientId
            WHERE Patient.isActive = 1
            ORDER BY activityDate DESC, recordId DESC
            """.trimIndent()
        val p = plan(sql)
        assertNoScanTable(p)
        assertTrue(p.any { it.contains("SEARCH") }, "expected SEARCH in recordRefs scoped")
    }

    @Test
    fun `recordRefs global does not SCAN TABLE`() {
        val sql =
            """
            EXPLAIN QUERY PLAN WITH activity_rows AS (
              SELECT id AS recordId, patientId, date AS activityDate, 'CONSULTATION' AS recordType FROM Consultation WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
              UNION ALL SELECT id, patientId, date, 'DENTISTRY' FROM Dentistry WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
              UNION ALL SELECT id, patientId, dateAdministered, 'DEWORMING' FROM Deworming WHERE isActive = 1 AND dateAdministered BETWEEN '2025-01-10' AND '2025-01-20'
              UNION ALL SELECT id, patientId, date, 'FARRIER_VISIT' FROM FarrierVisit WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
              UNION ALL SELECT id, patientId, date, 'IMAGING' FROM Imaging WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
              UNION ALL SELECT id, patientId, date, 'LAB_RESULT' FROM LabResult WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
              UNION ALL SELECT id, patientId, date, 'LAMENESS' FROM Lameness WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
              UNION ALL SELECT id, patientId, startDate, 'MEDICATION' FROM Medication WHERE isActive = 1 AND startDate IS NOT NULL AND startDate BETWEEN '2025-01-10' AND '2025-01-20'
              UNION ALL SELECT id, patientId, date, 'REPRODUCTION_EVENT' FROM Reproduction WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
              UNION ALL SELECT id, patientId, dateAdministered, 'REPRO_MEDICATION' FROM ReproMedication WHERE isActive = 1 AND dateAdministered BETWEEN '2025-01-10' AND '2025-01-20'
              UNION ALL SELECT id, patientId, date, 'SURGERY' FROM Surgery WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
              UNION ALL SELECT id, patientId, date, 'CONTROLLED_SUBSTANCE' FROM Substance WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
              UNION ALL SELECT id, patientId, date, 'ULTRASOUND' FROM Ultrasound WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
              UNION ALL SELECT id, patientId, dateAdministered, 'VACCINATION' FROM Vaccination WHERE isActive = 1 AND dateAdministered BETWEEN '2025-01-10' AND '2025-01-20'
              UNION ALL SELECT id, patientId, date, 'WEIGHT' FROM Weight WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
              UNION ALL SELECT id, patientId, date, 'EMBRYO_TRANSFER' FROM EmbryoTransfer WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
              UNION ALL SELECT id, patientId, date, 'ICSI' FROM Icsi WHERE isActive = 1 AND date BETWEEN '2025-01-10' AND '2025-01-20'
            )
            SELECT recordId, activity_rows.patientId AS patientId, Patient.name AS patientName, activityDate AS date, recordType AS recordTypeWireName
            FROM activity_rows
            JOIN Patient ON Patient.id = activity_rows.patientId
            WHERE Patient.isActive = 1
            ORDER BY activityDate DESC, recordId DESC
            """.trimIndent()
        val p = plan(sql)
        assertNoScanTable(p)
    }

    @Test
    fun `repro event counts scoped uses SEARCH via patient_date`() {
        val sql =
            """
            EXPLAIN QUERY PLAN SELECT eventType, COUNT(*) AS cnt FROM Reproduction
            JOIN Patient ON Patient.id = Reproduction.patientId
            WHERE Reproduction.isActive = 1 AND Patient.isActive = 1
            AND Reproduction.date BETWEEN '2025-01-10' AND '2025-01-20' AND Reproduction.patientId = 1
            GROUP BY eventType
            """.trimIndent()
        val p = plan(sql)
        assertFalse(p.any { it.contains("SCAN TABLE Reproduction") }, "unexpected SCAN TABLE Reproduction in repro scoped:\n${p.joinToString("\n")}")
        assertTrue(
            p.any {
                it.contains("SEARCH") && it.contains("Reproduction")
            },
            "expected SEARCH Reproduction via idx_reproduction_patient_date in:\n${p.joinToString("\n")}",
        )
    }

    @Test
    fun `repro event counts global does not SCAN TABLE Reproduction`() {
        val sql =
            """
            EXPLAIN QUERY PLAN SELECT eventType, COUNT(*) AS cnt FROM Reproduction
            JOIN Patient ON Patient.id = Reproduction.patientId
            WHERE Reproduction.isActive = 1 AND Patient.isActive = 1
            AND Reproduction.date BETWEEN '2025-01-10' AND '2025-01-20'
            GROUP BY eventType
            """.trimIndent()
        val p = plan(sql)
        // Global may SCAN INDEX but not SCAN TABLE on active reproduction
        assertFalse(p.any { it.contains("SCAN TABLE Reproduction") }, "unexpected SCAN TABLE Reproduction in repro global:\n${p.joinToString("\n")}")
    }

    @Test
    fun `embryo and icsi scoped use SEARCH via patient_date`() {
        val embryoSql =
            """
            EXPLAIN QUERY PLAN SELECT COUNT(*) AS collections, COALESCE(SUM(embryoCount), 0) AS totalEmbryos FROM EmbryoTransfer
            JOIN Patient ON Patient.id = EmbryoTransfer.patientId
            WHERE EmbryoTransfer.isActive = 1 AND Patient.isActive = 1
            AND EmbryoTransfer.date BETWEEN '2025-01-10' AND '2025-01-20' AND EmbryoTransfer.patientId = 1
            """.trimIndent()
        val icsiSql =
            """
            EXPLAIN QUERY PLAN SELECT COUNT(*) AS sessions, COALESCE(SUM(folliclesRecovered), 0) AS totalFollicles FROM Icsi
            JOIN Patient ON Patient.id = Icsi.patientId
            WHERE Icsi.isActive = 1 AND Patient.isActive = 1
            AND Icsi.date BETWEEN '2025-01-10' AND '2025-01-20' AND Icsi.patientId = 1
            """.trimIndent()
        val pEmbryo = plan(embryoSql)
        val pIcsi = plan(icsiSql)
        assertTrue(pEmbryo.any { it.contains("SEARCH") && it.contains("EmbryoTransfer") }, "expected SEARCH EmbryoTransfer in:\n${pEmbryo.joinToString("\n")}")
        assertTrue(pIcsi.any { it.contains("SEARCH") && it.contains("Icsi") }, "expected SEARCH Icsi in:\n${pIcsi.joinToString("\n")}")
    }

    @Test
    fun `gestation active scoped uses SEARCH via patient_expected_due_date`() {
        val sql =
            """
            EXPLAIN QUERY PLAN SELECT Gestation.id AS gestationId, Gestation.patientId AS patientId, Gestation.breedingDate AS breedingDate, Gestation.expectedDueDate AS expectedDueDate, Gestation.gestationDays AS gestationDays, Gestation.status AS status, Patient.name AS patientName
            FROM Gestation
            JOIN Patient ON Patient.id = Gestation.patientId
            WHERE Gestation.patientId = 1
            AND Gestation.isActive = 1
            AND Patient.isActive = 1
            AND (Gestation.status IS NULL OR LOWER(Gestation.status) NOT IN ('completed', 'failed', 'foaled'))
            ORDER BY Gestation.expectedDueDate ASC, Gestation.id ASC
            """.trimIndent()
        val p = plan(sql)
        assertTrue(p.any { it.contains("SEARCH") && it.contains("Gestation") }, "expected SEARCH Gestation via patient index in:\n${p.joinToString("\n")}")
    }
}
