package com.github.rodrigotimoteo.animally.presentation.ui.search

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.domain.search.usecase.SearchUseCase
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.search.SearchViewModel
import com.github.rodrigotimoteo.animally.presentation.search.view.SearchScreen
import com.github.rodrigotimoteo.animally.presentation.ui.ProvideTestLifecycle
import com.github.rodrigotimoteo.animally.presentation.ui.installMainDispatcher
import com.github.rodrigotimoteo.animally.presentation.ui.restoreMainDispatcher
import com.github.rodrigotimoteo.animally.presentation.ui.uiTestIoDispatcher
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SearchScreenTest {
    @AfterTest
    fun tearDownMainDispatcher() {
        restoreMainDispatcher()
    }

    private val result =
        SearchResult(
            patientId = 1L,
            patientName = "Charlie",
            breed = null,
            microchipId = null,
            recordType = ISearchRepository.TYPE_PATIENT,
            recordId = 1L,
            date = LocalDate(2024, 1, 1),
            snippet = "charlie the horse",
        )

    private fun createViewModel(searchRepository: ISearchRepository): SearchViewModel =
        SearchViewModel(
            searchUseCase = SearchUseCase(searchRepository),
            animallyNavigator = AnimallyNavigator(),
            ioDispatcher = uiTestIoDispatcher(),
            debounceMillis = 0,
        )

    private fun ComposeUiTest.setSearchScreen(
        viewModel: SearchViewModel,
        block: ComposeUiTest.() -> Unit,
    ) {
        setContent {
            ProvideTestLifecycle {
                SearchScreen(viewModel = viewModel)
            }
        }
        block()
    }

    @Test
    fun `when query is empty then hint is shown and no results`() =
        runComposeUiTest {
            installMainDispatcher()
            val searchRepository: ISearchRepository = mock()
            every { searchRepository.search(any(), any(), any(), any()) } returns listOf(result)
            val viewModel = createViewModel(searchRepository)

            setSearchScreen(viewModel) {
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithText("Type at least 2 characters to search").fetchSemanticsNodes().isNotEmpty()
                }
                onNodeWithText("Type at least 2 characters to search").assertIsDisplayed()
                onAllNodesWithContentDescription("Patient result").assertCountEquals(0)
            }
        }

    @Test
    fun `when typing at least two characters then results are shown`() =
        runComposeUiTest {
            installMainDispatcher()
            val searchRepository: ISearchRepository = mock()
            every { searchRepository.search(any(), any(), any(), any()) } returns listOf(result)
            val viewModel = createViewModel(searchRepository)

            setSearchScreen(viewModel) {
                viewModel.onQueryChange("flu")
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithContentDescription("Patient result").fetchSemanticsNodes().isNotEmpty()
                }
                onNodeWithContentDescription("Patient result").assertIsDisplayed()
                onNodeWithText("Charlie").assertIsDisplayed()
            }
        }

    @Test
    fun `when filter chip toggled then selected state changes`() =
        runComposeUiTest {
            installMainDispatcher()
            val searchRepository: ISearchRepository = mock()
            every { searchRepository.search(any(), any(), any(), any()) } returns emptyList()
            val viewModel = createViewModel(searchRepository)

            setSearchScreen(viewModel) {
                viewModel.onQueryChange("flu")
                val chip = onNodeWithContentDescription("Filter by Patient")
                chip.assertIsNotSelected()

                chip.performClick()
                waitForIdle()
                chip.assertIsSelected()

                chip.performClick()
                waitForIdle()
                chip.assertIsNotSelected()
            }
        }

    @Test
    fun `when query has no matches then empty results state is shown`() =
        runComposeUiTest {
            installMainDispatcher()
            val searchRepository: ISearchRepository = mock()
            every { searchRepository.search(any(), any(), any(), any()) } returns emptyList()
            val viewModel = createViewModel(searchRepository)

            setSearchScreen(viewModel) {
                viewModel.onQueryChange("zzz")
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithText("No results found").fetchSemanticsNodes().isNotEmpty()
                }
                onNodeWithText("No results found").assertIsDisplayed()
            }
        }
}
