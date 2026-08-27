package com.arena.marketradar.domain.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.arena.marketradar.data.model.ForecastItem
import com.arena.marketradar.data.model.MarketPrice
import com.arena.marketradar.data.model.PriceUnit
import com.arena.marketradar.domain.util.Formatters
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Generates a weekly market report PDF on-device (no extra dependency). */
object ReportGenerator {

    private fun reportsDir(context: Context): File {
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun generate(context: Context, prices: List<MarketPrice>, forecasts: List<ForecastItem>, lang: String): File? {
        return try {
            val doc = PdfDocument()
            val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
            val canvas = page.canvas
            render(canvas, prices, forecasts, lang)
            doc.finishPage(page)

            val ts = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ENGLISH).format(Date())
            val file = File(reportsDir(context), "MarketRadar_Report_$ts.pdf")
            FileOutputStream(file).use { doc.writeTo(it) }
            doc.close()
            file
        } catch (e: Exception) { null }
    }

    /** Renders the first PDF page to a PNG so it can be shared as an image. */
    fun renderToPng(context: Context, pdf: File): File? {
        return try {
            val pfd = ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount == 0) return null
                renderer.openPage(0).use { page ->
                    val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val out = File(reportsDir(context), pdf.name.replace(".pdf", ".png"))
                    FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }
                    out
                }
            }
        } catch (e: Exception) { null }
    }

    private fun render(canvas: Canvas, prices: List<MarketPrice>, forecasts: List<ForecastItem>, lang: String) {
        val title = Paint().apply { color = Color.rgb(30, 158, 106); textSize = 26f; typeface = Typeface.DEFAULT_BOLD }
        val sub = Paint().apply { color = Color.DKGRAY; textSize = 12f }
        val body = Paint().apply { color = Color.BLACK; textSize = 12f }
        val head = Paint().apply { color = Color.rgb(30, 158, 106); textSize = 13f; typeface = Typeface.DEFAULT_BOLD }
        val red = Paint().apply { color = Color.rgb(225, 85, 77); textSize = 12f; typeface = Typeface.DEFAULT_BOLD }

        val date = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        canvas.drawText(if (lang == "fa") "گزارش بازار — رصد بازار" else "Market Report — Market Radar", 40f, 70f, title)
        canvas.drawText(if (lang == "fa") "تاریخ: $date  •  قیمت‌ها و سیگنال‌های زنده" else "Date: $date  •  Live prices & signals", 40f, 100f, sub)

        var y = 150f
        canvas.drawText(if (lang == "fa") "فهرست بازار" else "Market Overview", 60f, y, head)
        y += 60f
        for (p in prices) {
            val name = if (lang == "fa") p.nameFa else p.nameEn
            val price = Formatters.money(p.price, p.unit, if (lang == "fa") "fa" else "en")
            canvas.drawText("$name", 60f, y, body)
            canvas.drawText(price, 360f, y, body)
            y += 24f
            if (y > 760f) break
        }

        if (forecasts.isNotEmpty()) {
            y += 30f
            canvas.drawText(if (lang == "fa") "سیگنال‌های پیش‌بینی" else "Forecast Signals", 60f, y, head)
            y += 30f
            for (f in forecasts.take(12)) {
                val dir = if (lang == "fa") when (f.signal) {
                    com.arena.marketradar.data.model.Signal.BULLISH -> "صعودی"
                    com.arena.marketradar.data.model.Signal.BEARISH -> "نزولی"
                    else -> "خنثی"
                } else f.signal.name
                canvas.drawText(f.nameEn, 60f, y, body)
                canvas.drawText("$dir  ~${f.probability}%", 300f, y, if (f.signal == com.arena.marketradar.data.model.Signal.BEARISH) red else body)
                y += 24f
                if (y > 760f) break
            }
        }

        canvas.drawText(if (lang == "fa") "⚠️ این گزارش فقط تحلیلی است و توصیهٔ سرمایه‌گذاری نیست." else "⚠️ Informational analysis only — not investment advice.", 40f, 800f, sub)
    }
}
