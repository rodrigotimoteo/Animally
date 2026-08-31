package com.github.rodrigotimoteo.animally.domain.insights.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.insights.model.ActivityPoint
import com.github.rodrigotimoteo.animally.domain.insights.model.CurrentCareSnapshot
import com.github.rodrigotimoteo.animally.domain.insights.model.CurrentGestationItem
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsExportFileNames
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsFilter
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsRecordRef
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsSnapshot
import com.github.rodrigotimoteo.animally.domain.insights.model.OverviewMetrics
import com.github.rodrigotimoteo.animally.domain.insights.model.RecordTypeCount
import com.github.rodrigotimoteo.animally.domain.insights.model.ReproductionEventCount
import com.github.rodrigotimoteo.animally.domain.insights.model.ReproductionMetrics
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEventType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class ExportInsightsBundleUseCaseTest {
    private val today = LocalDate(2025, 2, 15)
    private val generatedAt = Instant.parse("2025-02-15T10:00:00Z")
    private val sut = ExportInsightsBundleUseCase(clock = { generatedAt })

    private fun snapshotFixture(): InsightsSnapshot {
        val overview =
            OverviewMetrics(
                patientCount = 3,
                activityCount = 10,
                caseDayCount = 5,
                activeDayCount = 4,
                averagePerActiveDay = 2.5,
                averagePerCaseDay = 2.0,
                comparison = null,
            )
        val activitySeries =
            listOf(
                ActivityPoint(LocalDate(2025, 1, 10), 3),
                ActivityPoint(LocalDate(2025, 1, 11), 7),
            )
        val recordMix =
            listOf(
                RecordTypeCount(RecordType.Consultation, 4, 0.4),
                RecordTypeCount(RecordType.Vaccination, 3, 0.3),
                RecordTypeCount(RecordType.Dentistry, 3, 0.3),
            )
        val reproduction =
            ReproductionMetrics(
                eventCounts =
                    listOf(
                        ReproductionEventCount(ReproductionEventType.Breeding, 5),
                        ReproductionEventCount(ReproductionEventType.Heat, 3),
                        ReproductionEventCount(ReproductionEventType.Other, 1),
                    ),
                embryoCollections = 2,
                embryosCollected = 5,
                averageEmbryosPerCollection = 2.5,
                icsiSessions = 1,
                folliclesRecovered = 10,
                averageFolliclesPerIcsi = 10.0,
                ultrasoundCount = 4,
            )
        val currentCare =
            CurrentCareSnapshot(
                activeGestations =
                    listOf(
                        CurrentGestationItem(10L, "Thunder", 100L, 120, LocalDate(2025, 3, 1), 14, "Active"),
                        CurrentGestationItem(5L, "Star", 101L, 200, LocalDate(2025, 4, 10), 54, "Active"),
                        CurrentGestationItem(42L, "Nova", 102L, 350, LocalDate(2025, 2, 10), -5, "Active"),
                    ),
            )
        return InsightsSnapshot(
            overview = overview,
            activitySeries = activitySeries,
            recordMix = recordMix,
            reproduction = reproduction,
            currentCare = currentCare,
            dataIssues = emptyList(),
            appliedFilter = InsightsFilter(LocalDate(2025, 1, 10), LocalDate(2025, 1, 20), patientId = null),
        )
    }

    @Test
    fun `overview csv totals match snapshot golden`() {
        val snap = snapshotFixture()
        val bundle = sut(snap, today)

        val overviewMap = parseSingleRow(bundle.overviewCsv)
        assertEquals(snap.overview.patientCount.toString(), overviewMap["patient_count"])
        assertEquals(snap.overview.activityCount.toString(), overviewMap["activity_count"])
        assertEquals(snap.overview.caseDayCount.toString(), overviewMap["case_day_count"])
        assertEquals(snap.overview.activeDayCount.toString(), overviewMap["active_day_count"])
        assertEquals(snap.overview.averagePerActiveDay.toString(), overviewMap["average_per_active_day"])
        assertEquals(snap.overview.averagePerCaseDay.toString(), overviewMap["average_per_case_day"])
    }

    @Test
    fun `activity series csv totals and sums match snapshot`() {
        val snap = snapshotFixture()
        val bundle = sut(snap, today)

        val rows = parseCsvRows(bundle.activitySeriesCsv)
        assertEquals(snap.activitySeries.size, rows.size)
        val sum = rows.sumOf { it["count"]!!.toInt() }
        assertEquals(snap.overview.activityCount, sum)
        // also each point matches
        snap.activitySeries.forEachIndexed { idx, point ->
            assertEquals(point.periodStart.toString(), rows[idx]["period_start"])
            assertEquals(point.count.toString(), rows[idx]["count"])
        }
    }

    @Test
    fun `record mix csv counts sum to activity count and shares valid`() {
        val snap = snapshotFixture()
        val bundle = sut(snap, today)

        val rows = parseCsvRows(bundle.recordMixCsv)
        assertEquals(snap.recordMix.size, rows.size)
        val sum = rows.sumOf { it["count"]!!.toInt() }
        assertEquals(snap.overview.activityCount, sum)
        val shareSum = rows.mapNotNull { it["share"]?.takeIf { v -> v.isNotEmpty() }?.toDouble() }.sum()
        assertEquals(1.0, shareSum, 0.001)
    }

    @Test
    fun `reproduction csvs match snapshot golden`() {
        val snap = snapshotFixture()
        val bundle = sut(snap, today)

        val eventRows = parseCsvRows(bundle.reproductionEventsCsv)
        assertEquals(snap.reproduction.eventCounts.size, eventRows.size)
        snap.reproduction.eventCounts.forEach { expected ->
            val found = eventRows.first { it["event_type_storage"] == expected.type.storageLabel }
            assertEquals(expected.count.toString(), found["count"])
            assertEquals(expected.type.displayLabel, found["event_type_display"])
        }

        val summary = parseSingleRow(bundle.reproductionSummaryCsv)
        assertEquals(snap.reproduction.embryoCollections.toString(), summary["embryo_collections"])
        assertEquals(snap.reproduction.embryosCollected.toString(), summary["embryos_collected"])
        assertEquals(snap.reproduction.averageEmbryosPerCollection.toString(), summary["average_embryos_per_collection"])
        assertEquals(snap.reproduction.icsiSessions.toString(), summary["icsi_sessions"])
        assertEquals(snap.reproduction.folliclesRecovered.toString(), summary["follicles_recovered"])
        assertEquals(snap.reproduction.averageFolliclesPerIcsi.toString(), summary["average_follicles_per_icsi"])
        assertEquals(snap.reproduction.ultrasoundCount.toString(), summary["ultrasound_count"])
    }

    @Test
    fun `current care csv uses pseudonym and no raw names or ids leak`() {
        val snap = snapshotFixture()
        val bundle = sut(snap, today, pseudonymize = true)

        // pseudonym map assigned ascending: ids 5 -> P001, 10 -> P002, 42 -> P003
        assertEquals("P001", bundle.pseudonymMap[5L])
        assertEquals("P002", bundle.pseudonymMap[10L])
        assertEquals("P003", bundle.pseudonymMap[42L])

        val csv = bundle.currentCareCsv
        // raw names must not appear anywhere in any exported file when pseudonymized
        val allContent = bundle.files.values.joinToString("\n")
        assertFalse(allContent.contains("Thunder"))
        assertFalse(allContent.contains("Star"))
        assertFalse(allContent.contains("Nova"))
        // raw patient ids as whole CSV cells should not appear as patient identifiers; gestation ids are allowed
        // Check pseudonym column exists
        assertTrue(csv.contains("patient_pseudonym"))
        assertFalse(csv.contains("patient_name"))
        assertFalse(csv.contains("patient_id"))

        val rows = parseCsvRows(csv)
        assertEquals(3, rows.size)
        val pseudos = rows.map { it["patient_pseudonym"] }.toSet()
        assertEquals(setOf("P001", "P002", "P003"), pseudos)

        // due soon flags correct: Thunder 14 days -> 30/60/90 = 1/1/1
        val thunderRow = rows.first { it["patient_pseudonym"] == "P002" }
        assertEquals("1", thunderRow["is_due_soon_30"])
        assertEquals("1", thunderRow["is_due_soon_60"])
        assertEquals("1", thunderRow["is_due_soon_90"])
        // Star 54 days -> 0/1/1
        val starRow = rows.first { it["patient_pseudonym"] == "P001" }
        assertEquals("0", starRow["is_due_soon_30"])
        assertEquals("1", starRow["is_due_soon_60"])
        assertEquals("1", starRow["is_due_soon_90"])
        // Nova -5 overdue -> 0/0/0
        val novaRow = rows.first { it["patient_pseudonym"] == "P003" }
        assertEquals("0", novaRow["is_due_soon_30"])
        assertEquals("0", novaRow["is_due_soon_60"])
        assertEquals("0", novaRow["is_due_soon_90"])
    }

    @Test
    fun `metadata csv contains range scope version and generation time`() {
        val snap = snapshotFixture()
        val bundle = sut(snap, today, generatedAt = generatedAt)
        val rows = parseCsvRows(bundle.metadataCsv)
        val map = rows.associate { it["key"]!! to it["value"]!! }
        assertEquals(generatedAt.toString(), map["generated_at"])
        assertEquals(today.toString(), map["today"])
        assertEquals("2025-01-10", map["range_from"])
        assertEquals("2025-01-20", map["range_to"])
        assertEquals("all", map["patient_scope"])
        assertTrue(map.containsKey("app_version"))
        assertTrue(map.containsKey("schema_version"))
        assertEquals("1", map["schema_version"])
    }

    @Test
    fun `metadata scope pseudonymized when filtered`() {
        val snap = snapshotFixture().copy(appliedFilter = InsightsFilter(LocalDate(2025, 1, 10), LocalDate(2025, 1, 20), patientId = 10L))
        // also add that patient to care so map contains it even if not in gestations
        val bundle = sut(snap, today, pseudonymize = true)
        val rows = parseCsvRows(bundle.metadataCsv)
        val map = rows.associate { it["key"]!! to it["value"]!! }
        // when pseudonymized, patient_scope should be pseudonym for 10L
        // 10L is P002 in this fixture (ids 5,10,42 sorted)
        assertEquals("P002", map["patient_scope"])
        // pseudonymMap should contain scope id
        assertEquals("P002", bundle.pseudonymMap[10L])
    }

    @Test
    fun `data dictionary contains all files and definitions`() {
        val snap = snapshotFixture()
        val bundle = sut(snap, today)

        val json = bundle.dataDictionaryJson
        assertTrue(json.contains(InsightsExportFileNames.OVERVIEW))
        assertTrue(json.contains(InsightsExportFileNames.ACTIVITY_SERIES))
        assertTrue(json.contains(InsightsExportFileNames.RECORD_MIX))
        assertTrue(json.contains(InsightsExportFileNames.REPRODUCTION_EVENTS))
        assertTrue(json.contains(InsightsExportFileNames.REPRODUCTION_SUMMARY))
        assertTrue(json.contains(InsightsExportFileNames.CURRENT_CARE))
        assertTrue(json.contains(InsightsExportFileNames.METADATA))
        assertTrue(json.contains("\"type\""))
        assertTrue(json.contains("\"definition\""))

        val md = bundle.dataDictionaryMarkdown
        assertTrue(md.contains("# Insights Export Data Dictionary"))
        assertTrue(md.contains(InsightsExportFileNames.OVERVIEW))
        assertTrue(md.contains("patient_count"))
        assertTrue(md.contains("average_per_active_day"))
        assertTrue(md.contains("period_start"))
        assertTrue(md.contains("record_type_wire"))
        assertTrue(md.contains("patient_pseudonym"))
    }

    @Test
    fun `pseudonym stable across bundles for same ids`() {
        val snap = snapshotFixture()
        val b1 = sut(snap, today)
        val b2 = sut(snap, today)
        assertEquals(b1.pseudonymMap, b2.pseudonymMap)
    }

    @Test
    fun `empty snapshot produces valid headers and empty rows`() {
        val emptySnap =
            InsightsSnapshot(
                overview =
                    OverviewMetrics(
                        patientCount = 0,
                        activityCount = 0,
                        caseDayCount = 0,
                        activeDayCount = 0,
                        averagePerActiveDay = null,
                        averagePerCaseDay = null,
                        comparison = null,
                    ),
                activitySeries = emptyList(),
                recordMix = emptyList(),
                reproduction =
                    ReproductionMetrics(
                        eventCounts = emptyList(),
                        embryoCollections = 0,
                        embryosCollected = 0,
                        averageEmbryosPerCollection = null,
                        icsiSessions = 0,
                        folliclesRecovered = 0,
                        averageFolliclesPerIcsi = null,
                        ultrasoundCount = 0,
                    ),
                currentCare = CurrentCareSnapshot(activeGestations = emptyList()),
                dataIssues = emptyList(),
                appliedFilter = null,
            )
        val bundle = sut(emptySnap, today)
        // overview still one row with zeros and empties
        val overview = parseSingleRow(bundle.overviewCsv)
        assertEquals("0", overview["activity_count"])
        assertEquals("", overview["average_per_active_day"])
        assertEquals("", overview["comparison_percentage_delta"])
        // activity series empty -> header only
        val actRows = parseCsvRows(bundle.activitySeriesCsv)
        assertTrue(actRows.isEmpty())
        // record mix empty
        assertTrue(parseCsvRows(bundle.recordMixCsv).isEmpty())
        // reproduction empty
        assertTrue(parseCsvRows(bundle.reproductionEventsCsv).isEmpty())
        // current care empty -> header only
        assertTrue(parseCsvRows(bundle.currentCareCsv).isEmpty())
        // metadata range empty strings
        val meta = parseCsvRows(bundle.metadataCsv).associate { it["key"]!! to it["value"]!! }
        assertEquals("", meta["range_from"])
        assertEquals("", meta["range_to"])
    }

    @Test
    fun `quoted newline round trip via CsvFormatter retained`() {
        val trickyName = "Thunder, \"Storm\"\nNova"
        val trickyStatus = "Active, \"needs\nreview\""
        val snap =
            snapshotFixture().copy(
                currentCare =
                    CurrentCareSnapshot(
                        activeGestations =
                            listOf(
                                CurrentGestationItem(1L, trickyName, 99L, 100, LocalDate(2025, 3, 15), 28, trickyStatus),
                            ),
                    ),
                appliedFilter = InsightsFilter(LocalDate(2025, 1, 10), LocalDate(2025, 1, 20)),
            )
        // pseudonymized: status still contains tricky chars and must be quoted correctly
        val bundlePseudo = sut(snap, today, pseudonymize = true)
        val csvPseudo = bundlePseudo.currentCareCsv
        // CsvFormatter should quote fields with comma/quote/newline; check raw CSV contains doubled quotes
        assertTrue(csvPseudo.contains("\""))
        // Ensure pseudonym mode hides raw tricky name
        assertFalse(csvPseudo.contains(trickyName))
        // Verify we can parse back status correctly despite quoting/newline
        val rowsPseudo = parseCsvRows(csvPseudo)
        assertEquals(1, rowsPseudo.size)
        // The status round-trips through our parser (which handles RFC4180) should equal original
        assertEquals(trickyStatus, rowsPseudo[0]["status"])
        assertEquals("P001", rowsPseudo[0]["patient_pseudonym"])

        // non-pseudonymized: name appears quoted and round-trips
        val bundleRaw = sut(snap, today, pseudonymize = false)
        val csvRaw = bundleRaw.currentCareCsv
        assertTrue(csvRaw.contains("patient_name"))
        val rowsRaw = parseCsvRows(csvRaw)
        assertEquals(trickyName, rowsRaw[0]["patient_name"])
        assertEquals(trickyStatus, rowsRaw[0]["status"])
    }

    @Test
    fun `record refs pseudonymized and sums not exposing raw ids`() {
        val refs =
            listOf(
                InsightsRecordRef(RecordType.Consultation, 5L, 1L, "Star", LocalDate(2025, 1, 15)),
                InsightsRecordRef(RecordType.Vaccination, 10L, 2L, "Thunder", LocalDate(2025, 1, 16)),
            )
        val snap = snapshotFixture()
        val bundle = sut(snap, today, pseudonymize = true, recordRefs = refs)
        val csv = bundle.recordRefsCsv
        assertTrue(csv.contains("patient_pseudonym"))
        assertFalse(csv.contains("Star"))
        assertFalse(csv.contains("Thunder"))
        val rows = parseCsvRows(csv)
        assertEquals(2, rows.size)
        assertEquals(setOf("P001", "P002"), rows.map { it["patient_pseudonym"] }.toSet())
    }

    @Test
    fun `bundle files map contains all expected filenames`() {
        val snap = snapshotFixture()
        val bundle = sut(snap, today)
        val expected =
            setOf(
                InsightsExportFileNames.OVERVIEW,
                InsightsExportFileNames.ACTIVITY_SERIES,
                InsightsExportFileNames.RECORD_MIX,
                InsightsExportFileNames.REPRODUCTION_EVENTS,
                InsightsExportFileNames.REPRODUCTION_SUMMARY,
                InsightsExportFileNames.CURRENT_CARE,
                InsightsExportFileNames.RECORD_REFS,
                InsightsExportFileNames.METADATA,
                InsightsExportFileNames.DATA_DICTIONARY_JSON,
                InsightsExportFileNames.DATA_DICTIONARY_MD,
            )
        assertEquals(expected, bundle.files.keys)
        expected.forEach { file ->
            assertTrue(bundle.files[file]!!.isNotEmpty() || file == InsightsExportFileNames.RECORD_REFS || file == InsightsExportFileNames.ACTIVITY_SERIES)
        }
    }

    @Test
    fun `export metadata pseudonymize false contains raw scope id`() {
        val snap = snapshotFixture().copy(appliedFilter = InsightsFilter(LocalDate(2025, 1, 10), LocalDate(2025, 1, 20), patientId = 42L))
        val bundle = sut(snap, today, pseudonymize = false)
        val meta = parseCsvRows(bundle.metadataCsv).associate { it["key"]!! to it["value"]!! }
        assertEquals("42", meta["patient_scope"])
    }

    // Simple RFC4180 parser for test verification (handles quoted fields, doubled quotes, embedded newlines already split by CsvFormatter line ending \r\n).
    // Since CsvFormatter uses \r\n, we split by \r\n then parse each line.
    private fun parseCsvRows(csv: String): List<Map<String, String>> {
        val lines = csv.split("\r\n").filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()
        val headers = parseLine(lines[0])
        if (lines.size == 1) return emptyList()
        return lines.drop(1).map { line ->
            val fields = parseLine(line)
            headers.zip(fields).toMap()
        }
    }

    private fun parseSingleRow(csv: String): Map<String, String> = parseCsvRows(csv).single()

    private fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"')
                    i += 2
                    continue
                }
                c == '"' -> {
                    inQuotes = !inQuotes
                }
                c == ',' && !inQuotes -> {
                    result.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}
