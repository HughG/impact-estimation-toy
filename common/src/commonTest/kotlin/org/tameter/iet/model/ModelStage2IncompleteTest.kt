package org.tameter.iet.model

import org.tameter.iet.model.bridge.ModelBridge
import org.tameter.iet.model.bridge.ModelEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Requirements refs:
 * - Stage 2 — Model–UI Bridge
 *   - RecomputeComplete event should be emitted after recomputation (currently missing) [IET/Stage2/Events]
 *   - Provide operations to add/remove/reorder rows and columns [IET/Stage2/Operations]
 *
 * Given/When/Then notes inline.
 */
class ModelStage2IncompleteTest {
    @Test
    fun recompute_complete_event_emitted_after_edit() {
        // Given a table and bridge
        val perf = PerformanceRequirement("P1", "ms", current = 100.0, goal = 200.0)
        val res = ResourceRequirement("R1", "$", budget = 100.0)
        val idea = DesignIdea("D1", name = "Idea 1")
        val table = ImpactEstimationTable(requirements = listOf(perf, res), ideas = listOf(idea))
        val bridge = ModelBridge(table)

        // When editing a cell
        bridge.setEstimation(0, 0, Estimation(estimatedValue = 150.0))

        // Then the bridge should emit RecomputeComplete after recomputation
        // (Currently not implemented; this test captures the requirement.)
        val evt = bridge.events.value
        assertTrue(evt is ModelEvent.RecomputeComplete, "Expected RecomputeComplete event after edit")
    }

    @Test
    fun row_add_emits_event_and_updates_read_model() {
        // Given an empty table/bridge
        val bridge = ModelBridge(ImpactEstimationTable())

        // When adding a row via the bridge
        val newReq = PerformanceRequirement("NewPerf", "ms", current = 0.0, goal = 100.0)
        bridge.addRow(newReq)

        // Then RowAdded event is emitted and read model includes the row
        val evt = bridge.events.value
        assertTrue(evt is ModelEvent.RowAdded, "Expected RowAdded event")
        val rm = bridge.readModel.value
        assertTrue(rm.rows.any { it.id == "NewPerf" }, "Read model should include newly added row")
    }

    @Test
    fun column_add_emits_event_and_updates_read_model() {
        // Given an empty table/bridge
        val bridge = ModelBridge(ImpactEstimationTable())

        // When adding a column via the bridge
        val newIdea = DesignIdea("NewIdea", name = "Idea X")
        bridge.addColumn(newIdea)

        // Then ColumnAdded event is emitted and read model includes the column
        val evt = bridge.events.value
        assertTrue(evt is ModelEvent.ColumnAdded, "Expected ColumnAdded event")
        val rm = bridge.readModel.value
        assertTrue(rm.columns.any { it.id == "NewIdea" }, "Read model should include newly added column")
    }

    @Test
    fun row_reorder_emits_event_and_updates_read_model_order() {
        // Given a table with two rows
        val r1 = PerformanceRequirement("R1", "ms", current = 0.0, goal = 10.0)
        val r2 = ResourceRequirement("R2", "$", budget = 5.0)
        val bridge = ModelBridge(ImpactEstimationTable(requirements = listOf(r1, r2)))

        // When reordering rows
        bridge.reorderRows(0, 1)

        // Then RowReordered event is emitted
        val evt = bridge.events.value
        assertTrue(evt is ModelEvent.RowReordered, "Expected RowReordered event")

        // And the read model reflects the new order (non-footer rows only)
        val rm = bridge.readModel.value
        val nonFooterIds = rm.rows.filter { !it.isPinnedFooter }.map { it.id }
        assertEquals(listOf("R2", "R1"), nonFooterIds, "Rows should be reordered in read model")
    }

    @Test
    fun column_reorder_emits_event_and_updates_read_model_order() {
        // Given a table with two columns
        val c1 = DesignIdea("C1", name = "Idea 1")
        val c2 = DesignIdea("C2", name = "Idea 2")
        val bridge = ModelBridge(ImpactEstimationTable(ideas = listOf(c1, c2)))

        // When reordering columns
        bridge.reorderColumns(0, 1)

        // Then ColumnReordered event is emitted
        val evt = bridge.events.value
        assertTrue(evt is ModelEvent.ColumnReordered, "Expected ColumnReordered event")

        // And the read model reflects the new column order
        val rm = bridge.readModel.value
        val colIds = rm.columns.map { it.id }
        assertEquals(listOf("C2", "C1"), colIds, "Columns should be reordered in read model")
    }

    @Test
    fun row_remove_emits_event_and_updates_read_model() {
        // Given a table with a row
        val r = PerformanceRequirement("Rmv", "ms", current = 0.0, goal = 10.0)
        val bridge = ModelBridge(ImpactEstimationTable(requirements = listOf(r)))

        // When removing the row by ID
        bridge.removeRow("Rmv")

        // Then RowRemoved event is emitted and read model no longer contains it
        val evt = bridge.events.value
        assertTrue(evt is ModelEvent.RowRemoved, "Expected RowRemoved event")
        val rm = bridge.readModel.value
        assertTrue(rm.rows.none { it.id == "Rmv" }, "Removed row should not be present in read model")
    }

    @Test
    fun column_remove_emits_event_and_updates_read_model() {
        // Given a table with a column
        val bridge = ModelBridge(ImpactEstimationTable(ideas = listOf(DesignIdea("Cmv", name = "Idea"))))

        // When removing the column by ID
        bridge.removeColumn("Cmv")

        // Then ColumnRemoved event is emitted and read model no longer contains it
        val evt = bridge.events.value
        assertTrue(evt is ModelEvent.ColumnRemoved, "Expected ColumnRemoved event")
        val rm = bridge.readModel.value
        assertTrue(rm.columns.none { it.id == "Cmv" }, "Removed column should not be present in read model")
    }
}
