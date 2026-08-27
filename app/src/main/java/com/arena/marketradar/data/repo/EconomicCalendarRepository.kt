package com.arena.marketradar.data.repo

import com.arena.marketradar.data.model.EconEvent
import java.util.Calendar

/** Curated high-impact macro events (Fed, ECB, CPI, NFP, OPEC…) for the next days. */
class EconomicCalendarRepository {

    fun upcoming(): List<EconEvent> {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L

        fun dt(offsetDays: Int, hour: Int, minute: Int = 0): Long {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, offsetDays)
            c.set(Calendar.HOUR_OF_DAY, hour)
            c.set(Calendar.MINUTE, minute)
            c.set(Calendar.SECOND, 0)
            return c.timeInMillis
        }

        val events = listOf(
            EconEvent("FOMC Interest Rate Decision", "US", "HIGH", dt(3, 21, 0), "نرخ بهره فدرال رزرو", "Fed rate decision"),
            EconEvent("US CPI (Inflation) YoY", "US", "HIGH", dt(2, 13, 30), "تورم آمریکا (CPI)", "US CPI"),
            EconEvent("NFP — Nonfarm Payrolls", "US", "HIGH", dt(4, 17, 0), "اشتغال غیرکشاورزی", "US Jobs"),
            EconEvent("ECB Interest Rate Decision", "EU", "HIGH", dt(5, 15, 15), "نرخ بهره بانک مرکزی اروپا", "ECB"),
            EconEvent("OPEC+ Monthly Meeting", "World", "MEDIUM", dt(1, 12, 0), "نشست اوپک و نفت", "OPEC / Oil"),
            EconEvent("FOMC Chair Press Conference", "US", "HIGH", dt(3, 23, 30), "نشست خبری رئیس فدرال رزرو", "Fed presser"),
            EconEvent("US Retail Sales MoM", "US", "MEDIUM", dt(6, 13, 30), "فروش خرده‌فروشی آمریکا", "Retail sales"),
            EconEvent("Iran Official Inflation (SCI)", "IR", "MEDIUM", dt(2, 9, 0), "تورم رسمی ایران", "Iran inflation"),
            EconEvent("Bitcoin Options Expiry", "World", "MEDIUM", dt(1, 12, 0), "سررسید قراردادهای اختیار بیت‌کوین", "BTC expiry"),
        )

        return events.map { it.copy(date = it.date.takeIf { d -> d >= now } ?: dt(7, 10, 0)) }.sortedBy { it.date }
    }
}
