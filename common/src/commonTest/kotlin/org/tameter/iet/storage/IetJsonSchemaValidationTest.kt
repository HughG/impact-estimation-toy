package org.tameter.iet.storage

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import org.tameter.iet.model.DesignIdea
import org.tameter.iet.model.Estimation
import org.tameter.iet.model.ImpactEstimationTable
import org.tameter.iet.model.PerformanceRequirement
import org.tameter.iet.model.ResourceRequirement
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class IetJsonSchemaValidationTest {
    @Test
    fun testJsonMatchesSchema() {
        // Given
        val r1 = PerformanceRequirement(id = "Perf1", unit = "ms", current = 100.0, goal = 50.0)
        val r2 = ResourceRequirement(id = "Cost1", unit = "$", budget = 1000.0)
        val ideaA = DesignIdea(id = "A")
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