package org.tameter.iet.model

import org.tameter.iet.policy.ValidationError

/**
 * Result of computing a cell's impact percentage.
 */
sealed class CellImpact {
    data class Valid(val percent: Double, val confidencePlusMinus: Double?) : CellImpact()
    data class Invalid(val errors: List<ValidationError>) : CellImpact()
}