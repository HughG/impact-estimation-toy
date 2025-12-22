@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.tameter.iet.model

import org.tameter.iet.model.bridge.ModelBridge
import org.tameter.iet.model.bridge.ModelEvent
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Functionality: Bridge structural operations (add/remove/reorder rows/columns)
 * - Emits appropriate events
 * - Updates read model (including order for reorders)
 *
 * Requirements refs:
 * - Stage 2 — Model–UI Bridge
 *   - RecomputeComplete event should be emitted after recomputation (currently missing) [IET/Stage2/Events]
 *   - Provide operations to add/remove/reorder rows and columns [IET/Stage2/Operations]
 *
 */
class ModelBridgeOperationsTest {
    @Test
    fun row_add_emits_event_and_updates_read_model() {
        // Given an empty table/bridge
        val bridge = ModelBridge(ImpactEstimationTable())

        runTest {
            // When adding a row via the bridge
            val ch = Channel<ModelEvent>(capacity = 1)
            val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                val e = bridge.events.first()
                ch.trySend(e)
            }
            val newReq = PerformanceRequirement("NewPerf", "NewPerfName", "ms", current = 0.0, goal = 100.0)
            bridge.addRow(newReq)
            advanceUntilIdle()

            // Then RowAdded event is emitted and read model includes the row
            val evt = withTimeout(1_500) { ch.receive() }
            collector.cancel()
            assertTrue(evt is ModelEvent.RowAdded, "Expected RowAdded event")
            val rm = bridge.readModel.value
            assertTrue(rm.rows.any { it.id == "NewPerf" }, "Read model should include newly added row")
        }
    }

    @Test
    fun column_add_emits_event_and_updates_read_model() {
        // Given an empty table/bridge
        val bridge = ModelBridge(ImpactEstimationTable())

        runTest {
            // When adding a column via the bridge
            val ch = Channel<ModelEvent>(capacity = 1)
            val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                val e = bridge.events.first()
                ch.trySend(e)
            }
            val newIdea = DesignIdea("NewIdea", name = "Idea X")
            bridge.addColumn(newIdea)
            advanceUntilIdle()

            // Then ColumnAdded event is emitted and read model includes the column
            val evt = withTimeout(1_500) { ch.receive() }
            collector.cancel()
            assertTrue(evt is ModelEvent.ColumnAdded, "Expected ColumnAdded event")
            val rm = bridge.readModel.value
            assertTrue(rm.columns.any { it.id == "NewIdea" }, "Read model should include newly added column")
        }
    }

    @Test
    fun row_reorder_emits_event_and_updates_read_model_order() {
        // Given a table with two rows
        val r1 = PerformanceRequirement("R1", "R1Name", "ms", current = 0.0, goal = 10.0)
        val r2 = ResourceRequirement("R2", "R2Name", "$", budget = 5.0)
        val bridge = ModelBridge(ImpactEstimationTable(requirements = listOf(r1, r2)))

        runTest {
            // When reordering rows
            val ch = Channel<ModelEvent>(capacity = 1)
            val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                val e = bridge.events.first()
                ch.trySend(e)
            }
            bridge.reorderRows(0, 1)
            advanceUntilIdle()

            // Then RowReordered event is emitted
            val evt = withTimeout(1_500) { ch.receive() }
            collector.cancel()
            assertTrue(evt is ModelEvent.RowReordered, "Expected RowReordered event")

            // And the read model reflects the new order (non-footer rows only)
            val rm = bridge.readModel.value
            val nonFooterIds = rm.rows.filter { !it.isPinnedFooter }.map { it.id }
            assertEquals(listOf("R2", "R1"), nonFooterIds, "Rows should be reordered in read model")
        }
    }

    @Test
    fun column_reorder_emits_event_and_updates_read_model_order() {
        // Given a table with two columns
        val c1 = DesignIdea("C1", name = "Idea 1")
        val c2 = DesignIdea("C2", name = "Idea 2")
        val bridge = ModelBridge(ImpactEstimationTable(ideas = listOf(c1, c2)))

        runTest {
            // When reordering columns
            val ch = Channel<ModelEvent>(capacity = 1)
            val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                val e = bridge.events.first()
                ch.trySend(e)
            }
            bridge.reorderColumns(0, 1)
            advanceUntilIdle()

            // Then ColumnReordered event is emitted
            val evt = withTimeout(1_500) { ch.receive() }
            collector.cancel()
            assertTrue(evt is ModelEvent.ColumnReordered, "Expected ColumnReordered event")

            // And the read model reflects the new column order
            val rm = bridge.readModel.value
            val colIds = rm.columns.map { it.id }
            assertEquals(listOf("C2", "C1"), colIds, "Columns should be reordered in read model")
        }
    }

    @Test
    fun row_remove_emits_event_and_updates_read_model() {
        // Given a table with one row
        val r1 = PerformanceRequirement("Rmv", "RmvName", "ms", current = 0.0, goal = 10.0)
        val bridge = ModelBridge(ImpactEstimationTable(requirements = listOf(r1)))

        runTest {
            // When removing the row by ID
            val ch = Channel<ModelEvent>(capacity = 1)
            val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                val e = bridge.events.first()
                ch.trySend(e)
            }
            bridge.removeRow("Rmv")
            advanceUntilIdle()

            // Then RowRemoved event is emitted and read model no longer contains it
            val evt = withTimeout(1_500) { ch.receive() }
            collector.cancel()
            assertTrue(evt is ModelEvent.RowRemoved, "Expected RowRemoved event")
            val rm = bridge.readModel.value
            assertTrue(rm.rows.none { it.id == "Rmv" }, "Removed row should not be present in read model")
        }
    }

    @Test
    fun column_remove_emits_event_and_updates_read_model() {
        // Given a table with one column
        val c1 = DesignIdea("Cmv", name = "Idea")
        val bridge = ModelBridge(ImpactEstimationTable(ideas = listOf(c1)))

        runTest {
            // When removing the column by ID
            val ch = Channel<ModelEvent>(capacity = 1)
            val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                val e = bridge.events.first()
                ch.trySend(e)
            }
            bridge.removeColumn("Cmv")
            advanceUntilIdle()

            // Then ColumnRemoved event is emitted and read model no longer contains it
            val evt = withTimeout(1_500) { ch.receive() }
            collector.cancel()
            assertTrue(evt is ModelEvent.ColumnRemoved, "Expected ColumnRemoved event")
            val rm = bridge.readModel.value
            assertTrue(rm.columns.none { it.id == "Cmv" }, "Removed column should not be present in read model")
        }
    }
}
