package org.tameter.iet.policy

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Number/locale policy (Stage 0 decision):
 * - Parsing/formatting policy will be centralised here.
 * - CommonMain avoids platform Locale; represent decimal separator explicitly.
 * - Default: Dot decimal separator, up to 2 fractional digits for percentages.
 */
object NumberPolicy {
    enum class DecimalSeparator { Dot, Comma }

    // Defaults may be overridden via settings later.
    var decimalSeparator: DecimalSeparator = DecimalSeparator.Dot
    var percentageFractionDigits: Int = 2

    fun formatPercentage(value: Double): String {
        // Simple manual formatting for KMP commonMain
        val factor = 10.0.pow(percentageFractionDigits)
        val rounded = (value * factor).roundToLong() / factor
        return "$rounded%"
    }
}
