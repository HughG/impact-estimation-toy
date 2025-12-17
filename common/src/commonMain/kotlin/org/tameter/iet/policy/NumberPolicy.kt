package org.tameter.iet.policy

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
}
