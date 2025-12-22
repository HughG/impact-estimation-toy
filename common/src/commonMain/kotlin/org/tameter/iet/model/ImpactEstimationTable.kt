package org.tameter.iet.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Stage 1: Table aggregate that maintains insertion order for rows and columns.
 */
@Serializable(with = ImpactEstimationTableSerializer::class)
class ImpactEstimationTable(
    requirements: List<Requirement> = emptyList(),
    ideas: List<DesignIdea> = emptyList(),
) {
    val requirements: MutableList<Requirement> = requirements.toMutableList()
    val ideas: MutableList<DesignIdea> = ideas.toMutableList()

    // Keyed by pair of indices (reqIdx, ideaIdx) to preserve ordering and allow stable ids via indices for now.
    internal val cells: MutableMap<Pair<Int, Int>, Estimation> = LinkedHashMap()

    fun setEstimation(reqIndex: Int, ideaIndex: Int, estimation: Estimation) {
        check(reqIndex in requirements.indices) { "Requirement index out of bounds" }
        check(ideaIndex in ideas.indices) { "DesignIdea index out of bounds" }
        cells[reqIndex to ideaIndex] = estimation
    }

    fun getEstimation(reqIndex: Int, ideaIndex: Int): Estimation? = cells[reqIndex to ideaIndex]

    fun computeCellImpact(reqIndex: Int, ideaIndex: Int): CellImpact? {
        val est = getEstimation(reqIndex, ideaIndex) ?: return null
        val req = requirements[reqIndex]
        return req.computeImpact(est)
    }

    fun totalForType(ideaIndex: Int, type: RequirementType): Double? {
        check(ideaIndex in ideas.indices) { "DesignIdea index out of bounds" }
        var sum = 0.0
        var hasAny = false
        requirements.forEachIndexed { reqIdx, req ->
            if (req.type == type) {
                when (val impact = computeCellImpact(reqIdx, ideaIndex)) {
                    is CellImpact.Valid -> {
                        sum += impact.percent
                        hasAny = true
                    }
                    is CellImpact.Invalid -> {
                        // skip invalid cells from totals
                    }
                    null -> {}
                }
            }
        }
        return if (hasAny) sum else null
    }

    /**
     * Performance-to-Cost Ratio per DesignIdea: TotalPerformance% / TotalResource%.
     * When TotalResource% is 0 or null, return null (undefined/N/A).
     */
    fun performanceToCostRatio(ideaIndex: Int): Double? {
        val perf = totalForType(ideaIndex, RequirementType.Performance)
        val resource = totalForType(ideaIndex, RequirementType.Resource)
        if (perf == null || resource == null) return null
        if (resource == 0.0) return null
        return perf / resource
    }
}

/**
 * Custom serializer for ImpactEstimationTable to manage JSON structure (including schema and version).
 */
object ImpactEstimationTableSerializer : KSerializer<ImpactEstimationTable> {
    private const val CURRENT_SCHEMA_VERSION: Int = 1
    private const val SCHEMA_REF: String = "docs/schema/iet.schema.json"

    @Serializable
    private data class TableSurrogate(
        @kotlinx.serialization.SerialName("$" + "schema")
        val schema: String = SCHEMA_REF,
        val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
        val requirements: List<Requirement>,
        val ideas: List<DesignIdea>,
        val cells: List<CellSurrogate>
    )

    @Serializable
    private data class CellSurrogate(
        val rowIndex: Int,
        val colIndex: Int,
        val estimatedValue: Double,
        val confidenceRange: Double? = null
    )

    override val descriptor: SerialDescriptor = TableSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: ImpactEstimationTable) {
        val cellSurrogates = value.cells.map { (indices, estimation) ->
            CellSurrogate(
                rowIndex = indices.first,
                colIndex = indices.second,
                estimatedValue = estimation.estimatedValue,
                confidenceRange = estimation.confidenceRange
            )
        }
        val surrogate = TableSurrogate(
            requirements = value.requirements,
            ideas = value.ideas,
            cells = cellSurrogates
        )
        encoder.encodeSerializableValue(TableSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): ImpactEstimationTable {
        val surrogate = decoder.decodeSerializableValue(TableSurrogate.serializer())
        require(surrogate.schemaVersion == CURRENT_SCHEMA_VERSION) {
            "Unsupported schemaVersion=${surrogate.schemaVersion} (expected $CURRENT_SCHEMA_VERSION)"
        }

        val table = ImpactEstimationTable(surrogate.requirements, surrogate.ideas)
        surrogate.cells.forEach { cell ->
            if (cell.rowIndex in table.requirements.indices && cell.colIndex in table.ideas.indices) {
                table.setEstimation(
                    cell.rowIndex,
                    cell.colIndex,
                    Estimation(cell.estimatedValue, cell.confidenceRange)
                )
            }
        }
        return table
    }
}
