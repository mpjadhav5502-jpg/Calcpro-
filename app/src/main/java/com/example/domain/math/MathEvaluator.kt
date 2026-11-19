package com.example.domain.math

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

enum class AngleMode {
    DEG,
    RAD
}

sealed class MathEvalError(message: String) : Exception(message) {
    class DivisionByZero : MathEvalError("Cannot divide by zero")
    class SyntaxError(detail: String = "Invalid expression") : MathEvalError(detail)
    class DomainError(detail: String = "Math domain error") : MathEvalError(detail)
    class Overflow : MathEvalError("Result is too large")
}

sealed class EvalResult {
    data class Success(val value: BigDecimal) : EvalResult()
    data class Failure(val error: MathEvalError) : EvalResult()
}

/**
 * Production-grade recursive-descent mathematical expression evaluator.
 * Uses BigDecimal (DECIMAL128) for exact arithmetic (+, -, *, /, integer ^, %, mod, !)
 * and IEEE-754 Double with trig epsilon snapping for transcendental functions.
 */
object MathEvaluator {

    private val MC = MathContext(34, RoundingMode.HALF_UP)
    private val TRANSCENDENTAL_MC = MathContext(12, RoundingMode.HALF_UP)

    private sealed class Token {
        data class Number(val value: BigDecimal) : Token()
        data class Constant(val name: String) : Token()
        data class Func(val name: String) : Token()
        data class Op(val symbol: String) : Token()
        object LParen : Token()
        object RParen : Token()
    }

    fun evaluate(rawExpression: String, angleMode: AngleMode = AngleMode.DEG): EvalResult {
        return try {
            val cleaned = preprocess(rawExpression)
            if (cleaned.isEmpty()) {
                return EvalResult.Failure(MathEvalError.SyntaxError("Empty expression"))
            }
            val tokens = tokenize(cleaned)
            val parser = Parser(tokens, angleMode)
            val result = parser.parseExpression()
            if (!parser.isAtEnd()) {
                return EvalResult.Failure(MathEvalError.SyntaxError("Unexpected token"))
            }
            EvalResult.Success(result.stripTrailingZerosSafe())
        } catch (e: MathEvalError) {
            EvalResult.Failure(e)
        } catch (e: ArithmeticException) {
            EvalResult.Failure(MathEvalError.Overflow())
        } catch (e: Exception) {
            EvalResult.Failure(MathEvalError.SyntaxError())
        }
    }

    private fun preprocess(input: String): String {
        return input
            .trim()
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("–", "-")
            .replace("√", "sqrt")
            .replace("∛", "cbrt")
            .replace("π", "pi")
            .replace(",", "")
            .replace(" ", "")
    }

    private fun tokenize(expr: String): List<Token> {
        val rawTokens = mutableListOf<Token>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isDigit() || c == '.' -> {
                    val start = i
                    var dotCount = 0
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        if (expr[i] == '.') dotCount++
                        if (dotCount > 1) throw MathEvalError.SyntaxError("Multiple decimal points")
                        i++
                    }
                    // Support scientific notation like 1.5E10 or 1.5e-5
                    if (i < expr.length && (expr[i] == 'E' || (expr[i] == 'e' && i + 1 < expr.length && (expr[i + 1].isDigit() || expr[i + 1] == '+' || expr[i + 1] == '-')))) {
                        i++
                        if (i < expr.length && (expr[i] == '+' || expr[i] == '-')) i++
                        if (i >= expr.length || !expr[i].isDigit()) {
                            throw MathEvalError.SyntaxError("Invalid exponent")
                        }
                        while (i < expr.length && expr[i].isDigit()) i++
                    }
                    val numStr = expr.substring(start, i)
                    if (numStr == ".") throw MathEvalError.SyntaxError("Invalid decimal")
                    val bd = try {
                        BigDecimal(numStr, MC)
                    } catch (_: NumberFormatException) {
                        throw MathEvalError.SyntaxError("Invalid number")
                    }
                    rawTokens.add(Token.Number(bd))
                }
                c.isLetter() -> {
                    val start = i
                    while (i < expr.length && expr[i].isLetter()) i++
                    val word = expr.substring(start, i).lowercase()
                    when (word) {
                        "pi", "e" -> rawTokens.add(Token.Constant(word))
                        "mod" -> rawTokens.add(Token.Op("mod"))
                        "sin", "cos", "tan", "asin", "acos", "atan",
                        "log", "ln", "sqrt", "cbrt", "abs", "sq", "cb" -> {
                            rawTokens.add(Token.Func(word))
                        }
                        else -> throw MathEvalError.SyntaxError("Unknown identifier: $word")
                    }
                }
                c == '(' -> {
                    rawTokens.add(Token.LParen)
                    i++
                }
                c == ')' -> {
                    rawTokens.add(Token.RParen)
                    i++
                }
                c == '+' || c == '-' || c == '*' || c == '/' || c == '^' || c == '%' || c == '!' -> {
                    rawTokens.add(Token.Op(c.toString()))
                    i++
                }
                c == '²' -> {
                    rawTokens.add(Token.Op("^"))
                    rawTokens.add(Token.Number(BigDecimal(2)))
                    i++
                }
                c == '³' -> {
                    rawTokens.add(Token.Op("^"))
                    rawTokens.add(Token.Number(BigDecimal(3)))
                    i++
                }
                else -> throw MathEvalError.SyntaxError("Unexpected character: $c")
            }
        }

        // Insert implicit multiplication where mathematically standard:
        // e.g., 2(3), (2)(3), 2pi, 2sin(30), pi(2), 5%(200) -> Wait: 50% * 200 or 50%200
        val result = mutableListOf<Token>()
        for (idx in rawTokens.indices) {
            val curr = rawTokens[idx]
            if (result.isNotEmpty()) {
                val prev = result.last()
                val prevIsValueEnd = prev is Token.Number ||
                    prev is Token.Constant ||
                    prev is Token.RParen ||
                    (prev is Token.Op && (prev.symbol == "%" || prev.symbol == "!"))
                val currIsValueStart = curr is Token.Number ||
                    curr is Token.Constant ||
                    curr is Token.Func ||
                    curr is Token.LParen
                if (prevIsValueEnd && currIsValueStart) {
                    result.add(Token.Op("*"))
                }
            }
            result.add(curr)
        }
        return result
    }

    private class Parser(
        private val tokens: List<Token>,
        private val angleMode: AngleMode
    ) {
        private var pos = 0

        fun isAtEnd(): Boolean = pos >= tokens.size

        private fun peek(): Token? = tokens.getOrNull(pos)

        private fun consume(): Token = tokens[pos++]

        private fun matchOp(symbol: String): Boolean {
            val t = peek()
            if (t is Token.Op && t.symbol == symbol) {
                pos++
                return true
            }
            return false
        }

        fun parseExpression(): BigDecimal = parseAddSub()

        private fun parseAddSub(): BigDecimal {
            var left = parseMulDiv()
            while (true) {
                when {
                    matchOp("+") -> left = left.add(parseMulDiv(), MC)
                    matchOp("-") -> left = left.subtract(parseMulDiv(), MC)
                    else -> break
                }
            }
            return left
        }

        private fun parseMulDiv(): BigDecimal {
            var left = parsePower()
            while (true) {
                when {
                    matchOp("*") -> left = left.multiply(parsePower(), MC)
                    matchOp("/") -> {
                        val right = parsePower()
                        if (right.compareTo(BigDecimal.ZERO) == 0) {
                            throw MathEvalError.DivisionByZero()
                        }
                        left = left.divide(right, MC)
                    }
                    matchOp("mod") -> {
                        val right = parsePower()
                        if (right.compareTo(BigDecimal.ZERO) == 0) {
                            throw MathEvalError.DivisionByZero()
                        }
                        left = left.remainder(right, MC)
                    }
                    else -> break
                }
            }
            return left
        }

        // Right-associative exponentiation, unary minus binds looser than ^ (-5^2 = -25)
        private fun parsePower(): BigDecimal {
            val base = parseUnary()
            if (matchOp("^")) {
                val exp = parsePower()
                return pow(base, exp)
            }
            return base
        }

        private fun parseUnary(): BigDecimal {
            return when {
                matchOp("+") -> parseUnary()
                matchOp("-") -> parseUnary().negate()
                else -> parsePostfix()
            }
        }

        private fun parsePostfix(): BigDecimal {
            var value = parsePrimary()
            while (true) {
                when {
                    matchOp("!") -> value = factorial(value)
                    matchOp("%") -> value = value.divide(BigDecimal(100), MC)
                    else -> break
                }
            }
            return value
        }

        private fun parsePrimary(): BigDecimal {
            val token = peek() ?: throw MathEvalError.SyntaxError("Unexpected end of expression")
            return when (token) {
                is Token.Number -> {
                    consume()
                    token.value
                }
                is Token.Constant -> {
                    consume()
                    when (token.name) {
                        "pi" -> BigDecimal(PI, TRANSCENDENTAL_MC)
                        "e" -> BigDecimal(Math.E, TRANSCENDENTAL_MC)
                        else -> throw MathEvalError.SyntaxError()
                    }
                }
                is Token.Func -> {
                    consume()
                    val arg = if (peek() is Token.LParen) {
                        consume()
                        val inner = parseExpression()
                        if (peek() !is Token.RParen) {
                            throw MathEvalError.SyntaxError("Missing closing parenthesis")
                        }
                        consume()
                        inner
                    } else {
                        parseUnary()
                    }
                    applyFunction(token.name, arg)
                }
                is Token.LParen -> {
                    consume()
                    val inner = parseExpression()
                    if (peek() !is Token.RParen) {
                        throw MathEvalError.SyntaxError("Missing closing parenthesis")
                    }
                    consume()
                    inner
                }
                else -> throw MathEvalError.SyntaxError("Unexpected token")
            }
        }

        private fun applyFunction(name: String, arg: BigDecimal): BigDecimal {
            val d = arg.toDouble()
            if (d.isInfinite() || d.isNaN()) throw MathEvalError.Overflow()
            return when (name) {
                "sin" -> {
                    val rad = if (angleMode == AngleMode.DEG) Math.toRadians(d) else d
                    cleanTranscendental(sin(rad))
                }
                "cos" -> {
                    val rad = if (angleMode == AngleMode.DEG) Math.toRadians(d) else d
                    cleanTranscendental(cos(rad))
                }
                "tan" -> {
                    if (angleMode == AngleMode.DEG) {
                        val mod180 = ((d % 180.0) + 180.0) % 180.0
                        if (abs(mod180 - 90.0) < 1e-10) {
                            throw MathEvalError.DomainError("tan(90°) is undefined")
                        }
                    } else {
                        val cosVal = cos(d)
                        if (abs(cosVal) < 1e-12) {
                            throw MathEvalError.DomainError("tan undefined")
                        }
                    }
                    val rad = if (angleMode == AngleMode.DEG) Math.toRadians(d) else d
                    cleanTranscendental(tan(rad))
                }
                "asin" -> {
                    if (d < -1.0 || d > 1.0) throw MathEvalError.DomainError("asin domain is [-1, 1]")
                    val res = asin(d)
                    cleanTranscendental(if (angleMode == AngleMode.DEG) Math.toDegrees(res) else res)
                }
                "acos" -> {
                    if (d < -1.0 || d > 1.0) throw MathEvalError.DomainError("acos domain is [-1, 1]")
                    val res = acos(d)
                    cleanTranscendental(if (angleMode == AngleMode.DEG) Math.toDegrees(res) else res)
                }
                "atan" -> {
                    val res = atan(d)
                    cleanTranscendental(if (angleMode == AngleMode.DEG) Math.toDegrees(res) else res)
                }
                "log" -> {
                    if (d <= 0.0) throw MathEvalError.DomainError("log requires positive input")
                    cleanTranscendental(log10(d))
                }
                "ln" -> {
                    if (d <= 0.0) throw MathEvalError.DomainError("ln requires positive input")
                    cleanTranscendental(ln(d))
                }
                "sqrt" -> {
                    if (arg < BigDecimal.ZERO) throw MathEvalError.DomainError("Square root of negative")
                    cleanTranscendental(sqrt(d))
                }
                "cbrt" -> cleanTranscendental(cbrt(d))
                "abs" -> arg.abs()
                "sq" -> arg.multiply(arg, MC)
                "cb" -> arg.multiply(arg, MC).multiply(arg, MC)
                else -> throw MathEvalError.SyntaxError("Unknown function $name")
            }
        }

        private fun pow(base: BigDecimal, exp: BigDecimal): BigDecimal {
            // Exact integer power for reasonable exponents
            val expStripped = exp.stripTrailingZerosSafe()
            if (expStripped.scale() <= 0) {
                try {
                    val intExp = expStripped.intValueExact()
                    if (intExp in -1000..1000) {
                        if (base.compareTo(BigDecimal.ZERO) == 0 && intExp < 0) {
                            throw MathEvalError.DivisionByZero()
                        }
                        return base.pow(intExp, MC)
                    }
                } catch (_: ArithmeticException) {
                    // Fall through to double pow
                }
            }
            val b = base.toDouble()
            val e = exp.toDouble()
            if (b < 0 && expStripped.scale() > 0) {
                throw MathEvalError.DomainError("Negative base with fractional exponent")
            }
            if (b == 0.0 && e < 0.0) {
                throw MathEvalError.DivisionByZero()
            }
            val res = b.pow(e)
            if (res.isNaN()) throw MathEvalError.DomainError()
            if (res.isInfinite()) throw MathEvalError.Overflow()
            return cleanTranscendental(res)
        }

        private fun factorial(value: BigDecimal): BigDecimal {
            val stripped = value.stripTrailingZerosSafe()
            if (stripped < BigDecimal.ZERO || stripped.scale() > 0) {
                throw MathEvalError.DomainError("Factorial requires a non-negative integer")
            }
            val n = try {
                stripped.intValueExact()
            } catch (_: ArithmeticException) {
                throw MathEvalError.Overflow()
            }
            if (n > 170) throw MathEvalError.Overflow()
            var acc = BigDecimal.ONE
            for (k in 2..n) {
                acc = acc.multiply(BigDecimal(k), MC)
            }
            return acc
        }

        private fun cleanTranscendental(value: Double): BigDecimal {
            if (value.isNaN()) throw MathEvalError.DomainError()
            if (value.isInfinite()) throw MathEvalError.Overflow()
            val snapped = if (abs(value) < 1e-12) 0.0 else value
            return BigDecimal(snapped.toString(), TRANSCENDENTAL_MC).stripTrailingZerosSafe()
        }
    }
}

fun BigDecimal.stripTrailingZerosSafe(): BigDecimal {
    return if (this.compareTo(BigDecimal.ZERO) == 0) {
        BigDecimal.ZERO
    } else {
        this.stripTrailingZeros()
    }
}
