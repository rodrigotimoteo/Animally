package com.github.rodrigotimoteo.animally.presentation.ui.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.github.rodrigotimoteo.animally.presentation.common.list.CollapsibleListContent
import com.github.rodrigotimoteo.animally.presentation.common.list.CollapsibleListState
import com.github.rodrigotimoteo.animally.presentation.common.list.ListDisplayActions
import com.github.rodrigotimoteo.animally.presentation.common.list.ListDisplayState
import com.github.rodrigotimoteo.animally.presentation.common.list.SearchableListHeader
import com.github.rodrigotimoteo.animally.presentation.common.list.filterBySearch
import com.github.rodrigotimoteo.animally.presentation.common.list.visibleForListDisplay
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SearchableCollapsibleListSectionTest {
    @Test
    fun `long list exposes search and expands on demand`() =
        runComposeUiTest {
            val records = (1..6).map { "Record $it" }

            setContent {
                var displayState by remember { mutableStateOf(ListDisplayState()) }
                val filteredRecords = records.filterBySearch(displayState.searchQuery) { it }
                val displayActions =
                    ListDisplayActions(
                        onSearchClick = {
                            displayState = displayState.copy(searchQuery = "")
                        },
                        onSearchQueryChange = { query ->
                            displayState = displayState.copy(searchQuery = query)
                        },
                        onCloseSearch = {
                            displayState = displayState.copy(searchQuery = null)
                        },
                        onToggleExpanded = {
                            displayState = displayState.copy(isExpanded = !displayState.isExpanded)
                        },
                    )

                MaterialTheme {
                    Column(Modifier.fillMaxSize()) {
                        SearchableListHeader(
                            title = "Records",
                            displayState = displayState,
                            onAddClick = {},
                            displayActions = displayActions,
                        )
                        CollapsibleListContent(
                            listState =
                                CollapsibleListState(
                                    visibleItems = filteredRecords.visibleForListDisplay(displayState),
                                    filteredItemCount = filteredRecords.size,
                                    displayState = displayState,
                                ),
                            displayActions = displayActions,
                            itemKey = { it },
                            modifier = Modifier.weight(1f),
                            itemContent = { record -> Text(record) },
                        )
                    }
                }
            }

            onNodeWithText("Search").assertIsDisplayed()
            onNodeWithText("Show all 6").assertIsDisplayed()
            assertTrue(onAllNodesWithText("Record 6").fetchSemanticsNodes().isEmpty())

            onNodeWithText("Search").performClick()
            assertTrue(onAllNodesWithContentDescription("Search Records").fetchSemanticsNodes().isNotEmpty())

            onNodeWithText("Close").performClick()
            onNodeWithText("Show all 6").assertIsDisplayed()

            onNodeWithText("Show all 6").performClick()
            onNodeWithText("Show less").assertIsDisplayed()
            assertTrue(onAllNodesWithText("Record 6").fetchSemanticsNodes().isNotEmpty())
        }
}
