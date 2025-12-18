package org.tameter.iet.model.bridge

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.tameter.iet.model.Estimation
import org.tameter.iet.model.ImpactEstimationTable

/**
 * Stage 2 (stub): Model–UI bridge API. Minimal types to allow tests to compile.
 * Behavior will be implemented in a later green step.
 */
sealed interface ModelEvent {
    data class RowAdded(val rowId: String) : ModelEvent
    data class RowRemoved(val rowId: String) : ModelEvent
    data class RowReordered(val fromIndex: Int, val toIndex: Int) : ModelEvent
    data class ColumnAdded(val columnId: String) : ModelEvent
    data class ColumnRemoved(val columnId: String) : ModelEvent
    data class ColumnReordered(val fromIndex: Int, val toIndex: Int) : ModelEvent
    data class CellEdited(val rowId: String, val columnId: String) : ModelEvent
    data class MetadataChanged(val what: String) : ModelEvent
    data object RecomputeComplete : ModelEvent
}

data class ColumnView(
    val id: String,
    val name: String,
)

data class CellView(
    val rowId: String,
    val columnId: String,
    val impactPercent: Double?, // null if invalid or not computed
)

data class RowView(
    val id: String,
    val isPinnedFooter: Boolean,
    val cells: List<CellView>,
)

data class TableReadModel(
    val columns: List<ColumnView>,
    val rows: List<RowView>,
)

class ModelBridge(
    private val table: ImpactEstimationTable,
) {
    // Stub state flows. Real emissions to be implemented later.
    private val _events = MutableStateFlow<ModelEvent?>(null)
    val events: StateFlow<ModelEvent?> = _events

    private val _readModel = MutableStateFlow(TableReadModel(columns = emptyList(), rows = emptyList()))
    val readModel: StateFlow<TableReadModel> = _readModel

    fun setEstimation(rowIndex: Int, columnIndex: Int, estimation: Estimation) {
        // pass-through to model; event emission not yet implemented
        table.setEstimation(rowIndex, columnIndex, estimation)
    }
}
