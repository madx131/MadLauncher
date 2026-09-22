package com.madlauncher.action

import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToLong

enum class CalcKind { MATH, CONVERSION }

data class CalcResult(
    /** Primary answer shown large, e.g. `22.22 °C` or `4`. */
    val answer: String,
    /** Short source line, e.g. `72 °F` or `2 + 2`. */
    val expression: String,
    /** Extra lines (other units / detail). */
    val extras: List<String> = emptyList(),
    val kind: CalcKind = CalcKind.MATH,
)

/**
 * Evaluates basic math and unit conversions for the Action Box.
 * Supports temperature (°C/°F/K), weight (kg/lb/oz/g), and pressure (psi/bar).
 */
object CalcParser {
    fun tryParse(raw: String): CalcResult? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("@") || trimmed.startsWith("!") || trimmed.startsWith("&") ||
            trimmed.startsWith("?") ||
            trimmed.startsWith("+") || trimmed.startsWith("#") || trimmed.startsWith("/") ||
            trimmed.startsWith(":")
        ) {
            return null
        }
        val lower = trimmed.lowercase(Locale.US)
        if (lower == "911" || lower.startsWith("911 ")) return null

        tryConvert(trimmed)?.let { return it }
        tryMath(trimmed)?.let { return it }
        return null
    }

    private fun tryMath(raw: String): CalcResult? {
        val expr = raw.trim().removeSuffix("=").trim()
        if (!expr.any { it.isDigit() }) return null
        if (!expr.any { it in "+-*/×÷^()" }) return null
        if (expr.any { it.isLetter() }) return null
        // Lone signed number is not a calculation.
        if (expr.matches(Regex("""^[+-]?\d+(\.\d+)?$"""))) return null
        val value = runCatching { evaluate(expr) }.getOrNull() ?: return null
        if (value.isNaN() || value.isInfinite()) return null
        return CalcResult(
            answer = formatNumber(value),
            expression = expr,
            kind = CalcKind.MATH,
        )
    }

    /** `2+2=` → `"4"` so the pill can keep calculating. */
    fun continueFromEquals(raw: String): String? {
        val trimmed = raw.trim()
        if (!trimmed.endsWith("=") || trimmed == "=") return null
        val result = tryParse(trimmed.removeSuffix("=").trim()) ?: return null
        return continueValue(result)
    }

    fun continueValue(result: CalcResult): String {
        if (result.kind == CalcKind.MATH) return result.answer
        val token = result.answer.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        return token.trimEnd('°', ',', ';').ifBlank { result.answer }
    }

    private fun tryConvert(raw: String): CalcResult? {
        val s = raw.trim().lowercase(Locale.US)
            .replace("°", " ")
            .replace("→", " to ")
            .replace("->", " to ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // 32 f to c | 100celsius in fahrenheit | 30psi | 10 kg to lb
        val m = Regex(
            """^([+-]?\d+(?:\.\d+)?)\s*([a-z]+(?:\s+[a-z]+)?)(?:\s+(?:to|in|as)\s+([a-z]+(?:\s+[a-z]+)?))?$""",
        ).matchEntire(s) ?: return null

        val amount = m.groupValues[1].toDoubleOrNull() ?: return null
        val from = normalizeUnit(m.groupValues[2]) ?: return null
        val toRaw = m.groupValues[3].trim()
        val to = if (toRaw.isEmpty()) null else normalizeUnit(toRaw)

        return when (from.category) {
            UnitCategory.TEMP -> convertTemp(amount, from, to)
            UnitCategory.WEIGHT -> convertWeight(amount, from, to)
            UnitCategory.PRESSURE -> convertPressure(amount, from, to)
        }
    }

    private fun convertTemp(amount: Double, from: UnitId, to: UnitId?): CalcResult? {
        val celsius = when (from) {
            UnitId.C -> amount
            UnitId.F -> (amount - 32.0) * 5.0 / 9.0
            UnitId.K -> amount - 273.15
            else -> return null
        }
        val targets = when {
            to != null -> listOf(to)
            from == UnitId.C -> listOf(UnitId.F, UnitId.K)
            from == UnitId.F -> listOf(UnitId.C, UnitId.K)
            else -> listOf(UnitId.C, UnitId.F)
        }.filter { it.category == UnitCategory.TEMP }

        fun asUnit(u: UnitId): Double = when (u) {
            UnitId.C -> celsius
            UnitId.F -> celsius * 9.0 / 5.0 + 32.0
            UnitId.K -> celsius + 273.15
            else -> Double.NaN
        }

        val primary = targets.firstOrNull() ?: return null
        val primaryVal = asUnit(primary)
        val extras = targets.drop(1).map { "${formatNumber(asUnit(it))} ${it.label}" }
        return CalcResult(
            answer = "${formatNumber(primaryVal)} ${primary.label}",
            expression = "${formatNumber(amount)} ${from.label}",
            extras = extras,
            kind = CalcKind.CONVERSION,
        )
    }

    private fun convertWeight(amount: Double, from: UnitId, to: UnitId?): CalcResult? {
        val grams = when (from) {
            UnitId.KG -> amount * 1000.0
            UnitId.G -> amount
            UnitId.LB -> amount * 453.59237
            UnitId.OZ -> amount * 28.349523125
            else -> return null
        }
        val targets = when {
            to != null -> listOf(to)
            from == UnitId.KG || from == UnitId.G -> listOf(UnitId.LB, UnitId.OZ)
            else -> listOf(UnitId.KG, UnitId.G)
        }.filter { it.category == UnitCategory.WEIGHT }

        fun asUnit(u: UnitId): Double = when (u) {
            UnitId.KG -> grams / 1000.0
            UnitId.G -> grams
            UnitId.LB -> grams / 453.59237
            UnitId.OZ -> grams / 28.349523125
            else -> Double.NaN
        }

        val primary = targets.firstOrNull() ?: return null
        val extras = targets.drop(1).map { "${formatNumber(asUnit(it))} ${it.label}" }
        return CalcResult(
            answer = "${formatNumber(asUnit(primary))} ${primary.label}",
            expression = "${formatNumber(amount)} ${from.label}",
            extras = extras,
            kind = CalcKind.CONVERSION,
        )
    }

    private fun convertPressure(amount: Double, from: UnitId, to: UnitId?): CalcResult? {
        val bar = when (from) {
            UnitId.PSI -> amount * 0.0689475729
            UnitId.BAR -> amount
            else -> return null
        }
        val target = to ?: if (from == UnitId.PSI) UnitId.BAR else UnitId.PSI
        if (target.category != UnitCategory.PRESSURE) return null
        val value = when (target) {
            UnitId.BAR -> bar
            UnitId.PSI -> bar / 0.0689475729
            else -> return null
        }
        return CalcResult(
            answer = "${formatNumber(value)} ${target.label}",
            expression = "${formatNumber(amount)} ${from.label}",
            kind = CalcKind.CONVERSION,
        )
    }

    private enum class UnitCategory { TEMP, WEIGHT, PRESSURE }

    private enum class UnitId(val label: String, val category: UnitCategory) {
        C("°C", UnitCategory.TEMP),
        F("°F", UnitCategory.TEMP),
        K("K", UnitCategory.TEMP),
        KG("kg", UnitCategory.WEIGHT),
        G("g", UnitCategory.WEIGHT),
        LB("lb", UnitCategory.WEIGHT),
        OZ("oz", UnitCategory.WEIGHT),
        PSI("psi", UnitCategory.PRESSURE),
        BAR("bar", UnitCategory.PRESSURE),
    }

    private fun normalizeUnit(raw: String): UnitId? {
        val u = raw.trim().lowercase(Locale.US).replace(Regex("\\s+"), "")
        return when (u) {
            "c", "cel", "cels", "celsius", "centigrade" -> UnitId.C
            "f", "fah", "fahr", "fahrenheit" -> UnitId.F
            "k", "kelvin" -> UnitId.K
            "kg", "kgs", "kilo", "kilos", "kilogram", "kilograms" -> UnitId.KG
            "g", "gram", "grams" -> UnitId.G
            "lb", "lbs", "pound", "pounds" -> UnitId.LB
            "oz", "ounce", "ounces" -> UnitId.OZ
            "psi", "psia", "psig" -> UnitId.PSI
            "bar", "bars" -> UnitId.BAR
            else -> null
        }
    }

    fun formatNumber(v: Double): String {
        if (v.isNaN() || v.isInfinite()) return "—"
        val rounded = (v * 1_000_000_000.0).roundToLong() / 1_000_000_000.0
        return if (kotlin.math.abs(rounded - rounded.toLong()) < 1e-9) {
            rounded.toLong().toString()
        } else {
            String.format(Locale.US, "%.6f", rounded).trimEnd('0').trimEnd('.')
        }
    }

    private fun evaluate(input: String): Double {
        val p = ExprParser(input.replace('×', '*').replace('÷', '/').replace('−', '-'))
        val v = p.parseExpression()
        p.skipWs()
        if (!p.eof) error("bad expression")
        return v
    }

    private class ExprParser(private val s: String) {
        var i = 0
        val eof: Boolean get() = i >= s.length

        fun skipWs() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        fun peek(): Char? {
            skipWs()
            return if (i < s.length) s[i] else null
        }

        fun parseExpression(): Double {
            var v = parseTerm()
            while (true) {
                when (peek()) {
                    '+' -> { i++; v += parseTerm() }
                    '-' -> { i++; v -= parseTerm() }
                    else -> return v
                }
            }
        }

        fun parseTerm(): Double {
            var v = parsePower()
            while (true) {
                when (peek()) {
                    '*' -> { i++; v *= parsePower() }
                    '/' -> { i++; v /= parsePower() }
                    '%' -> { i++; v %= parsePower() }
                    else -> return v
                }
            }
        }

        fun parsePower(): Double {
            var v = parseFactor()
            if (peek() == '^') {
                i++
                val exp = parseFactor()
                v = v.pow(exp)
            }
            return v
        }

        fun parseFactor(): Double {
            when (peek()) {
                '+' -> { i++; return parseFactor() }
                '-' -> { i++; return -parseFactor() }
                '(' -> {
                    i++
                    val v = parseExpression()
                    if (peek() == ')') i++
                    return v
                }
                else -> return parseNumber()
            }
        }

        fun parseNumber(): Double {
            skipWs()
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            if (start == i) error("expected number")
            return s.substring(start, i).toDouble()
        }
    }
}
