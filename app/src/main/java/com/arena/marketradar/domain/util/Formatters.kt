package com.arena.marketradar.domain.util

import com.arena.marketradar.data.model.PriceUnit
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object Formatters {

    /** When true, numeric output uses Persian (Eastern Arabic) digits. */
    @Volatile
    var usePersianDigits: Boolean = true

    /** Converts to Persian digits & separators, respecting the flag. */
    fun localize(num: String, lang: String = "fa"): String {
        if (lang != "fa" || !usePersianDigits) return num
        return buildString {
            for (ch in num) {
                when (ch) {
                    '0' -> append('۰'); '1' -> append('۱'); '2' -> append('۲')
                    '3' -> append('۳'); '4' -> append('۴'); '5' -> append('۵')
                    '6' -> append('۶'); '7' -> append('۷'); '8' -> append('۸')
                    '9' -> append('۹')
                    '.' -> append('٫'); ',' -> append('٬'); '-' -> append('−')
                    else -> append(ch)
                }
            }
        }
    }

    fun money(value: Double, unit: PriceUnit, lang: String = "fa"): String {
        val formatter = NumberFormat.getNumberInstance(Locale(lang))
        formatter.maximumFractionDigits = when (unit) {
            PriceUnit.GRAM, PriceUnit.OUNCE -> 2
            PriceUnit.USD, PriceUnit.EUR -> 2
            PriceUnit.TOMAN -> 0
            PriceUnit.RIAL -> 0
            PriceUnit.COIN -> 0
        }
        val num = formatter.format(value)
        return when (lang) {
            "fa" -> "${localize(num, lang)} ${unitFa(unit)}"
            else -> "$num ${unitEn(unit)}"
        }
    }

    fun plain(value: Double, maxDecimals: Int = 2): String {
        val formatter = NumberFormat.getNumberInstance(Locale("en"))
        formatter.maximumFractionDigits = maxDecimals
        formatter.minimumFractionDigits = 0
        return localize(formatter.format(value))
    }

    fun unitEn(unit: PriceUnit): String = when (unit) {
        PriceUnit.TOMAN -> "Toman"; PriceUnit.RIAL -> "Rial"; PriceUnit.USD -> "$"
        PriceUnit.EUR -> "€"; PriceUnit.GRAM -> "g"; PriceUnit.OUNCE -> "oz"; PriceUnit.COIN -> ""
    }

    fun unitFa(unit: PriceUnit): String = when (unit) {
        PriceUnit.TOMAN -> "تومان"; PriceUnit.RIAL -> "ریال"; PriceUnit.USD -> "دلار"
        PriceUnit.EUR -> "یورو"; PriceUnit.GRAM -> "گرم"; PriceUnit.OUNCE -> "اونس"; PriceUnit.COIN -> ""
    }

    fun percent(value: Double): String = localize("${if (value >= 0) "+" else ""}%.1f".format(value)) + "%"

    fun signed(value: Double): String = "${if (value >= 0) "+" else ""}${plain(value)}"

    /** Applies the user's display-unit preference (Toman ⇄ Rial). */
    fun displayPrice(assetUnit: PriceUnit, price: Double, prefUnit: PriceUnit): Pair<Double, PriceUnit> =
        when {
            assetUnit == PriceUnit.TOMAN && prefUnit == PriceUnit.RIAL -> Pair(price * 10.0, PriceUnit.RIAL)
            assetUnit == PriceUnit.RIAL && prefUnit == PriceUnit.TOMAN -> Pair(price / 10.0, PriceUnit.TOMAN)
            else -> Pair(price, assetUnit)
        }

    fun timeShort(millis: Long): String {
        val fmt = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, java.util.Locale("fa"))
        return fmt.format(java.util.Date(millis))
    }

    fun changeColor(value: Double?): Long =
        when {
            value == null || abs(value) < 0.0001 -> 0xFF607D8B
            value > 0 -> 0xFF1E9E6A
            else -> 0xFFE1554D
        }.let { it }
}
