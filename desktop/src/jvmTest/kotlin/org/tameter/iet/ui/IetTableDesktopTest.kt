package org.tameter.iet.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import org.tameter.iet.model.DesignIdea
import org.tameter.iet.model.ImpactEstimationTable
import org.tameter.iet.model.PerformanceRequirement
import org.tameter.iet.model.ResourceRequirement
import org.tameter.iet.model.bridge.ModelBridge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule

class IetTableDesktopTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun placeholdersAreVisibleForEmptyFields() {
        val table = ImpactEstimationTable(
            requirements = listOf(
                PerformanceRequirement(id = "Perf-1", unit = "", current = 0.0, goal = 10.0),
                ResourceRequirement(id = "Res-1", unit = "", budget = 100.0)
            ),
            ideas = listOf(
                DesignIdea(id = "Idea A"),
                DesignIdea(id = "Idea B")
            )
        )
        val modelBridge = ModelBridge(table)

        composeTestRule.setContent {
            MaterialTheme {
                IetTable(modelBridge)
            }
        }

        composeTestRule.waitForIdle()

        val unitNodes = fetchNodesWithText("unit")
        assertEquals(2, unitNodes.size, "Expected a unit placeholder for each requirement row.")
        assertAllHaveNonZeroWidth(unitNodes, "unit")

        val valNodes = fetchNodesWithText("val")
        assertEquals(4, valNodes.size, "Expected a value placeholder for each cell.")
        assertAllHaveNonZeroWidth(valNodes, "val")

        val confNodes = fetchNodesWithText("conf")
        assertEquals(4, confNodes.size, "Expected a confidence placeholder for each cell.")
        assertAllHaveNonZeroWidth(confNodes, "conf")
    }

    private fun fetchNodesWithText(text: String): List<SemanticsNode> {
        return composeTestRule
            .onAllNodesWithText(text, useUnmergedTree = true)
            .fetchSemanticsNodes()
    }

    private fun assertAllHaveNonZeroWidth(nodes: List<SemanticsNode>, label: String) {
        nodes.forEach { node ->
            assertTrue(node.boundsInRoot.width > 0f, "Expected $label placeholder to have width > 0.")
        }
    }
}
