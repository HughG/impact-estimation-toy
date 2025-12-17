package org.tameter.iet.policy

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Requirements refs: IET/Model (namespaces), IET/Model/Storage (namespaces),
 * and Stage 0 plan items.
 */
class NumberPolicyTest {
    @Test
    fun numberPolicy_defaults_as_expected() {
        // Default decimal separator is Dot, and 2 fractional digits for percentages
        assertEquals(NumberPolicy.DecimalSeparator.Dot, NumberPolicy.decimalSeparator)
        assertEquals(2, NumberPolicy.percentageFractionDigits)
    }
}