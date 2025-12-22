package org.tameter.iet.model

import org.tameter.iet.model.bridge.ModelBridge
import org.tameter.iet.model.bridge.TableReadModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Functionality: Bridge read model snapshot (stable IDs, pinned footer rows)
 *
 * Requirements refs:
 * - Stage 2 — Model–UI Bridge (observability via StateFlow)
 * - Events: cell edited, recompute complete
 * - Stable identifiers: row/column IDs propagated to events and read model
 * - Derived read models: flattened rows including pinned footer rows (totals)
 */
class ModelBridgeReadModelTest {
    @Test
    fun read_model_contains_stable_ids_and_pinned_footer_totals_rows() {
        // Given a table with mixed requirement types
        val perfA = PerformanceRequirement("P1", "ms", current = 100.0, goal = 200.0)
        val perfB = PerformanceRequirement("P2", "%", current = 20.0, goal = 50.0)
        val res = ResourceRequirement("R1", "$", budget = 100.0)
        val idea = DesignIdea("D1")
        val table = ImpactEstimationTable(requirements = listOf(perfA, perfB, res), ideas = listOf(idea))
        val bridge = ModelBridge(table)

        // When inspecting the read model snapshot
        val rm: TableReadModel = bridge.readModel.value

        // Then it exposes column and row stable IDs and includes pinned footer rows (totals/ratio)
        // Note: exact row ordering not asserted here; only the presence and flags.
        val columnIds = rm.columns.map { it.id }
        assertTrue(columnIds.contains("D1"), "Read model should include column with ID D1")

        val rowIds = rm.rows.map { it.id }
        assertTrue(rowIds.containsAll(listOf("P1", "P2", "R1")), "Read model should include all requirement row IDs")

        val pinnedCount = rm.rows.count { it.isPinnedFooter }
        assertTrue(pinnedCount >= 2, "Expected pinned footer rows (e.g., totals and ratio)")
    }
}
