package org.tameter.iet.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Requirements refs:
 * - IET/Model/Entities (QualityRequirement, DesignIdea, Estimation)
 * - IET/Model/EstimationCell (impact percent, confidence)
 * - IET/Model/Grouping/Totals (group totals)
 * - IET/Model/Output/PerformanceToCostRatio (ratio, N/A when cost total is 0)
 * - Validation rules listed under Stage 1 plan
 *
 * Given/When/Then style notes inline per test.
 */
class ModelStage1Test {
    @Test
    fun impact_for_performance_requirement_is_percentage_of_delta() {
        // Given a performance requirement with an estimate halfway between current and goal
        val req = QualityRequirement(
            id = "Perf1",
            unit = "ms",
            type = RequirementType.Performance,
            current = 100.0,
            goal = 200.0,
        )
        val est = Estimation(estimatedValue = 150.0)

        // When computing impact
        val impact = computeImpact(req, est)

        // Then percent is 50%
        assertTrue(impact is CellImpact.Valid)
        assertEquals(50.0, impact.percent, 1e-9)
    }

    // TODO 2025-12-17 hughg: This test and the implementation are wrong for Cost (so I probably specified it wrongly).
    // A higher estimated cost should give a higher output value, so that the resulting perf/cost ratio is lower.
    @Test
    fun impact_for_cost_requirement_handles_decrease_towards_lower_goal() {
        // Given a cost requirement 100 -> 60 (minimize)
        val req = QualityRequirement(
            id = "Cost1",
            unit = "$",
            type = RequirementType.Cost,
            current = 100.0,
            goal = 60.0,
        )
        val est = Estimation(estimatedValue = 80.0)

        val impact = computeImpact(req, est)

        // (80 - 100) / (60 - 100) = 0.5 => 50%
        assertTrue(impact is CellImpact.Valid)
        assertEquals(50.0, impact.percent, 1e-9)
    }

    @Test
    fun totals_and_ratio_across_rows_per_column() {
        // Given two performance rows and one cost row, and one design idea column
        val perfA = QualityRequirement("P1", "ms", RequirementType.Performance, 100.0, 200.0)
        val perfB = QualityRequirement("P2", "%", RequirementType.Performance, 20.0, 50.0)
        val cost = QualityRequirement("C1", "$", RequirementType.Cost, 100.0, 60.0)
        val idea = DesignIdea("D1", name = "Idea 1")

        val table = ImpactEstimationTable(requirements = listOf(perfA, perfB, cost), ideas = listOf(idea))
        // Estimations
        table.setEstimation(0, 0, Estimation(estimatedValue = 150.0)) // 50%
        table.setEstimation(1, 0, Estimation(estimatedValue = 35.0))  // (35-20)/(50-20)=0.5 -> 50%
        table.setEstimation(2, 0, Estimation(estimatedValue = 80.0))  // 50% cost

        // When computing totals
        val perfTotal = table.totalForType(0, RequirementType.Performance)
        val costTotal = table.totalForType(0, RequirementType.Cost)
        val ratio = table.performanceToCostRatio(0)

        // Then totals are sums and ratio divides them
        assertEquals(100.0, perfTotal!!, 1e-9)
        assertEquals(50.0, costTotal!!, 1e-9)
        assertEquals(2.0, ratio!!, 1e-9)
    }

    @Test
    fun validation_goal_equals_current_on_performance_makes_cell_invalid_and_excluded_from_totals() {
        // Given a performance row with goal == current
        val perf = QualityRequirement("Pbad", "ms", RequirementType.Performance, 100.0, 100.0)
        val cost = QualityRequirement("C1", "$", RequirementType.Cost, 100.0, 60.0)
        val idea = DesignIdea("D1", name = "Idea 1")
        val table = ImpactEstimationTable(requirements = listOf(perf, cost), ideas = listOf(idea))
        table.setEstimation(0, 0, Estimation(estimatedValue = 110.0)) // invalid perf
        table.setEstimation(1, 0, Estimation(estimatedValue = 80.0))  // 50% cost

        // When computing cell impact and totals
        val cell = table.computeCellImpact(0, 0)
        val perfTotal = table.totalForType(0, RequirementType.Performance)
        val costTotal = table.totalForType(0, RequirementType.Cost)
        val ratio = table.performanceToCostRatio(0)

        // Then cell invalid, perf total excludes it (null means nothing to sum), ratio defined using perf null => null
        assertTrue(cell is CellImpact.Invalid)
        assertNull(perfTotal)
        assertEquals(50.0, costTotal!!, 1e-9)
        assertNull(ratio)
    }

    @Test
    fun ratio_is_undefined_when_cost_total_is_zero() {
        // Given a perf improvement but zero cost impact
        val perf = QualityRequirement("P1", "ms", RequirementType.Performance, 100.0, 200.0)
        val cost = QualityRequirement("C1", "$", RequirementType.Cost, 100.0, 60.0)
        val idea = DesignIdea("D1", name = "Idea 1")
        val table = ImpactEstimationTable(requirements = listOf(perf, cost), ideas = listOf(idea))
        table.setEstimation(0, 0, Estimation(estimatedValue = 150.0)) // +50% perf
        table.setEstimation(1, 0, Estimation(estimatedValue = 100.0)) // 0% cost

        // When computing ratio
        val ratio = table.performanceToCostRatio(0)

        // Then undefined (N/A)
        assertNull(ratio)
    }
}
