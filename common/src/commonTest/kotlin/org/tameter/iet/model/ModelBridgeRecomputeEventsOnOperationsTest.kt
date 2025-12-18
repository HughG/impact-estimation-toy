@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.tameter.iet.model

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.tameter.iet.model.bridge.ModelBridge
import org.tameter.iet.model.bridge.ModelEvent
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Functionality: RecomputeComplete emission after structural operations
 *
 * Given/When/Then:
 * - Given a ModelBridge over a table
 * - When performing structural operations (add/remove/reorder rows/columns)
 * - Then a RecomputeComplete event should be emitted after the structural event
 *
 * Requirements refs:
 * - Stage 2 — Model–UI Bridge: recompute complete should follow changes [IET/Stage2/Events]
 */
class ModelBridgeRecomputeEventsOnOperationsTest {
    @Test
    fun recompute_emitted_after_row_add() = runTest {
        // Given
        val bridge = ModelBridge(ImpactEstimationTable())

        // When
        val ch = Channel<ModelEvent>(capacity = 2)
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            bridge.events
                .take(2)
                .collect { e -> ch.trySend(e) }
        }
        bridge.addRow(PerformanceRequirement("P_new", "ms", 0.0, 1.0))
        advanceUntilIdle()

        // Then: first is RowAdded, second should be RecomputeComplete
        val first = withTimeout(1_500) { ch.receive() }
        val second = withTimeout(1_500) { ch.receive() }
        collector.cancel()
        assertTrue(first is ModelEvent.RowAdded, "Expected RowAdded first")
        assertTrue(second is ModelEvent.RecomputeComplete, "Expected RecomputeComplete after RowAdded")
    }

    @Test
    fun recompute_emitted_after_column_remove() = runTest {
        // Given a table with one column
        val c1 = DesignIdea("C1", name = "Idea")
        val bridge = ModelBridge(ImpactEstimationTable(ideas = listOf(c1)))

        // When
        val ch = Channel<ModelEvent>(capacity = 2)
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            bridge.events
                .take(2)
                .collect { e -> ch.trySend(e) }
        }
        bridge.removeColumn("C1")
        advanceUntilIdle()

        // Then: first is ColumnRemoved, second should be RecomputeComplete
        val first = withTimeout(1_500) { ch.receive() }
        val second = withTimeout(1_500) { ch.receive() }
        collector.cancel()
        assertTrue(first is ModelEvent.ColumnRemoved, "Expected ColumnRemoved first")
        assertTrue(second is ModelEvent.RecomputeComplete, "Expected RecomputeComplete after ColumnRemoved")
    }
}
