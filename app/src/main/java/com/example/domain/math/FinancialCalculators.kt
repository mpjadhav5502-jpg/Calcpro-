package com.example.domain.math

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

enum class PercentageMode {
    PERCENT_OF_NUMBER,
    PERCENT_INCREASE,
    PERCENT_DECREASE,
    PERCENT_DIFFERENCE
}

data class CalculationBreakdown(
    val finalAnswer: BigDecimal,
    val summaryExpression: String,
    val steps: List<String>
)

data class GstBreakdown(
    val baseAmount: BigDecimal,
    val gstRate: BigDecimal,
    val totalGst: BigDecimal,
    val cgst: BigDecimal,
    val sgst: BigDecimal,
    val totalAmount: BigDecimal,
    val isInclusive: Boolean,
    val steps: List<String>
)

data class DiscountBreakdown(
    val originalPrice: BigDecimal,
    val discountPercent: BigDecimal,
    val extraTaxPercent: BigDecimal,
    val savingsAmount: BigDecimal,
    val priceAfterDiscount: BigDecimal,
    val taxAmount: BigDecimal,
    val finalPrice: BigDecimal,
    val steps: List<String>
)

object FinancialCalculators {

    private val MC = MathContext(34, RoundingMode.HALF_UP)
    private val HUNDRED = BigDecimal(100)
    private val TWO = BigDecimal(2)

    fun calculatePercentage(
        mode: PercentageMode,
        firstInput: BigDecimal,
        secondInput: BigDecimal,
        precision: Int = 6
    ): CalculationBreakdown? {
        return when (mode) {
            PercentageMode.PERCENT_OF_NUMBER -> {
                // firstInput% of secondInput
                val result = firstInput.divide(HUNDRED, MC).multiply(secondInput, MC).stripTrailingZerosSafe()
                val fP = NumberFormatter.formatPlain(firstInput, precision)
                val fN = NumberFormatter.formatPlain(secondInput, precision)
                val fR = NumberFormatter.formatPlain(result, precision)
                CalculationBreakdown(
                    finalAnswer = result,
                    summaryExpression = "$fP% of $fN",
                    steps = listOf(
                        "Formula: (Percentage ÷ 100) × Number",
                        "Step 1: $fP ÷ 100 = ${NumberFormatter.formatPlain(firstInput.divide(HUNDRED, MC), precision)}",
                        "Step 2: × $fN = $fR"
                    )
                )
            }
            PercentageMode.PERCENT_INCREASE -> {
                // Increase firstInput by secondInput%
                val increaseAmt = firstInput.multiply(secondInput, MC).divide(HUNDRED, MC).stripTrailingZerosSafe()
                val result = firstInput.add(increaseAmt, MC).stripTrailingZerosSafe()
                val fVal = NumberFormatter.formatPlain(firstInput, precision)
                val fPct = NumberFormatter.formatPlain(secondInput, precision)
                val fInc = NumberFormatter.formatPlain(increaseAmt, precision)
                val fRes = NumberFormatter.formatPlain(result, precision)
                CalculationBreakdown(
                    finalAnswer = result,
                    summaryExpression = "$fVal + $fPct%",
                    steps = listOf(
                        "Formula: Original + (Original × Rate ÷ 100)",
                        "Step 1 (Increase): $fVal × $fPct% = $fInc",
                        "Step 2 (New Value): $fVal + $fInc = $fRes"
                    )
                )
            }
            PercentageMode.PERCENT_DECREASE -> {
                // Decrease firstInput by secondInput%
                val decreaseAmt = firstInput.multiply(secondInput, MC).divide(HUNDRED, MC).stripTrailingZerosSafe()
                val result = firstInput.subtract(decreaseAmt, MC).stripTrailingZerosSafe()
                val fVal = NumberFormatter.formatPlain(firstInput, precision)
                val fPct = NumberFormatter.formatPlain(secondInput, precision)
                val fDec = NumberFormatter.formatPlain(decreaseAmt, precision)
                val fRes = NumberFormatter.formatPlain(result, precision)
                CalculationBreakdown(
                    finalAnswer = result,
                    summaryExpression = "$fVal − $fPct%",
                    steps = listOf(
                        "Formula: Original − (Original × Rate ÷ 100)",
                        "Step 1 (Decrease): $fVal × $fPct% = $fDec",
                        "Step 2 (New Value): $fVal − $fDec = $fRes"
                    )
                )
            }
            PercentageMode.PERCENT_DIFFERENCE -> {
                val avg = firstInput.add(secondInput, MC).divide(TWO, MC)
                if (avg.compareTo(BigDecimal.ZERO) == 0) return null
                val diff = firstInput.subtract(secondInput, MC).abs()
                val result = diff.divide(avg, MC).multiply(HUNDRED, MC).stripTrailingZerosSafe()
                val fA = NumberFormatter.formatPlain(firstInput, precision)
                val fB = NumberFormatter.formatPlain(secondInput, precision)
                val fDiff = NumberFormatter.formatPlain(diff, precision)
                val fAvg = NumberFormatter.formatPlain(avg, precision)
                val fRes = NumberFormatter.formatPlain(result, precision)
                CalculationBreakdown(
                    finalAnswer = result,
                    summaryExpression = "% diff($fA, $fB)",
                    steps = listOf(
                        "Formula: |A − B| ÷ ((A + B) ÷ 2) × 100",
                        "Step 1 (Difference): |$fA − $fB| = $fDiff",
                        "Step 2 (Average): ($fA + $fB) ÷ 2 = $fAvg",
                        "Step 3 (% Diff): ($fDiff ÷ $fAvg) × 100 = $fRes%"
                    )
                )
            }
        }
    }

    fun calculateGst(
        amount: BigDecimal,
        gstRatePercent: BigDecimal,
        isInclusive: Boolean,
        precision: Int = 2
    ): GstBreakdown {
        val fAmt = NumberFormatter.formatPlain(amount, precision)
        val fRate = NumberFormatter.formatPlain(gstRatePercent, precision)
        return if (!isInclusive) {
            // Add GST: Total = Base + Base * rate/100
            val gstAmount = amount.multiply(gstRatePercent, MC).divide(HUNDRED, MC).stripTrailingZerosSafe()
            val total = amount.add(gstAmount, MC).stripTrailingZerosSafe()
            val halfGst = gstAmount.divide(TWO, MC).stripTrailingZerosSafe()
            val fGst = NumberFormatter.formatPlain(gstAmount, precision)
            val fHalf = NumberFormatter.formatPlain(halfGst, precision)
            val fTotal = NumberFormatter.formatPlain(total, precision)
            GstBreakdown(
                baseAmount = amount,
                gstRate = gstRatePercent,
                totalGst = gstAmount,
                cgst = halfGst,
                sgst = halfGst,
                totalAmount = total,
                isInclusive = false,
                steps = listOf(
                    "Mode: Exclusive (Add $fRate% GST)",
                    "Step 1 (GST): $fAmt × $fRate% = $fGst",
                    "Step 2 (CGST + SGST): $fHalf + $fHalf",
                    "Step 3 (Total): $fAmt + $fGst = $fTotal"
                )
            )
        } else {
            // Remove GST: Base = Total / (1 + rate/100)
            val divisor = BigDecimal.ONE.add(gstRatePercent.divide(HUNDRED, MC), MC)
            val base = amount.divide(divisor, MC).stripTrailingZerosSafe()
            val gstAmount = amount.subtract(base, MC).stripTrailingZerosSafe()
            val halfGst = gstAmount.divide(TWO, MC).stripTrailingZerosSafe()
            val fBase = NumberFormatter.formatPlain(base, precision)
            val fGst = NumberFormatter.formatPlain(gstAmount, precision)
            val fHalf = NumberFormatter.formatPlain(halfGst, precision)
            GstBreakdown(
                baseAmount = base,
                gstRate = gstRatePercent,
                totalGst = gstAmount,
                cgst = halfGst,
                sgst = halfGst,
                totalAmount = amount,
                isInclusive = true,
                steps = listOf(
                    "Mode: Inclusive (Remove $fRate% GST)",
                    "Step 1 (Base): $fAmt ÷ (1 + $fRate/100) = $fBase",
                    "Step 2 (GST Included): $fAmt − $fBase = $fGst",
                    "Step 3 (CGST + SGST): $fHalf + $fHalf"
                )
            )
        }
    }

    fun calculateDiscount(
        originalPrice: BigDecimal,
        discountPercent: BigDecimal,
        extraTaxPercent: BigDecimal = BigDecimal.ZERO,
        precision: Int = 2
    ): DiscountBreakdown {
        val savings = originalPrice.multiply(discountPercent, MC).divide(HUNDRED, MC).stripTrailingZerosSafe()
        val afterDiscount = originalPrice.subtract(savings, MC).stripTrailingZerosSafe()
        val tax = afterDiscount.multiply(extraTaxPercent, MC).divide(HUNDRED, MC).stripTrailingZerosSafe()
        val finalPrice = afterDiscount.add(tax, MC).stripTrailingZerosSafe()

        val fOrig = NumberFormatter.formatPlain(originalPrice, precision)
        val fDisc = NumberFormatter.formatPlain(discountPercent, precision)
        val fSave = NumberFormatter.formatPlain(savings, precision)
        val fAfter = NumberFormatter.formatPlain(afterDiscount, precision)
        val fTaxPct = NumberFormatter.formatPlain(extraTaxPercent, precision)
        val fTax = NumberFormatter.formatPlain(tax, precision)
        val fFinal = NumberFormatter.formatPlain(finalPrice, precision)

        val steps = mutableListOf(
            "Step 1 (Savings): $fOrig × $fDisc% = $fSave",
            "Step 2 (Discounted Price): $fOrig − $fSave = $fAfter"
        )
        if (extraTaxPercent > BigDecimal.ZERO) {
            steps.add("Step 3 (Tax $fTaxPct%): $fAfter × $fTaxPct% = $fTax")
            steps.add("Step 4 (Final Price): $fAfter + $fTax = $fFinal")
        }

        return DiscountBreakdown(
            originalPrice = originalPrice,
            discountPercent = discountPercent,
            extraTaxPercent = extraTaxPercent,
            savingsAmount = savings,
            priceAfterDiscount = afterDiscount,
            taxAmount = tax,
            finalPrice = finalPrice,
            steps = steps
        )
    }
}
