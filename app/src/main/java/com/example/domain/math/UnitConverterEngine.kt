package com.example.domain.math

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

enum class UnitCategory(val title: String) {
    LENGTH("Length"),
    WEIGHT("Weight"),
    TEMPERATURE("Temperature"),
    AREA("Area"),
    VOLUME("Volume"),
    TIME("Time"),
    DATA("Data"),
    SPEED("Speed")
}

data class ConversionUnit(
    val id: String,
    val name: String,
    val symbol: String,
    val category: UnitCategory,
    // Multiplier to convert 1 unit -> base unit (not used directly for Temperature)
    val toBaseFactor: BigDecimal
)

object UnitConverterEngine {

    private val MC = MathContext(34, RoundingMode.HALF_UP)

    val unitsByCategory: Map<UnitCategory, List<ConversionUnit>> = mapOf(
        UnitCategory.LENGTH to listOf(
            ConversionUnit("mm", "Millimeter", "mm", UnitCategory.LENGTH, BigDecimal("0.001")),
            ConversionUnit("cm", "Centimeter", "cm", UnitCategory.LENGTH, BigDecimal("0.01")),
            ConversionUnit("m", "Meter", "m", UnitCategory.LENGTH, BigDecimal.ONE),
            ConversionUnit("km", "Kilometer", "km", UnitCategory.LENGTH, BigDecimal("1000")),
            ConversionUnit("in", "Inch", "in", UnitCategory.LENGTH, BigDecimal("0.0254")),
            ConversionUnit("ft", "Foot", "ft", UnitCategory.LENGTH, BigDecimal("0.3048")),
            ConversionUnit("yd", "Yard", "yd", UnitCategory.LENGTH, BigDecimal("0.9144")),
            ConversionUnit("mi", "Mile", "mi", UnitCategory.LENGTH, BigDecimal("1609.344"))
        ),
        UnitCategory.WEIGHT to listOf(
            ConversionUnit("mg", "Milligram", "mg", UnitCategory.WEIGHT, BigDecimal("0.000001")),
            ConversionUnit("g", "Gram", "g", UnitCategory.WEIGHT, BigDecimal("0.001")),
            ConversionUnit("kg", "Kilogram", "kg", UnitCategory.WEIGHT, BigDecimal.ONE),
            ConversionUnit("t", "Tonne", "t", UnitCategory.WEIGHT, BigDecimal("1000")),
            ConversionUnit("oz", "Ounce", "oz", UnitCategory.WEIGHT, BigDecimal("0.028349523125")),
            ConversionUnit("lb", "Pound", "lb", UnitCategory.WEIGHT, BigDecimal("0.45359237"))
        ),
        UnitCategory.TEMPERATURE to listOf(
            ConversionUnit("c", "Celsius", "°C", UnitCategory.TEMPERATURE, BigDecimal.ONE),
            ConversionUnit("f", "Fahrenheit", "°F", UnitCategory.TEMPERATURE, BigDecimal.ONE),
            ConversionUnit("k", "Kelvin", "K", UnitCategory.TEMPERATURE, BigDecimal.ONE)
        ),
        UnitCategory.AREA to listOf(
            ConversionUnit("sqm", "Square Meter", "m²", UnitCategory.AREA, BigDecimal.ONE),
            ConversionUnit("sqkm", "Square Kilometer", "km²", UnitCategory.AREA, BigDecimal("1000000")),
            ConversionUnit("sqft", "Square Foot", "ft²", UnitCategory.AREA, BigDecimal("0.09290304")),
            ConversionUnit("acre", "Acre", "ac", UnitCategory.AREA, BigDecimal("4046.8564224")),
            ConversionUnit("ha", "Hectare", "ha", UnitCategory.AREA, BigDecimal("10000"))
        ),
        UnitCategory.VOLUME to listOf(
            ConversionUnit("ml", "Milliliter", "mL", UnitCategory.VOLUME, BigDecimal("0.001")),
            ConversionUnit("l", "Liter", "L", UnitCategory.VOLUME, BigDecimal.ONE),
            ConversionUnit("cbm", "Cubic Meter", "m³", UnitCategory.VOLUME, BigDecimal("1000")),
            ConversionUnit("gal", "Gallon (US)", "gal", UnitCategory.VOLUME, BigDecimal("3.785411784"))
        ),
        UnitCategory.TIME to listOf(
            ConversionUnit("s", "Second", "s", UnitCategory.TIME, BigDecimal.ONE),
            ConversionUnit("min", "Minute", "min", UnitCategory.TIME, BigDecimal("60")),
            ConversionUnit("hr", "Hour", "hr", UnitCategory.TIME, BigDecimal("3600")),
            ConversionUnit("day", "Day", "d", UnitCategory.TIME, BigDecimal("86400")),
            ConversionUnit("wk", "Week", "wk", UnitCategory.TIME, BigDecimal("604800")),
            ConversionUnit("mo", "Month (30d)", "mo", UnitCategory.TIME, BigDecimal("2592000")),
            ConversionUnit("yr", "Year (365d)", "yr", UnitCategory.TIME, BigDecimal("31536000"))
        ),
        UnitCategory.DATA to listOf(
            ConversionUnit("b", "Byte", "B", UnitCategory.DATA, BigDecimal.ONE),
            ConversionUnit("kb", "Kilobyte", "KB", UnitCategory.DATA, BigDecimal("1024")),
            ConversionUnit("mb", "Megabyte", "MB", UnitCategory.DATA, BigDecimal("1048576")),
            ConversionUnit("gb", "Gigabyte", "GB", UnitCategory.DATA, BigDecimal("1073741824")),
            ConversionUnit("tb", "Terabyte", "TB", UnitCategory.DATA, BigDecimal("1099511627776"))
        ),
        UnitCategory.SPEED to listOf(
            ConversionUnit("mps", "Meter/Second", "m/s", UnitCategory.SPEED, BigDecimal.ONE),
            ConversionUnit("kmh", "Kilometer/Hour", "km/h", UnitCategory.SPEED, BigDecimal("0.2777777777777778")),
            ConversionUnit("mph", "Miles/Hour", "mph", UnitCategory.SPEED, BigDecimal("0.44704"))
        )
    )

    fun convert(value: BigDecimal, from: ConversionUnit, to: ConversionUnit): BigDecimal {
        if (from.id == to.id) return value.stripTrailingZerosSafe()
        if (from.category != to.category) {
            throw IllegalArgumentException("Cannot convert ${from.category} to ${to.category}")
        }
        if (from.category == UnitCategory.TEMPERATURE) {
            return convertTemperature(value, from.id, to.id).stripTrailingZerosSafe()
        }
        val baseValue = value.multiply(from.toBaseFactor, MC)
        return baseValue.divide(to.toBaseFactor, MC).stripTrailingZerosSafe()
    }

    private fun convertTemperature(value: BigDecimal, fromId: String, toId: String): BigDecimal {
        val offset = BigDecimal("273.15")
        val nineFifths = BigDecimal("1.8")
        val thirtyTwo = BigDecimal("32")
        val celsius = when (fromId) {
            "c" -> value
            "f" -> value.subtract(thirtyTwo, MC).divide(nineFifths, MC)
            "k" -> value.subtract(offset, MC)
            else -> value
        }
        return when (toId) {
            "c" -> celsius
            "f" -> celsius.multiply(nineFifths, MC).add(thirtyTwo, MC)
            "k" -> celsius.add(offset, MC)
            else -> celsius
        }
    }
}
