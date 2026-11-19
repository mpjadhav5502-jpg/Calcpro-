package com.example

import com.example.domain.math.AngleMode
import com.example.domain.math.EvalResult
import com.example.domain.math.FinancialCalculators
import com.example.domain.math.MathEvalError
import com.example.domain.math.MathEvaluator
import com.example.domain.math.NumberFormatter
import com.example.domain.math.PercentageMode
import com.example.domain.math.UnitCategory
import com.example.domain.math.UnitConverterEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CalcProMathUnitTest {

    private fun evalPlain(expr: String, mode: AngleMode = AngleMode.DEG): String {
        val res = MathEvaluator.evaluate(expr, mode)
        assertTrue("Expected success for '$expr' but got $res", res is EvalResult.Success)
        return NumberFormatter.formatPlain((res as EvalResult.Success).value, decimalPrecision = 8)
    }

    @Test
    fun verifyRequiredCalculations() {
        assertEquals("50", evalPlain("25 + 25"))
        assertEquals("65", evalPlain("100 - 35"))
        assertEquals("144", evalPlain("12 × 12"))
        assertEquals("25", evalPlain("100 ÷ 4"))
        assertEquals("250", evalPlain("200 + 10 × 5"))
        assertEquals("100", evalPlain("50% × 200"))
        assertEquals("12", evalPlain("sqrt(144)"))
        assertEquals("25", evalPlain("5^2"))
        assertEquals("25", evalPlain("5²"))
        assertEquals("0.5", evalPlain("sin(30)", AngleMode.DEG))
        assertEquals("2", evalPlain("log(100)"))
    }

    @Test
    fun verifyScientificFunctionsAndModes() {
        assertEquals("0.5", evalPlain("cos(60)", AngleMode.DEG))
        assertEquals("1", evalPlain("tan(45)", AngleMode.DEG))
        assertEquals("30", evalPlain("asin(0.5)", AngleMode.DEG))
        assertEquals("60", evalPlain("acos(0.5)", AngleMode.DEG))
        assertEquals("45", evalPlain("atan(1)", AngleMode.DEG))
        assertEquals("1", evalPlain("ln(e)"))
        assertEquals("3", evalPlain("cbrt(27)"))
        assertEquals("125", evalPlain("5³"))
        assertEquals("120", evalPlain("5!"))
        assertEquals("1", evalPlain("10 mod 3"))
        assertEquals("1", evalPlain("sin(pi/2)", AngleMode.RAD))
    }

    @Test
    fun verifyNegativeDecimalsAndLargeNumbers() {
        assertEquals("-2", evalPlain("-5 + 3"))
        assertEquals("25", evalPlain("(-5)^2"))
        assertEquals("0.3", evalPlain("0.1 + 0.2"))
        assertEquals("100000000000", evalPlain("100000 × 1000000"))
    }

    @Test
    fun verifyDivisionByZeroAndInvalidExpressions() {
        val divZero = MathEvaluator.evaluate("10 ÷ 0")
        assertTrue(divZero is EvalResult.Failure && divZero.error is MathEvalError.DivisionByZero)

        val syntaxErr = MathEvaluator.evaluate("25 + × 5")
        assertTrue(syntaxErr is EvalResult.Failure && syntaxErr.error is MathEvalError.SyntaxError)

        val domainErr = MathEvaluator.evaluate("sqrt(-9)")
        assertTrue(domainErr is EvalResult.Failure && domainErr.error is MathEvalError.DomainError)

        val tan90 = MathEvaluator.evaluate("tan(90)", AngleMode.DEG)
        assertTrue(tan90 is EvalResult.Failure && tan90.error is MathEvalError.DomainError)
    }

    @Test
    fun verifyPercentageGstAndDiscountCalculators() {
        val pct = FinancialCalculators.calculatePercentage(
            PercentageMode.PERCENT_OF_NUMBER,
            BigDecimal("50"),
            BigDecimal("200")
        )!!
        assertEquals(0, pct.finalAnswer.compareTo(BigDecimal("100")))

        val inc = FinancialCalculators.calculatePercentage(
            PercentageMode.PERCENT_INCREASE,
            BigDecimal("200"),
            BigDecimal("15")
        )!!
        assertEquals(0, inc.finalAnswer.compareTo(BigDecimal("230")))

        val dec = FinancialCalculators.calculatePercentage(
            PercentageMode.PERCENT_DECREASE,
            BigDecimal("200"),
            BigDecimal("25")
        )!!
        assertEquals(0, dec.finalAnswer.compareTo(BigDecimal("150")))

        val addGst = FinancialCalculators.calculateGst(BigDecimal("1000"), BigDecimal("18"), isInclusive = false)
        assertEquals(0, addGst.totalGst.compareTo(BigDecimal("180")))
        assertEquals(0, addGst.totalAmount.compareTo(BigDecimal("1180")))

        val remGst = FinancialCalculators.calculateGst(BigDecimal("1180"), BigDecimal("18"), isInclusive = true)
        assertEquals(0, remGst.baseAmount.compareTo(BigDecimal("1000")))
        assertEquals(0, remGst.totalGst.compareTo(BigDecimal("180")))

        val disc = FinancialCalculators.calculateDiscount(BigDecimal("1000"), BigDecimal("20"), BigDecimal("10"))
        assertEquals(0, disc.savingsAmount.compareTo(BigDecimal("200")))
        assertEquals(0, disc.finalPrice.compareTo(BigDecimal("880")))
    }

    @Test
    fun verifyUnitConversionsAcrossCategories() {
        val lenUnits = UnitConverterEngine.unitsByCategory[UnitCategory.LENGTH]!!
        val meter = lenUnits.first { it.id == "m" }
        val km = lenUnits.first { it.id == "km" }
        assertEquals("1", NumberFormatter.formatPlain(UnitConverterEngine.convert(BigDecimal("1000"), meter, km)))

        val tempUnits = UnitConverterEngine.unitsByCategory[UnitCategory.TEMPERATURE]!!
        val c = tempUnits.first { it.id == "c" }
        val f = tempUnits.first { it.id == "f" }
        val k = tempUnits.first { it.id == "k" }
        assertEquals("212", NumberFormatter.formatPlain(UnitConverterEngine.convert(BigDecimal("100"), c, f)))
        assertEquals("273.15", NumberFormatter.formatPlain(UnitConverterEngine.convert(BigDecimal.ZERO, c, k)))

        val dataUnits = UnitConverterEngine.unitsByCategory[UnitCategory.DATA]!!
        val mb = dataUnits.first { it.id == "mb" }
        val gb = dataUnits.first { it.id == "gb" }
        assertEquals("1", NumberFormatter.formatPlain(UnitConverterEngine.convert(BigDecimal("1024"), mb, gb)))

        val speedUnits = UnitConverterEngine.unitsByCategory[UnitCategory.SPEED]!!
        val mps = speedUnits.first { it.id == "mps" }
        val kmh = speedUnits.first { it.id == "kmh" }
        assertEquals("36", NumberFormatter.formatPlain(UnitConverterEngine.convert(BigDecimal("10"), mps, kmh), 2))
    }
}
