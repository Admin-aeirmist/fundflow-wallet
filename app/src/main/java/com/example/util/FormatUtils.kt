package com.example.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CurrencyFormatter {
    fun formatBDT(amount: Double): String {
        val isNegative = amount < 0
        val absAmount = kotlin.math.abs(amount)
        val numStr = try {
            val decFormat = NumberFormat.getNumberInstance(Locale("en", "IN")) as DecimalFormat
            decFormat.applyPattern("#,##,##0.00")
            decFormat.format(absAmount)
        } catch (e: Exception) {
            String.format(Locale.US, "%.2f", absAmount)
        }
        val prefix = if (isNegative) "-৳ " else "৳ "
        return "$prefix$numStr"
    }
}

object DateUtils {
    private val ymdFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

    fun today(): String {
        return ymdFormat.format(Date())
    }

    fun daysFromNow(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, days)
        return ymdFormat.format(cal.time)
    }

    fun formatDisplay(dateStr: String): String {
        return try {
            val date = ymdFormat.parse(dateStr)
            if (date != null) displayFormat.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    fun isOverdue(dueDateStr: String): Boolean {
        return try {
            val todayStr = today()
            dueDateStr < todayStr
        } catch (e: Exception) {
            false
        }
    }

    fun calculateNextMonthDueDate(currentDueDateStr: String, targetDayOfMonth: Int): String {
        return try {
            val cal = Calendar.getInstance()
            val parsed = ymdFormat.parse(currentDueDateStr)
            if (parsed != null) cal.time = parsed
            cal.add(Calendar.MONTH, 1)
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val clampedDay = targetDayOfMonth.coerceIn(1, maxDay)
            cal.set(Calendar.DAY_OF_MONTH, clampedDay)
            ymdFormat.format(cal.time)
        } catch (e: Exception) {
            today()
        }
    }

    fun calculateInitialDpsDueDate(dayOfMonth: Int): String {
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val targetDay = dayOfMonth.coerceIn(1, maxDay)
        cal.set(Calendar.DAY_OF_MONTH, targetDay)

        if (targetDay < currentDay) {
            cal.add(Calendar.MONTH, 1)
            val nextMaxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, nextMaxDay))
        }
        return ymdFormat.format(cal.time)
    }
}
