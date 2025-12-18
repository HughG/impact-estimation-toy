package org.tameter.iet.model

/**
 * Stage 1: Core entities used by the IET model.
 */
data class Requirement(
    val id: String,
    val unit: String,
    val type: RequirementType,
    // For Performance requirements
    val current: Double? = null,
    val goal: Double? = null,
    // For Resource requirements
    val budget: Double? = null,
)

data class DesignIdea(
    val id: String,
    val name: String,
    val description: String? = null,
)

/**
 * Estimation input for a table cell. Confidence range is symmetric absolute delta
 * on the same scale as the requirement values.
 */
data class Estimation(
    val estimatedValue: Double,
    val confidenceRange: Double? = null,
)
