package org.tameter.iet.model

/**
 * Stage 1: Minimal recalculation service skeleton with callback hooks.
 * UI layer can subscribe to be notified after recomputation.
 */
class RecalculationService {
    fun interface Listener {
        fun onRecomputed()
    }

    private val listeners: MutableList<Listener> = mutableListOf()

    fun addListener(listener: Listener) { listeners += listener }
    fun removeListener(listener: Listener) { listeners -= listener }

    /**
        Recompute derived outputs for the table. For now, the computation is on-demand and
        stateless; this method simply triggers listeners to re-read from the table.
     */
    fun recompute(table: ImpactEstimationTable) {
        // In future: cache derived read models. For Stage 1, no-op then notify.
        listeners.forEach { it.onRecomputed() }
    }
}
