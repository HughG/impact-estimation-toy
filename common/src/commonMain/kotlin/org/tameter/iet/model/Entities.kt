package org.tameter.iet.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.tameter.iet.policy.ValidationError

/**
 * Stage 1: Core entities used by the IET model.
 */
@Serializable
sealed class Requirement {
    abstract val id: String
    abstract val name: String
    abstract val unit: String
    @Transient
    abstract val type: RequirementType

    /**
     * Compute the impact percentage of an estimation against this requirement.
     */
    abstract fun computeImpact(est: Estimation): CellImpact
    protected fun getConfidenceRange(
        est: Estimation,
        errors: MutableList<ValidationError>
    ): Double? {
        val conf = est.confidenceRange
        if (conf != null && conf < 0) {
            errors += ValidationError("Confidence range must be >= 0")
        }
        return conf
    }
}

@Serializable
@SerialName("performance")
data class PerformanceRequirement(
    override val id: String,
    override val name: String,
    override val unit: String,
    val current: Double,
    val goal: Double,
) : Requirement() {
    @Transient
    override val type: RequirementType = RequirementType.Performance

    /**
     * Compute the impact percentage of an estimation against a requirement.
     *
     * Performance:
     *   percentage = (estimated - current) / (goal - current) * 100
     *   - goal must differ from current; if equal, invalid.
     *   - confidence% = (confidence / |goal - current|) * 100
     */
    override fun computeImpact(est: Estimation): CellImpact {
        val errors = mutableListOf<ValidationError>()

        val conf = getConfidenceRange(est, errors)

        val requiredDelta = goal - current
        if (requiredDelta == 0.0) {
            errors += ValidationError("Performance requirement goal must differ from current")
        }

        if (errors.isNotEmpty()) return CellImpact.Invalid(errors)

        val estimatedDelta = est.estimatedValue - current
        val percent = estimatedDelta / requiredDelta * 100.0
        val confPct = conf?.let { (it / kotlin.math.abs(requiredDelta)) * 100.0 }
        return CellImpact.Valid(percent = percent, confidencePlusMinus = confPct)
    }
}

@Serializable
@SerialName("resource")
data class ResourceRequirement(
    override val id: String,
    override val name: String,
    override val unit: String,
    val budget: Double,
) : Requirement() {
    @Transient
    override val type: RequirementType = RequirementType.Resource

    /**
     * Compute the impact percentage of an estimation against a requirement.
     *
     * Resource (minimize):
     *   percentage = (budget - estimated) / budget * 100
     *   - budget must be > 0; estimated must be >= 0.
     *   - confidence% = (confidence / budget) * 100
     */
    override fun computeImpact(est: Estimation): CellImpact {
        val errors = mutableListOf<ValidationError>()

        val conf = getConfidenceRange(est, errors)


        if (budget <= 0.0) {
            errors += ValidationError("Resource budget must be > 0")
        }
        if (est.estimatedValue < 0.0) {
            errors += ValidationError("Resource estimated value must be >= 0")
        }

        if (errors.isNotEmpty()) return CellImpact.Invalid(errors)

        val percent = (budget - est.estimatedValue) / budget * 100.0
        val confPct = conf?.let { (it / budget) * 100.0 }
        return CellImpact.Valid(percent = percent, confidencePlusMinus = confPct)
    }
}

@Serializable
data class DesignIdea(
    val id: String,
    val name: String,
    val description: String? = null,
)

/**
 * Estimation input for a table cell. Confidence range is the symmetric absolute delta
 * on the same scale as the requirement values.
 */
@Serializable
data class Estimation(
    val estimatedValue: Double,
    val confidenceRange: Double? = null,
)
