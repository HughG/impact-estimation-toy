@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.tameter.iet.model

import org.tameter.iet.model.bridge.ModelBridge
import org.tameter.iet.model.bridge.ModelEvent
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Functionality: Bridge events (CellEdited, RecomputeComplete)
 * - Ensures ordered emission on edit
 * - Ensures RecomputeComplete after recomputation
 *
 * Requirements refs:
 * - Stage 2 — Model–UI Bridge (observability via StateFlow)
 * - Events: cell edited, recompute complete
 */
class ModelBridgeEventsTest {
    @Test
    fun recompute_complete_event_emitted_after_edit() {
        // Given a table and bridge
        val perf = PerformanceRequirement("P1", "ms", current = 100.0, goal = 200.0)
        val res = ResourceRequirement("R1", "$", budget = 100.0)
        val idea = DesignIdea("D1")
        val table = ImpactEstimationTable(requirements = listOf(perf, res), ideas = listOf(idea))
        val bridge = ModelBridge(table)

        // When editing a cell, subscribe to events first to avoid missing emissions
        runTest {
            val ch = Channel<ModelEvent>(capacity = 2)
            val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                bridge.events
                    .take(2)
                    .collect { e -> ch.trySend(e) }
            }

            bridge.setEstimation(0, 0, Estimation(estimatedValue = 150.0))
            advanceUntilIdle()

            // First should be CellEdited, second should be RecomputeComplete
            val first = withTimeout(1_500) { ch.receive() }
            val second = withTimeout(1_500) { ch.receive() }
            collector.cancel()
            assertTrue(first is ModelEvent.CellEdited, "Expected first to be CellEdited")
            assertTrue(second is ModelEvent.RecomputeComplete, "Expected RecomputeComplete event after edit")
        }
    }
}
