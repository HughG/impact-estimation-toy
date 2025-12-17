package org.tameter.iet.policy

/**
 * Error-handling strategy (Stage 0 decision):
 * - ValidationError: user-editable input issues; surface to UI, non-fatal; computations should skip invalid inputs.
 * - InvariantViolation: programmer/logic errors; should be prevented by model; treated as failures in tests.
 * - IoError: storage or external environment problems; surface to UI, allow retry.
 */
sealed interface DomainError {
    val message: String
}

data class ValidationError(override val message: String) : DomainError

data class InvariantViolation(override val message: String) : DomainError

data class IoError(override val message: String) : DomainError
