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
    fun row_and_column_operations_emit_events_and_update_read_model() {
        // Given an empty table/bridge
        val table = ImpactEstimationTable()
        val bridge = ModelBridge(table)

        // When adding a row and a column via the bridge
        val newReq = PerformanceRequirement("NewPerf", "ms", current = 0.0, goal = 100.0)
        bridge.addRow(newReq)
        val newIdea = DesignIdea("NewIdea", name = "Idea X")
        bridge.addColumn(newIdea)

        // Then corresponding events should be observed and read model updated
        val evt1 = bridge.events.value
        assertTrue(evt1 is ModelEvent.ColumnAdded || evt1 is ModelEvent.RowAdded, "Expected RowAdded/ColumnAdded event")

        val rm = bridge.readModel.value
        val rowIds = rm.rows.map { it.id }
        val colIds = rm.columns.map { it.id }
        assertTrue(rowIds.contains("NewPerf"), "Read model should include newly added row")
        assertTrue(colIds.contains("NewIdea"), "Read model should include newly added column")

        // When reordering and removing
        // (Use simple indices; exact ordering semantics verified later.)
        bridge.reorderRows(0, 0)
        bridge.reorderColumns(0, 0)
        bridge.removeRow("NewPerf")
        bridge.removeColumn("NewIdea")

        // Then corresponding events should be emitted; for brevity, check last event type belongs to set
        val evt2 = bridge.events.value
        val allowed = setOf(
            ModelEvent.RowRemoved::class,
            ModelEvent.ColumnRemoved::class,
            ModelEvent.RowReordered::class,
            ModelEvent.ColumnReordered::class
        )
        assertTrue(allowed.contains(evt2!!::class), "Expected one of the row/column operation events")

        // And the read model reflects removals
        val rm2 = bridge.readModel.value
        assertTrue(rm2.rows.none { it.id == "NewPerf" }, "Removed row should not be present in read model")
        assertTrue(rm2.columns.none { it.id == "NewIdea" }, "Removed column should not be present in read model")
    }
}
