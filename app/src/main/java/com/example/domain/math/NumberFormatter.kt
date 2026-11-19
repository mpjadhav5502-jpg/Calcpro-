package com.example.domain.math

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object NumberFormatter {

    fun format(
        value: BigDecimal,
        decimalPrecision: Int = 6,
        useThousandsSeparator: Boolean = true
    ): String {
        val clean = value.stripTrailingZerosSafe()
        if (clean.compareTo(BigDecimal.ZERO) == 0) return "0"

        val absVal = clean.abs()
        val maxStandard = BigDecimal("1000000000000") // 1e12
        val minStandard = BigDecimal("0.00000001")    // 1e-8

        if (absVal >= maxStandard || (absVal < minStandard && absVal > BigDecimal.ZERO)) {
            val sciSymbols = DecimalFormatSymbols(Locale.US)
            val pattern = buildString {
                append("0.")
                repeat(decimalPrecision.coerceIn(1, 10)) { append("#") }
                append("E0")
            }
            return DecimalFormat(pattern, sciSymbols).format(clean)
        }

        val rounded = clean.setScale(decimalPrecision.coerceIn(0, 10), RoundingMode.HALF_UP)
            .stripTrailingZerosSafe()

        val symbols = DecimalFormatSymbols(Locale.US)
        val df = DecimalFormat().apply {
            decimalFormatSymbols = symbols
            isGroupingUsed = useThousandsSeparator
            groupingSize = 3
            minimumFractionDigits = 0
            maximumFractionDigits = decimalPrecision.coerceIn(0, 10)
            roundingMode = RoundingMode.HALF_UP
        }
        return df.format(rounded)
    }

    fun formatPlain(value: BigDecimal, decimalPrecision: Int = 6): String {
        return format(value, decimalPrecision, useThousandsSeparator = false)
    }
}
