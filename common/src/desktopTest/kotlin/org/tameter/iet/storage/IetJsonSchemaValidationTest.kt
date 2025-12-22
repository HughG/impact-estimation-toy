package org.tameter.iet.storage

import kotlin.test.Test
import kotlin.test.assertTrue
import org.tameter.iet.model.*
import java.io.File
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.fasterxml.jackson.databind.ObjectMapper

class IetJsonSchemaValidationTest {
    @Test
    fun testJsonMatchesSchema() {
        // Given
        val r1 = PerformanceRequirement(id = "Perf1", name = "P1", unit = "ms", current = 100.0, goal = 50.0)
        val r2 = ResourceRequirement(id = "Cost1", name = "C1", unit = "$", budget = 1000.0)
        val ideaA = DesignIdea(id = "A", name = "Idea A")
        val table = ImpactEstimationTable(requirements = listOf(r1, r2), ideas = listOf(ideaA))
        table.setEstimation(0, 0, Estimation(estimatedValue = 80.0, confidenceRange = 5.0))
        table.setEstimation(1, 0, Estimation(estimatedValue = 900.0, confidenceRange = 50.0))

        val json = IetJsonStorage.toJson(table)

        // When: Validate against schema
        val schemaFile = File("../docs/schema/iet.schema.json")
        assertTrue(schemaFile.exists(), "Schema file should exist at ${schemaFile.absolutePath}")

        val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
        val schema = factory.getSchema(schemaFile.readText())
        
        val mapper = ObjectMapper()
        val node = mapper.readTree(json)
        val errors = schema.validate(node)

        // Then: No validation errors
        assertTrue(errors.isEmpty(), "JSON should match schema. Errors: $errors\nJSON:\n$json")
    }
}
