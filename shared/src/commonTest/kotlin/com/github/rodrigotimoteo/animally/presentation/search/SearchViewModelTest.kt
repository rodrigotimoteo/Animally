package com.github.rodrigotimoteo.animally.presentation.search

import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.domain.search.usecase.SearchUseCase
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.navigation.Route
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val searchRepositoryMock: ISearchRepository = mock()

    private val searchUseCase = SearchUseCase(searchRepositoryMock)

    private val navigator = AnimallyNavigator()

    private val result =
        SearchResult(
            patientId = 1L,
            patientName = "Charlie",
            breed = null,
            microchipId = null,
            recordType = ISearchRepository.TYPE_PATIENT,
            recordId = 1L,
            date = null,
            snippet = "charlie",
        )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        SearchViewModel(
            searchUseCase = searchUseCase,
            animallyNavigator = navigator,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `query shorter than two chars skips search and clears results`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onQueryChange("fl")
            advanceUntilIdle()
            assertEquals(listOf(result), vm.uiState.value.results)

            vm.onQueryChange("f")
            advanceUntilIdle()

            assertTrue(
                vm.uiState.value.results
                    .isEmpty(),
            )
            assertFalse(vm.uiState.value.isLoading)
            verify(VerifyMode.exactly(1)) {
                searchRepositoryMock.search(any(), any(), any(), any())
            }
        }

    @Test
    fun `blank query trims and skips search`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onQueryChange("   ")
            advanceUntilIdle()

            assertTrue(
                vm.uiState.value.results
                    .isEmpty(),
            )
            assertFalse(vm.uiState.value.isLoading)
            verify(VerifyMode.exactly(0)) {
                searchRepositoryMock.search(any(), any(), any(), any())
            }
        }

    @Test
    fun `query change runs search and stores results`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onQueryChange("flu")
            advanceUntilIdle()

            assertEquals("flu", vm.uiState.value.query)
            assertEquals(listOf(result), vm.uiState.value.results)
            assertFalse(vm.uiState.value.isLoading)
            assertNull(vm.uiState.value.errorMessage)
            verify(VerifyMode.exactly(1)) { searchRepositoryMock.search("flu*", null, null, null) }
        }

    @Test
    fun `toggle record type adds then removes filter chip`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            vm.onQueryChange("flu")
            advanceUntilIdle()

            vm.toggleRecordType(ISearchRepository.TYPE_PATIENT)
            advanceUntilIdle()
            assertTrue(
                vm.uiState.value.recordTypes
                    .contains(ISearchRepository.TYPE_PATIENT),
            )
            verify(VerifyMode.exactly(1)) {
                searchRepositoryMock.search(
                    "flu*",
                    null,
                    null,
                    listOf(ISearchRepository.TYPE_PATIENT),
                )
            }

            vm.toggleRecordType(ISearchRepository.TYPE_PATIENT)
            advanceUntilIdle()
            assertTrue(
                vm.uiState.value.recordTypes
                    .isEmpty(),
            )
            verify(VerifyMode.exactly(2)) { searchRepositoryMock.search("flu*", null, null, null) }
        }

    @Test
    fun `set from and to dates narrows search bounds`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            vm.onQueryChange("flu")
            advanceUntilIdle()

            vm.setFromDate(LocalDate(2025, 1, 1))
            vm.setToDate(LocalDate(2025, 12, 31))
            advanceUntilIdle()

            assertEquals(LocalDate(2025, 1, 1), vm.uiState.value.fromDate)
            assertEquals(LocalDate(2025, 12, 31), vm.uiState.value.toDate)
            verify(VerifyMode.exactly(1)) {
                searchRepositoryMock.search(
                    "flu*",
                    LocalDate(2025, 1, 1),
                    LocalDate(2025, 12, 31),
                    null,
                )
            }
        }

    @Test
    fun `clearing date filters resets search bounds to null`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            vm.onQueryChange("flu")
            advanceUntilIdle()
            vm.setFromDate(LocalDate(2025, 1, 1))
            advanceUntilIdle()

            vm.setFromDate(null)
            advanceUntilIdle()

            assertNull(vm.uiState.value.fromDate)
            verify(VerifyMode.exactly(2)) { searchRepositoryMock.search("flu*", null, null, null) }
        }

    @Test
    fun `rapid query changes collapse into a single debounced search`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onQueryChange("f")
            vm.onQueryChange("fl")
            vm.onQueryChange("flu")
            vm.onQueryChange("flut")
            advanceUntilIdle()

            assertEquals("flut", vm.uiState.value.query)
            assertEquals(listOf(result), vm.uiState.value.results)
            verify(VerifyMode.exactly(1)) { searchRepositoryMock.search("flut*", null, null, null) }
        }

    @Test
    fun `rapid successive queries only latest result lands`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val slowResult = result.copy(patientName = "Slow horse")
            val fastResult = result.copy(patientName = "Fast horse")
            val slowRepo =
                object : ISearchRepository {
                    override fun search(
                        query: String,
                        from: LocalDate?,
                        to: LocalDate?,
                        recordTypes: List<String>?,
                    ): List<SearchResult> = if (query == "flu*") listOf(slowResult) else listOf(fastResult)

                    override fun indexRecord(
                        recordType: String,
                        patientId: Long,
                        recordId: Long,
                        date: LocalDate?,
                        searchableText: String,
                    ) = Unit

                    override fun deleteRecord(
                        recordType: String,
                        recordId: Long,
                    ) = Unit

                    override fun rebuild() = Unit
                }
            val vm =
                SearchViewModel(
                    searchUseCase = SearchUseCase(slowRepo),
                    animallyNavigator = navigator,
                    ioDispatcher = StandardTestDispatcher(testScheduler),
                )

            vm.onQueryChange("flu")
            testScheduler.advanceTimeBy(301)
            vm.onQueryChange("fluu")
            testScheduler.advanceUntilIdle()

            assertEquals("fluu", vm.uiState.value.query)
            assertEquals(listOf(fastResult), vm.uiState.value.results)
        }

    @Test
    fun `result click navigates to patient detail route`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onResultClick(42L)

            assertEquals(Route.PatientDetail(42L), navigator.backStack.last())
        }

    @Test
    fun `search failure surfaces error and stops loading`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { searchRepositoryMock.search(any(), any(), any(), any()) } throws
                RuntimeException("fts broken")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            vm.onQueryChange("flu")
            advanceUntilIdle()

            assertEquals("fts broken", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
            assertTrue(
                vm.uiState.value.results
                    .isEmpty(),
            )
        }

    @Test
    fun `onDismissError clears error message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            every { searchRepositoryMock.search(any(), any(), any(), any()) } throws
                RuntimeException("fts broken")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            vm.onQueryChange("flu")
            advanceUntilIdle()
            assertEquals("fts broken", vm.uiState.value.errorMessage)

            vm.onDismissError()

            assertNull(vm.uiState.value.errorMessage)
        }
}
