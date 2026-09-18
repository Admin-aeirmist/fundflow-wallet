package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FundFlow", appName)
    }

    @Test
    fun testCurrencyFormatting() {
        val formatted = CurrencyFormatter.formatBDT(15000.0)
        assertTrue(formatted.contains("৳"))
        assertTrue(formatted.contains("15,000.00"))
    }

    @Test
    fun testDpsNextDueDateCalculation() {
        val nextDue = DateUtils.calculateNextMonthDueDate("2026-01-15", 15)
        assertEquals("2026-02-15", nextDue)

        // Leap year / month boundary clamp
        val nextDueFrom31st = DateUtils.calculateNextMonthDueDate("2026-01-31", 31)
        assertEquals("2026-02-28", nextDueFrom31st)
    }
}
