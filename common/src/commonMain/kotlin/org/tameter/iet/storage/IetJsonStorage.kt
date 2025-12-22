package org.tameter.iet.storage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.tameter.iet.model.*

/**
 * Stage 3 (Storage): JSON serialization facade using kotlinx.serialization.
 */
object IetJsonStorage {
    private const val CURRENT_SCHEMA_VERSION: Int = 1
    private const val SCHEMA_REF: String = "docs/schema/iet.schema.json"

    private val jsonConfig = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "requirementType"
    }

    @Serializable
    private data class TableDto(
        @SerialName("$" + "schema")
        val schema: String,
        val schemaVersion: Int,
        val requirements: List<Requirement>,
        val ideas: List<DesignIdea>,
        val cells: List<CellDto>
    )

    @Serializable
    private data class CellDto(
        val rowIndex: Int,
        val colIndex: Int,
        val estimatedValue: Double,
        val confidenceRange: Double? = null
    )

    /** Serialize the user-input state of the table to a JSON string. */
    fun toJson(table: ImpactEstimationTable): String {
        val cellDtos = mutableListOf<CellDto>()
        table.requirements.forEachIndexed { rIdx, _ ->
            table.ideas.forEachIndexed { cIdx, _ ->
                table.getEstimation(rIdx, cIdx)?.let { est ->
                    cellDtos.add(
                        CellDto(
                            rowIndex = rIdx,
                            colIndex = cIdx,
                            estimatedValue = est.estimatedValue,
                            confidenceRange = est.confidenceRange
                        )
                    )
                }
            }
        }

        val dto = TableDto(
            schema = SCHEMA_REF,
            schemaVersion = CURRENT_SCHEMA_VERSION,
            requirements = table.requirements,
            ideas = table.ideas,
            cells = cellDtos
        )
        return jsonConfig.encodeToString(dto)
    }

    /** Deserialize the user-input state from a JSON string into a new table. */
    fun fromJson(jsonText: String): ImpactEstimationTable {
        val dto = jsonConfig.decodeFromString<TableDto>(jsonText)
        require(dto.schemaVersion == CURRENT_SCHEMA_VERSION) {
            "Unsupported schemaVersion=${dto.schemaVersion} (expected $CURRENT_SCHEMA_VERSION)"
        }
        
        val table = ImpactEstimationTable(dto.requirements, dto.ideas)
        dto.cells.forEach { cell ->
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
