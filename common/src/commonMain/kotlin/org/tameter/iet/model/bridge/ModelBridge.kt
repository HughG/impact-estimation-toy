package org.tameter.iet.model.bridge

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.tameter.iet.model.Estimation
import org.tameter.iet.model.ImpactEstimationTable
import org.tameter.iet.model.RequirementType
import org.tameter.iet.model.CellImpact
import org.tameter.iet.model.Requirement
import org.tameter.iet.model.DesignIdea

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
    private val _events = MutableSharedFlow<ModelEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<ModelEvent> = _events

    private val _readModel = MutableStateFlow(TableReadModel(columns = emptyList(), rows = emptyList()))
    val readModel: StateFlow<TableReadModel> = _readModel

    init {
        // Populate initial snapshot
        _readModel.value = buildReadModel()
    }

    fun setEstimation(rowIndex: Int, columnIndex: Int, estimation: Estimation) {
        // Update model
        table.setEstimation(rowIndex, columnIndex, estimation)

        // Emit CellEdited with stable IDs
        val rowId = table.requirements[rowIndex].id
        val columnId = table.ideas[columnIndex].id
        _events.tryEmit(ModelEvent.CellEdited(rowId = rowId, columnId = columnId))

        // Rebuild read model snapshot after change
        _readModel.value = buildReadModel()

        // Emit recompute completion after snapshot updated
        _events.tryEmit(ModelEvent.RecomputeComplete)
    }

    private fun buildReadModel(): TableReadModel {
        val columns = table.ideas.map { idea ->
            ColumnView(id = idea.id, name = idea.name)
        }

        // Regular rows (requirements)
        val normalRows = table.requirements.mapIndexed { reqIdx, req ->
            val cells = table.ideas.mapIndexed { ideaIdx, idea ->
                val impact = table.computeCellImpact(reqIdx, ideaIdx)
                val percent = when (impact) {
                    is CellImpact.Valid -> impact.percent
                    is CellImpact.Invalid -> null
                    null -> null
                }
                CellView(rowId = req.id, columnId = idea.id, impactPercent = percent)
            }
            RowView(id = req.id, isPinnedFooter = false, cells = cells)
        }

        // Pinned footer rows: at least two (Performance Totals and Resource Totals)
        val footerRows = mutableListOf<RowView>()
        // Build totals per idea for Performance
        run {
            val cells = table.ideas.mapIndexed { ideaIdx, idea ->
                val total = table.totalForType(ideaIdx, RequirementType.Performance)
                CellView(rowId = "__perf_totals__", columnId = idea.id, impactPercent = total)
            }
            footerRows += RowView(id = "__perf_totals__", isPinnedFooter = true, cells = cells)
        }
        // Build totals per idea for Resource
        run {
            val cells = table.ideas.mapIndexed { ideaIdx, idea ->
                val total = table.totalForType(ideaIdx, RequirementType.Resource)
                CellView(rowId = "__res_totals__", columnId = idea.id, impactPercent = total)
            }
            footerRows += RowView(id = "__res_totals__", isPinnedFooter = true, cells = cells)
        }

        // Optional: Ratio row (doesn't affect current tests but useful)
        run {
            val cells = table.ideas.mapIndexed { ideaIdx, idea ->
                val ratio = table.performanceToCostRatio(ideaIdx)
                CellView(rowId = "__ratio__", columnId = idea.id, impactPercent = ratio)
            }
            footerRows += RowView(id = "__ratio__", isPinnedFooter = true, cells = cells)
        }

        return TableReadModel(columns = columns, rows = normalRows + footerRows)
    }

    // Stage 2 (planned): operations for rows/columns — stubs added to support failing tests
    fun addRow(requirement: Requirement) {
        // For now, manipulate the underlying lists directly (ImpactEstimationTable exposes them as mutable lists)
        table.requirements.add(requirement)
        // Update snapshot first so UI can read after event
        _readModel.value = buildReadModel()
        _events.tryEmit(ModelEvent.RowAdded(requirement.id))
        _events.tryEmit(ModelEvent.RecomputeComplete)
    }

    fun removeRow(rowId: String) {
        val idx = table.requirements.indexOfFirst { it.id == rowId }
        if (idx >= 0) {
            table.requirements.removeAt(idx)
            _readModel.value = buildReadModel()
            _events.tryEmit(ModelEvent.RowRemoved(rowId))
            _events.tryEmit(ModelEvent.RecomputeComplete)
        }
    }

    fun reorderRows(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex !in table.requirements.indices) return
        var targetIndex = toIndex
        val item = table.requirements.removeAt(fromIndex)
        // After removal, if inserting beyond end, clamp to end
        if (targetIndex > table.requirements.size) targetIndex = table.requirements.size
        table.requirements.add(targetIndex, item)
        _readModel.value = buildReadModel()
        _events.tryEmit(ModelEvent.RowReordered(fromIndex, toIndex))
        _events.tryEmit(ModelEvent.RecomputeComplete)
    }

    fun addColumn(idea: DesignIdea) {
        table.ideas.add(idea)
        _readModel.value = buildReadModel()
        _events.tryEmit(ModelEvent.ColumnAdded(idea.id))
        _events.tryEmit(ModelEvent.RecomputeComplete)
    }

    fun removeColumn(columnId: String) {
        val idx = table.ideas.indexOfFirst { it.id == columnId }
        if (idx >= 0) {
            table.ideas.removeAt(idx)
            _readModel.value = buildReadModel()
            _events.tryEmit(ModelEvent.ColumnRemoved(columnId))
            _events.tryEmit(ModelEvent.RecomputeComplete)
        }
    }

    fun reorderColumns(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex !in table.ideas.indices) return
        var targetIndex = toIndex
        val item = table.ideas.removeAt(fromIndex)
        if (targetIndex > table.ideas.size) targetIndex = table.ideas.size
        table.ideas.add(targetIndex, item)
        _readModel.value = buildReadModel()
        _events.tryEmit(ModelEvent.ColumnReordered(fromIndex, toIndex))
        _events.tryEmit(ModelEvent.RecomputeComplete)
    }
}
