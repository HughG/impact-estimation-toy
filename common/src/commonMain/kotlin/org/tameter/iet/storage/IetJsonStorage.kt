package org.tameter.iet.storage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.tameter.iet.model.*

/**
 * Stage 3 (Storage): JSON serialization facade using kotlinx.serialization.
 */
object IetJsonStorage {
    private val jsonConfig = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "requirementType"
    }

    /** Serialize the user-input state of the table to a JSON string. */
    fun toJson(table: ImpactEstimationTable): String {
        return jsonConfig.encodeToString(table)
    }

    /** Deserialize the user-input state from a JSON string into a new table. */
    fun fromJson(jsonText: String): ImpactEstimationTable {
        return jsonConfig.decodeFromString(jsonText)
    }
}
