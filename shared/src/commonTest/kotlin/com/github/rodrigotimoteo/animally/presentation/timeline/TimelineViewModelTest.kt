package com.github.rodrigotimoteo.animally.presentation.timeline

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.timeline.usecase.GetTimelineUseCase
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.navigation.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModelTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var useCase: GetTimelineUseCase
    private val navigator = AnimallyNavigator()

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        useCase = GetTimelineUseCase(database)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        patientId: Long?,
        dispatcher: TestDispatcher,
    ) = TimelineViewModel(
        patientId = patientId,
        getTimelineUseCase = useCase,
        animallyNavigator = navigator,
        ioDispatcher = dispatcher,
    )

    private fun seedPatient(
        id: Long = 1L,
        name: String = "Charlie",
    ) {
        database.patientQueries.insert(
            name = name,
            species = "Equine",
            breed = null,
            dateOfBirth = null,
            gender = null,
            microchipId = null,
            ueln = null,
            registrationNumber = null,
            stableLocation = null,
            photoUri = null,
            notes = null,
            ownerId = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
            cogginsTestDate = null,
            cogginsResult = null,
            cogginsExpiryDate = null,
        )
    }

    private fun seedVaccination(
        patientId: Long,
        vaccineName: String,
        date: LocalDate,
    ) {
        database.vaccinationQueries.insert(
            patientId = patientId,
            vaccineName = vaccineName,
            dateAdministered = date,
            nextDueDate = null,
            vetName = null,
            batchNumber = null,
            site = null,
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )
    }

    private fun seedDeworming(
        patientId: Long,
        product: String,
        date: LocalDate,
    ) {
        database.dewormingQueries.insert(
            patientId = patientId,
            product = product,
            dateAdministered = date,
            nextDueDate = null,
            dose = null,
            vetName = null,
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )
    }

    @Test
    fun `patient timeline loads groups sorted by date descending`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            seedPatient()
            seedVaccination(1L, "Flu", LocalDate(2024, 1, 15))
            seedDeworming(1L, "Ivermectin", LocalDate(2024, 3, 20))
            seedVaccination(1L, "Tetanus", LocalDate(2024, 2, 10))
            val vm = createViewModel(1L, StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
            assertEquals(3, state.groups.size)
            assertEquals(LocalDate(2024, 3, 20), state.groups[0].date)
            assertEquals(LocalDate(2024, 2, 10), state.groups[1].date)
            assertEquals(LocalDate(2024, 1, 15), state.groups[2].date)
            assertEquals("Charlie", state.patientName)
        }

    @Test
    fun `global timeline aggregates entries from all patients`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            database.patientQueries.insert(
                name = "Charlie",
                species = "Equine",
                breed = null,
                dateOfBirth = null,
                gender = null,
                microchipId = null,
                ueln = null,
                registrationNumber = null,
                stableLocation = null,
                photoUri = null,
                notes = null,
                ownerId = null,
                isActive = true,
                createdAt = Instant.fromEpochMilliseconds(0L),
                updatedAt = Instant.fromEpochMilliseconds(0L),
                cogginsTestDate = null,
                cogginsResult = null,
                cogginsExpiryDate = null,
            )
            database.patientQueries.insert(
                name = "Ghost",
                species = "Equine",
                breed = null,
                dateOfBirth = null,
                gender = null,
                microchipId = null,
                ueln = null,
                registrationNumber = null,
                stableLocation = null,
                photoUri = null,
                notes = null,
                ownerId = null,
                isActive = true,
                createdAt = Instant.fromEpochMilliseconds(0L),
                updatedAt = Instant.fromEpochMilliseconds(0L),
                cogginsTestDate = null,
                cogginsResult = null,
                cogginsExpiryDate = null,
            )
            seedVaccination(1L, "Flu", LocalDate(2024, 5, 1))
            seedDeworming(2L, "Ivermectin", LocalDate(2024, 5, 1))
            val vm = createViewModel(null, StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.patientName)
            assertEquals(1, state.groups.size)
            assertEquals(2, state.groups[0].entries.size)
            assertTrue(state.groups[0].entries.any { it.patientName == "Charlie" })
            assertTrue(state.groups[0].entries.any { it.patientName == "Ghost" })
        }

    @Test
    fun `empty timeline shows no groups`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            seedPatient()
            val vm = createViewModel(1L, StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
            assertTrue(state.groups.isEmpty())
        }

    @Test
    fun `entry click navigates to vaccination edit`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            seedPatient()
            seedVaccination(1L, "Flu", LocalDate(2024, 1, 15))
            val vm = createViewModel(1L, StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            val entry =
                vm.uiState.value.groups
                    .first()
                    .entries
                    .first()
            vm.onEntryClick(entry.recordType, entry.patientId, entry.recordId)

            assertEquals(Route.AddEditVaccination(1L, entry.recordId), navigator.backStack.last())
        }

    @Test
    fun `unknown record type navigates to patient detail`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            seedPatient()
            val vm = createViewModel(1L, StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.onEntryClick("UnknownType", 1L, 999L)

            assertEquals(Route.PatientDetail(1L), navigator.backStack.last())
        }
}
