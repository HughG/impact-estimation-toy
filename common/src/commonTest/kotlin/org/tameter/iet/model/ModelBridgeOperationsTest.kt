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
            val newReq = PerformanceRequirement("NewPerf", "ms", current = 0.0, goal = 100.0)
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
            val newIdea = DesignIdea("NewIdea")
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
        val r1 = PerformanceRequirement("R1", "ms", current = 0.0, goal = 10.0)
        val r2 = ResourceRequirement("R2", "$", budget = 5.0)
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
        val c1 = DesignIdea("C1")
        val c2 = DesignIdea("C2")
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
        val r1 = PerformanceRequirement("Rmv", "ms", current = 0.0, goal = 10.0)
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
        val c1 = DesignIdea("Cmv")
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
    @Test
    fun column_reorder_preserves_estimations() {
        // Given a table with two columns and one estimation
        val c1 = DesignIdea("C1")
        val c2 = DesignIdea("C2")
        val r1 = PerformanceRequirement("R1", "ms", 0.0, 10.0)
        val table = ImpactEstimationTable(requirements = listOf(r1), ideas = listOf(c1, c2))
        table.setEstimation(0, 0, Estimation(5.0)) // R1, C1
        val bridge = ModelBridge(table)

        runTest {
            // When reordering columns (C1, C2) -> (C2, C1)
            bridge.reorderColumns(0, 1)
            advanceUntilIdle()

            // Then R1, C1 estimation should still be 5.0 (now at index 1)
            val rm = bridge.readModel.value
            assertEquals(2, rm.columns.size)
            assertEquals("C2", rm.columns[0].id)
            assertEquals("C1", rm.columns[1].id)

            // Cell at (R1, C1) should have impact for 5.0 (50%)
            val rowR1 = rm.rows.find { it.id == "R1" }!!
            // C2 is at index 0, C1 is at index 1
            assertEquals(null, rowR1.cells[0].impactPercent, "C2 should have no estimation")
            assertEquals(50.0, rowR1.cells[1].impactPercent, "C1 should still have its estimation")
        }
    }

    @Test
    fun row_reorder_preserves_estimations() {
        // Given a table with two rows and one estimation
        val r1 = PerformanceRequirement("R1", "ms", 0.0, 10.0)
        val r2 = PerformanceRequirement("R2", "ms", 0.0, 10.0)
        val c1 = DesignIdea("C1")
        val table = ImpactEstimationTable(requirements = listOf(r1, r2), ideas = listOf(c1))
        table.setEstimation(0, 0, Estimation(5.0)) // R1, C1
        val bridge = ModelBridge(table)

        runTest {
            // When reordering rows (R1, R2) -> (R2, R1)
            bridge.reorderRows(0, 1)
            advanceUntilIdle()

            // Then R1, C1 estimation should still be 5.0 (now at index 1)
            val rm = bridge.readModel.value
            val r2View = rm.rows.find { it.id == "R2" }!!
            val r1View = rm.rows.find { it.id == "R1" }!!

            assertEquals(null, r2View.cells[0].impactPercent, "R2 should have no estimation")
            assertEquals(50.0, r1View.cells[0].impactPercent, "R1 should still have its estimation")
        }
    }

    @Test
    fun requirement_update_emits_event_and_updates_read_model() {
        // Given a table with one requirement
        val r1 = PerformanceRequirement("R1", "ms", current = 0.0, goal = 10.0)
        val i1 = DesignIdea("I1")
        val table = ImpactEstimationTable(requirements = listOf(r1), ideas = listOf(i1))
        table.setEstimation(0, 0, Estimation(5.0))
        val bridge = ModelBridge(table)

        runTest {
            // When updating the requirement (ID change)
            val ch = Channel<ModelEvent>(capacity = 1)
            val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                // Collect events until MetadataChanged
                bridge.events.collect { 
                    if (it is ModelEvent.MetadataChanged) {
                        ch.send(it)
                    }
                }
            }
            val updated = PerformanceRequirement("R1_New", "s", current = 0.0, goal = 1.0)
            bridge.updateRequirement("R1", updated)
            advanceUntilIdle()

            // Then MetadataChanged event is emitted
            val evt = withTimeout(1_500) { ch.receive() }
            collector.cancel()
            assertEquals("requirement", (evt as ModelEvent.MetadataChanged).what)

            // And read model is updated
            val rm = bridge.readModel.value
            val row = rm.rows.find { it.id == "R1_New" }
            assertEquals("s", row?.unit)
            assertEquals(0.0, row?.performanceDetails?.current)
            assertEquals(1.0, row?.performanceDetails?.goal)

            // And estimations are preserved for the new ID
            assertEquals(5.0, row?.cells?.find { it.columnId == "I1" }?.estimatedValue)
        }
    }

    @Test
    fun idea_update_emits_event_and_updates_read_model() {
        // Given a table with one idea
        val r1 = PerformanceRequirement("R1", "ms", current = 0.0, goal = 10.0)
        val i1 = DesignIdea("I1")
        val table = ImpactEstimationTable(requirements = listOf(r1), ideas = listOf(i1))
        table.setEstimation(0, 0, Estimation(5.0))
        val bridge = ModelBridge(table)

        runTest {
            // When updating the idea (ID change)
            val ch = Channel<ModelEvent>(capacity = 1)
            val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                bridge.events.collect { 
                    if (it is ModelEvent.MetadataChanged) {
                        ch.send(it)
                    }
                }
            }
            val updated = DesignIdea("I1_New")
            bridge.updateDesignIdea("I1", updated)
            advanceUntilIdle()

            // Then MetadataChanged event is emitted
            val evt = withTimeout(1_500) { ch.receive() }
            collector.cancel()
            assertEquals("idea", (evt as ModelEvent.MetadataChanged).what)

            // And read model is updated
            val rm = bridge.readModel.value
            assertTrue(rm.columns.any { it.id == "I1_New" })

            // And estimations are preserved for the new ID
            val row = rm.rows.find { it.id == "R1" }
            assertEquals(5.0, row?.cells?.find { it.columnId == "I1_New" }?.estimatedValue)
        }
    }
}
