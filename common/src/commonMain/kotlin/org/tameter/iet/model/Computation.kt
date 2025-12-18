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
 * Performance:
 *   percent = (estimated - current) / (goal - current) * 100
 *   - goal must differ from current; if equal, invalid.
 *   - confidence% = (confidence / |goal - current|) * 100
 *
 * Resource (minimize):
 *   percent = (budget - estimated) / budget * 100
 *   - budget must be > 0; estimated must be >= 0.
 *   - confidence% = (confidence / budget) * 100
 */
fun computeImpact(req: Requirement, est: Estimation): CellImpact {
    val errors = mutableListOf<ValidationError>()

    val conf = est.confidenceRange
    if (conf != null && conf < 0) {
        errors += ValidationError("Confidence range must be >= 0")
    }

    when (req.type) {
        RequirementType.Performance -> {
            val current = req.current
            val goal = req.goal
            if (current == null || goal == null) {
                errors += ValidationError("Performance requirement must have current and goal")
                return CellImpact.Invalid(errors)
            }
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

        RequirementType.Resource -> {
            val budget = req.budget
            if (budget == null) {
                errors += ValidationError("Resource requirement must have a budget")
                return CellImpact.Invalid(errors)
            }
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
}
