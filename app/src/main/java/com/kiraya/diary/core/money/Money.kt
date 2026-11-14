package com.kiraya.diary.core.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Money is always stored as a [Long] in paisa (1 rupee = 100 paisa).
 * These helpers convert between paisa and what the user sees or types.
 */
object Money {
    const val PAISA_PER_RUPEE = 100L

    /** "1,250" or "1,250.50". Paisa are shown only when not zero. */
    fun format(paisa: Long, locale: Locale = Locale.getDefault(), grouping: Boolean = true): String {
        val nf = NumberFormat.getNumberInstance(locale).apply {
            val hasPaisa = paisa % PAISA_PER_RUPEE != 0L
            minimumFractionDigits = if (hasPaisa) 2 else 0
            maximumFractionDigits = 2
            isGroupingUsed = grouping
        }
        return nf.format(BigDecimal.valueOf(paisa).movePointLeft(2))
    }

    /** "Rs 1,250". */
    fun formatWithSymbol(
        paisa: Long,
        symbol: String = "Rs",
        locale: Locale = Locale.getDefault(),
        grouping: Boolean = true,
    ): String {
        val sign = if (paisa < 0) "-" else ""
        return "$sign$symbol ${format(abs(paisa), locale, grouping)}"
    }

    /** Parses user input like "1250", "1,250.5" into paisa. Returns null if not valid. */
    fun parse(input: String): Long? {
        val clean = input.trim().replace(",", "")
        if (clean.isEmpty()) return null
        return try {
            BigDecimal(clean).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
        } catch (_: NumberFormatException) {
            null
        } catch (_: ArithmeticException) {
            null
        }
    }

    /** Plain text for an input field: "1250" or "1250.5". Empty for zero. */
    fun toInput(paisa: Long): String {
        if (paisa == 0L) return ""
        return BigDecimal.valueOf(paisa).movePointLeft(2).stripTrailingZeros().toPlainString()
    }

    /** Percent of an amount, rounded to the nearest paisa. 5% of 10,000 -> 500. */
    fun percentOf(paisa: Long, percent: Double): Long =
        BigDecimal.valueOf(paisa)
            .multiply(BigDecimal.valueOf(percent))
            .divide(BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
}
