package org.tameter.iet.model

import org.tameter.iet.policy.ValidationError

/**
 * Result of computing a cell's impact percentage.
 */
sealed class CellImpact {
    data class Valid(val percent: Double, val confidencePlusMinus: Double?) : CellImpact()
    data class Invalid(val errors: List<ValidationError>) : CellImpact()
}

/**
 * Compute the impact percentage of an estimation against a requirement.
 *
 * percent = (estimated - current) / (goal - current) * 100
 * - For Performance: goal must differ from current; if equal, invalid.
 * - For Cost: values must be >= 0 (current, goal, estimate); goal may equal current (then impact is 0%).
 * - Confidence range is mapped to percent via (confidence / |goal - current|) * 100 when denominator != 0.
 */
fun computeImpact(req: QualityRequirement, est: Estimation): CellImpact {
    val errors = mutableListOf<ValidationError>()

    if (req.type == RequirementType.Cost) {
        if (req.current < 0) errors += ValidationError("Cost current must be >= 0")
        if (req.goal < 0) errors += ValidationError("Cost goal must be >= 0")
        if (est.estimatedValue < 0) errors += ValidationError("Cost estimated value must be >= 0")
    }

    val delta = req.goal - req.current
    if (req.type == RequirementType.Performance && delta == 0.0) {
        errors += ValidationError("Performance requirement goal must differ from current")
    }

    val conf = est.confidenceRange
    if (conf != null && conf < 0) {
        errors += ValidationError("Confidence range must be >= 0")
    }

    if (errors.isNotEmpty()) return CellImpact.Invalid(errors)

    // If delta is zero but not forbidden (e.g., Cost), treat impact as 0 when estimate == current; else +/-Inf? We choose 0.
    if (delta == 0.0) {
        val plusMinus = if (conf != null && conf > 0) null else null
        return CellImpact.Valid(percent = 0.0, confidencePlusMinus = plusMinus)
    }

    val percent = (est.estimatedValue - req.current) / delta * 100.0
    val confPct = conf?.let { (it / kotlin.math.abs(delta)) * 100.0 }
    return CellImpact.Valid(percent = percent, confidencePlusMinus = confPct)
}
