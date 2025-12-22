package org.tameter.iet.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import org.tameter.iet.model.*

/**
 * Requirements: IET/Model/Storage
 * Given/When/Then:
 * - Given a small ImpactEstimationTable with mixed requirement types and some estimations
 * - When we serialize to JSON and then deserialize back
 * - Then the user inputs round-trip equivalently and the order of rows/columns is preserved.
 */
class IetJsonStorageTest {
    @Test
    fun testRoundTripPreservesInputsAndOrder() {
        // Given
        val r1 = PerformanceRequirement(id = "Perf1", name = "P1", unit = "ms", current = 100.0, goal = 50.0)
        val r2 = ResourceRequirement(id = "Cost1", name = "C1", unit = "$", budget = 1000.0)
        val ideaA = DesignIdea(id = "A", name = "Idea A")
        val ideaB = DesignIdea(id = "B", name = "Idea B")

        val table = ImpactEstimationTable(requirements = listOf(r1, r2), ideas = listOf(ideaA, ideaB))
        table.setEstimation(0, 0, Estimation(estimatedValue = 80.0, confidenceRange = 5.0))
        table.setEstimation(0, 1, Estimation(estimatedValue = 60.0, confidenceRange = null))
        table.setEstimation(1, 0, Estimation(estimatedValue = 900.0, confidenceRange = 50.0))

        // When
        val json = IetJsonStorage.toJson(table)
        val loaded = IetJsonStorage.fromJson(json)

        // Then: same number and order of requirements and ideas
        assertEquals(listOf("Perf1", "Cost1"), loaded.requirements.map { it.id })
        assertEquals(listOf("P1", "C1"), loaded.requirements.map { it.name })
        assertEquals(listOf(RequirementType.Performance, RequirementType.Resource), loaded.requirements.map { it.type })
        assertEquals(listOf("A", "B"), loaded.ideas.map { it.id })

        // Then: estimations preserved per (row, col)
        fun Estimation?.asPair() = this?.let { it.estimatedValue to it.confidenceRange }
        assertEquals(table.getEstimation(0, 0).asPair(), loaded.getEstimation(0, 0).asPair())
        assertEquals(table.getEstimation(0, 1).asPair(), loaded.getEstimation(0, 1).asPair())
        assertEquals(table.getEstimation(1, 0).asPair(), loaded.getEstimation(1, 0).asPair())
        // Unset cells remain unset
        assertEquals(null, loaded.getEstimation(1, 1))
    }

    /**
     * Given/When/Then:
     * - Given any table
     * - When serialized to JSON
     * - Then the root contains $schema and schemaVersion fields for human-readable schema reference and versioning.
     */
    @Test
    fun testJsonRootContainsSchemaFields() {
        val table = ImpactEstimationTable(
            requirements = listOf(
                PerformanceRequirement("R", name = "RName", unit = "%", current = 0.0, goal = 100.0)
            ),
            ideas = listOf(DesignIdea("X", name = "X"))
        )
        table.setEstimation(0, 0, Estimation(1.0))
        val json = IetJsonStorage.toJson(table)
        // simple textual checks to avoid depending on a specific JSON library in tests
        assertTrue(json.contains("\"" + "\$schema" + "\""), "JSON should contain \$schema field")
        assertTrue(json.contains("\"schemaVersion\""), "JSON should contain schemaVersion field")
        assertTrue(json.contains("\"performanceRequirements\""), "JSON should contain performanceRequirements field")
        assertTrue(json.contains("\"resourceRequirements\""), "JSON should contain resourceRequirements field")
        assertTrue(json.contains("\"designIdeas\""), "JSON should contain designIdeas field")
        assertTrue(json.contains("\"cells\""), "JSON should contain cells field")
        
        // Check property names in cells
        assertTrue(json.contains("\"requirementId\""), "JSON cells should use requirementId")
        assertTrue(json.contains("\"designId\""), "JSON cells should use designId")
        assertFalse(json.contains("\"rowIndex\""), "JSON cells should NOT use rowIndex")
        assertFalse(json.contains("\"colIndex\""), "JSON cells should NOT use colIndex")
    }

    @Test
    fun testCellConfidenceFieldName() {
        val r1 = PerformanceRequirement(id = "Perf1", name = "P1", unit = "ms", current = 100.0, goal = 50.0)
        val ideaA = DesignIdea(id = "A", name = "Idea A")
        val table = ImpactEstimationTable(requirements = listOf(r1), ideas = listOf(ideaA))
        table.setEstimation(0, 0, Estimation(estimatedValue = 80.0, confidenceRange = 5.0))

        val json = IetJsonStorage.toJson(table)
        assertTrue(json.contains("\"confidence\""), "JSON should use 'confidence' instead of 'confidenceRange'")
        assertFalse(json.contains("\"confidenceRange\""), "JSON should NOT use 'confidenceRange'")
    }
}
