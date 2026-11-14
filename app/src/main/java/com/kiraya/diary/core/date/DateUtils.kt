package com.kiraya.diary.core.date

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Dates are stored as epoch millis. Months are keyed as yyyyMM ints (e.g. 202611). */
object DateUtils {
    const val DAY_MILLIS = 24L * 60 * 60 * 1000

    fun now(): Long = System.currentTimeMillis()

    fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun monthKey(millis: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return c.get(Calendar.YEAR) * 100 + (c.get(Calendar.MONTH) + 1)
    }

    fun currentMonthKey(): Int = monthKey(now())

    fun yearOf(monthKey: Int): Int = monthKey / 100

    fun monthOf(monthKey: Int): Int = monthKey % 100

    fun addMonths(monthKey: Int, months: Int): Int {
        val total = yearOf(monthKey) * 12 + (monthOf(monthKey) - 1) + months
        return (total / 12) * 100 + (total % 12) + 1
    }

    /** First millisecond of the month. */
    fun monthStart(monthKey: Int): Long = Calendar.getInstance().apply {
        clear()
        set(yearOf(monthKey), monthOf(monthKey) - 1, 1)
    }.timeInMillis

    /** First millisecond of the next month (exclusive end). */
    fun monthEnd(monthKey: Int): Long = monthStart(addMonths(monthKey, 1))

    /** Millis for a given day in the month; the day is clamped to the month length. */
    fun dayInMonth(monthKey: Int, day: Int): Long {
        val c = Calendar.getInstance().apply {
            clear()
            set(yearOf(monthKey), monthOf(monthKey) - 1, 1)
        }
        val last = c.getActualMaximum(Calendar.DAY_OF_MONTH)
        c.set(Calendar.DAY_OF_MONTH, day.coerceIn(1, last))
        return c.timeInMillis
    }

    fun dayOfMonth(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_MONTH)

    fun yearStart(year: Int): Long = monthStart(year * 100 + 1)

    fun yearEnd(year: Int): Long = monthStart((year + 1) * 100 + 1)

    fun addYears(millis: Long, years: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            add(Calendar.YEAR, years)
        }.timeInMillis

    fun formatDate(millis: Long, locale: Locale = Locale.getDefault()): String =
        SimpleDateFormat("dd MMM yyyy", locale).format(Date(millis))

    fun formatMonth(monthKey: Int, locale: Locale = Locale.getDefault()): String =
        SimpleDateFormat("MMMM yyyy", locale).format(Date(monthStart(monthKey)))

    fun formatMonthShort(monthKey: Int, locale: Locale = Locale.getDefault()): String =
        SimpleDateFormat("MMM yy", locale).format(Date(monthStart(monthKey)))

    /** Whole months between two dates (b - a), ignoring days. */
    fun monthsBetween(a: Long, b: Long): Int {
        val ka = monthKey(a)
        val kb = monthKey(b)
        return (yearOf(kb) - yearOf(ka)) * 12 + (monthOf(kb) - monthOf(ka))
    }

    /** Whole days from a to b (can be negative). */
    fun daysBetween(a: Long, b: Long): Int = ((startOfDay(b) - startOfDay(a)) / DAY_MILLIS).toInt()
}
