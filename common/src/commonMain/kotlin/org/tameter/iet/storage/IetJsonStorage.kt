package org.tameter.iet.storage

import org.tameter.iet.model.*

/**
 * Stage 3 (Storage): JSON serialization facade.
 * Domain → DTO → JSON and back. Preserves insertion order of rows/columns.
 */
object IetJsonStorage {
    /** Serialize the user-input state of the table to a JSON string. */
    fun toJson(table: ImpactEstimationTable): String {
        TODO("Implement JSON serialization for ImpactEstimationTable")
    }

    /** Deserialize the user-input state from a JSON string into a new table. */
    fun fromJson(jsonText: String): ImpactEstimationTable {
        TODO("Implement JSON deserialization for ImpactEstimationTable")
    }
}
