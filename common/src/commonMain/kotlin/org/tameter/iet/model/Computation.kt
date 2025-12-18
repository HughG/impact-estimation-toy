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
fun computeImpact(req: Requirement, est: Estimation): CellImpact = req.computeImpact(est)
