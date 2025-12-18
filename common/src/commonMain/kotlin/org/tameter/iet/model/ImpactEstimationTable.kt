package org.tameter.iet.model

/**
 * Stage 1: Table aggregate that maintains insertion order for rows and columns.
 */
class ImpactEstimationTable(
    requirements: List<Requirement> = emptyList(),
    ideas: List<DesignIdea> = emptyList(),
) {
    val requirements: MutableList<Requirement> = requirements.toMutableList()
    val ideas: MutableList<DesignIdea> = ideas.toMutableList()

    // Keyed by pair of indices (reqIdx, ideaIdx) to preserve ordering and allow stable ids via indices for now.
    private val cells: MutableMap<Pair<Int, Int>, Estimation> = LinkedHashMap()

    fun setEstimation(reqIndex: Int, ideaIndex: Int, estimation: Estimation) {
        check(reqIndex in requirements.indices) { "Requirement index out of bounds" }
        check(ideaIndex in ideas.indices) { "DesignIdea index out of bounds" }
        cells[reqIndex to ideaIndex] = estimation
    }

    fun getEstimation(reqIndex: Int, ideaIndex: Int): Estimation? = cells[reqIndex to ideaIndex]

    fun computeCellImpact(reqIndex: Int, ideaIndex: Int): CellImpact? {
        val est = getEstimation(reqIndex, ideaIndex) ?: return null
        val req = requirements[reqIndex]
        return computeImpact(req, est)
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
