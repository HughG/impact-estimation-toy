package org.tameter.iet.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Functionality: Domain computations and validation (Stage-agnostic)
 * - Impact percent for Performance/Resource
 * - Totals and Performance-to-Cost Ratio
 * - Validation and N/A cases
 *
 * Requirements refs:
 * - IET/Model/Entities (Requirement, DesignIdea, Estimation)
 * - IET/Model/EstimationCell (impact percent, confidence)
 * - IET/Model/Grouping/Totals (group totals)
 * - IET/Model/Output/PerformanceToResourceRatio (ratio, N/A when resource total is 0)
 * - Validation rules listed under Stage 1 plan
 *
 * Given/When/Then notes inline per test.
 */
class ModelDomainComputationTest {
    @Test
    fun impact_for_performance_requirement_is_percentage_of_delta() {
        // Given a performance requirement with an estimate halfway between current and goal
        val req = PerformanceRequirement(
            id = "Perf1",
            name = "P1Name",
            unit = "ms",
            current = 100.0,
            goal = 200.0,
        )
        val est = Estimation(estimatedValue = 150.0)

        // When computing impact
        val impact = req.computeImpact(est)

        // Then percent is 50%
        assertTrue(impact is CellImpact.Valid)
        assertEquals(50.0, impact.percent, 1e-9)
    }

    @Test
    fun impact_for_resource_requirement_is_percentage_of_budget_saved() {
        // Given a resource requirement with budget 100 and estimated spend 50 (minimize)
        val req = ResourceRequirement(
            id = "Res1",
            name = "Res1Name",
            unit = "$",
            budget = 100.0,
        )
        val est = Estimation(estimatedValue = 50.0)

        val impact = req.computeImpact(est)

        // (100 - 50) / 100 = 0.5 => 50%
        assertTrue(impact is CellImpact.Valid)
        assertEquals(50.0, impact.percent, 1e-9)
    }

    @Test
    fun totals_and_ratio_across_rows_per_column() {
        // Given two performance rows and one resource row, and one design idea column
        val perfA = PerformanceRequirement("P1", "P1Name", "ms", current = 100.0, goal = 200.0)
        val perfB = PerformanceRequirement("P2", "P2Name", "%", current = 20.0, goal = 50.0)
        val res = ResourceRequirement("R1", "R1Name", "$", budget = 100.0)
        val idea = DesignIdea("D1", name = "Idea 1")

        val table = ImpactEstimationTable(requirements = listOf(perfA, perfB, res), ideas = listOf(idea))
        // Estimations
        table.setEstimation(0, 0, Estimation(estimatedValue = 150.0)) // 50%
        table.setEstimation(1, 0, Estimation(estimatedValue = 35.0))  // (35-20)/(50-20)=0.5 -> 50%
        table.setEstimation(2, 0, Estimation(estimatedValue = 50.0))  // 50% resource use

        // When computing totals
        val perfTotal = table.totalForType(0, RequirementType.Performance)
        val resTotal = table.totalForType(0, RequirementType.Resource)
        val ratio = table.performanceToCostRatio(0)

        // Then totals are sums and ratio divides them
        assertEquals(100.0, perfTotal!!, 1e-9)
        assertEquals(50.0, resTotal!!, 1e-9)
        assertEquals(2.0, ratio!!, 1e-9)
    }

    @Test
    fun validation_goal_equals_current_on_performance_makes_cell_invalid_and_excluded_from_totals() {
        // Given a performance row with goal == current
        val perf = PerformanceRequirement("Pbad", "PbadName", "ms", current = 100.0, goal = 100.0)
        val res = ResourceRequirement("R1", "R1Name", "$", budget = 100.0)
        val idea = DesignIdea("D1", name = "Idea 1")
        val table = ImpactEstimationTable(requirements = listOf(perf, res), ideas = listOf(idea))
        table.setEstimation(0, 0, Estimation(estimatedValue = 110.0)) // invalid perf
        table.setEstimation(1, 0, Estimation(estimatedValue = 50.0))  // 50% resource use

        // When computing cell impact and totals
        val cell = table.computeCellImpact(0, 0)
        val perfTotal = table.totalForType(0, RequirementType.Performance)
        val resTotal = table.totalForType(0, RequirementType.Resource)
        val ratio = table.performanceToCostRatio(0)

        // Then cell is invalid, perf total excludes it (null means nothing to sum), ratio defined using perf null => null
        assertTrue(cell is CellImpact.Invalid)
        assertNull(perfTotal)
        assertEquals(50.0, resTotal!!, 1e-9)
        assertNull(ratio)
    }

    @Test
    fun ratio_is_undefined_when_resource_total_is_zero() {
        // Given a perf improvement but zero resource impact
        val perf = PerformanceRequirement("P1", "P1Name", "ms", current = 100.0, goal = 200.0)
        val res = ResourceRequirement("R1", "R1Name", "$", budget = 100.0)
        val idea = DesignIdea("D1", name = "Idea 1")
        val table = ImpactEstimationTable(requirements = listOf(perf, res), ideas = listOf(idea))
        table.setEstimation(0, 0, Estimation(estimatedValue = 150.0)) // +50% perf
        table.setEstimation(1, 0, Estimation(estimatedValue = 100.0)) // 0% resource (spends all budget)

        // When computing the ratio
        val ratio = table.performanceToCostRatio(0)

        // Then undefined (N/A)
        assertNull(ratio)
    }
}
