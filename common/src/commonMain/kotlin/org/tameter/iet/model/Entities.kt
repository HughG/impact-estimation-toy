package org.tameter.iet.model

/**
 * Stage 1: Core entities used by the IET model.
 */
sealed class Requirement {
    abstract val id: String
    abstract val unit: String
    abstract val type: RequirementType

    /**
     * Compute the impact percentage of an estimation against this requirement.
     */
    abstract fun computeImpact(est: Estimation): CellImpact
}

data class PerformanceRequirement(
    override val id: String,
    override val unit: String,
    val current: Double,
    val goal: Double,
) : Requirement() {
    override val type: RequirementType = RequirementType.Performance

    override fun computeImpact(est: Estimation): CellImpact {
        val errors = mutableListOf<org.tameter.iet.policy.ValidationError>()

        val conf = est.confidenceRange
        if (conf != null && conf < 0) {
            errors += org.tameter.iet.policy.ValidationError("Confidence range must be >= 0")
        }

        val requiredDelta = goal - current
        if (requiredDelta == 0.0) {
            errors += org.tameter.iet.policy.ValidationError("Performance requirement goal must differ from current")
        }

        if (errors.isNotEmpty()) return CellImpact.Invalid(errors)

        val estimatedDelta = est.estimatedValue - current
        val percent = estimatedDelta / requiredDelta * 100.0
        val confPct = conf?.let { (it / kotlin.math.abs(requiredDelta)) * 100.0 }
        return CellImpact.Valid(percent = percent, confidencePlusMinus = confPct)
    }
}

data class ResourceRequirement(
    override val id: String,
    override val unit: String,
    val budget: Double,
) : Requirement() {
    override val type: RequirementType = RequirementType.Resource

    override fun computeImpact(est: Estimation): CellImpact {
        val errors = mutableListOf<org.tameter.iet.policy.ValidationError>()

        val conf = est.confidenceRange
        if (conf != null && conf < 0) {
            errors += org.tameter.iet.policy.ValidationError("Confidence range must be >= 0")
        }

        if (budget <= 0.0) {
            errors += org.tameter.iet.policy.ValidationError("Resource budget must be > 0")
        }
        if (est.estimatedValue < 0.0) {
            errors += org.tameter.iet.policy.ValidationError("Resource estimated value must be >= 0")
        }

        if (errors.isNotEmpty()) return CellImpact.Invalid(errors)

        val percent = (budget - est.estimatedValue) / budget * 100.0
        val confPct = conf?.let { (it / budget) * 100.0 }
        return CellImpact.Valid(percent = percent, confidencePlusMinus = confPct)
    }
}

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
