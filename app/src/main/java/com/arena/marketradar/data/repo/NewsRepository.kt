package com.arena.marketradar.data.repo

import com.arena.marketradar.data.model.NewsItem
import com.arena.marketradar.domain.analysis.SentimentAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URLEncoder
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Fetches finance/crypto/gold news from many public Google News RSS feeds
 * (Persian + English) and scores sentiment on-device. Persian & English
 * language is detected from the title so the UI can filter by language.
 */
class NewsRepository(private val analyzer: SentimentAnalyzer) {

    /** (query, language) pairs; Persian feeds come first so they surface quickly. */
    private val feeds: List<String> = buildList {
        fun add(query: String, lang: String) {
            val langTag = if (lang == "fa") "fa&gl=IR&ceid=IR:fa" else "en-US&gl=US&ceid=US:en"
            val encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
            add("https://news.google.com/rss/search?q=$encoded&hl=$langTag")
        }
        // ---- Persian (fa) ----
        add("بیت کوین", "fa"); add("طلا", "fa"); add("دلار", "fa"); add("سکه", "fa")
        add("ارز دیجیتال", "fa"); add("بورس", "fa"); add("بانک مرکزی", "fa")
        add("تورم", "fa"); add("نفت", "fa"); add("بازار طلا", "fa")
        // ---- English (en) ----
        add("bitcoin", "en"); add("cryptocurrency gold", "en"); add("dollar exchange rate", "en")
        add("gold price forecast", "en"); add("interest rate fed", "en"); add("oil opec", "en")
    }

    suspend fun fetch(): List<NewsItem> = coroutineScope {
        val jobs = feeds.map { async(Dispatchers.IO) { safeParse(it) } }
        jobs.mapNotNull { it.await() }
            .flatten()
            .distinctBy { it.title }
            .sortedByDescending { it.published }
            .take(160)
    }

    private fun safeParse(url: String): List<NewsItem> = try { parse(url) } catch (e: Exception) { emptyList() }

    private fun parse(url: String): List<NewsItem> {
        val result = mutableListOf<NewsItem>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()

        val connection = URL(url).openConnection()
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) MarketRadar")
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        parser.setInput(connection.getInputStream(), null)

        var event = parser.eventType
        var inItem = false
        var title = ""
        var link = ""
        var pubDate = ""
        var source = ""
        val dateFormats = listOf(
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH),
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH),
        )

        fun flush() {
            if (title.isNotBlank() && link.isNotBlank()) {
                val published = dateFormats.firstNotNullOfOrNull { fmt ->
                    try { fmt.parse(pubDate)?.time } catch (e: Exception) { null }
                } ?: System.currentTimeMillis()
                result += NewsItem(
                    title = title.trim(),
                    link = link.trim(),
                    source = source.ifBlank { "News" },
                    published = published,
                    sentiment = analyzer.score(title),
                    sentimentLabel = analyzer.label(analyzer.score(title)),
                    assets = analyzer.assetsIn(title),
                    isPersian = containsPersian(title),
                )
            }
            title = ""; link = ""; pubDate = ""; source = ""
        }

        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name?.lowercase()) {
                    "item" -> inItem = true
                    "title" -> if (inItem) title = parser.nextText().ifEmpty { title }
                    "link" -> if (inItem) link = parser.nextText().ifEmpty { link }
                    "pubdate" -> if (inItem) pubDate = parser.nextText().ifEmpty { pubDate }
                    "description" -> if (inItem && title.isBlank()) title = parser.nextText().ifEmpty { title }
                    "source" -> if (inItem) source = parser.nextText().ifEmpty { source }
                }
            } else if (event == XmlPullParser.END_TAG && parser.name?.lowercase() == "item") {
                inItem = false
                flush()
            }
            event = parser.next()
        }
        return result
    }

    private fun containsPersian(text: String): Boolean {
        for (ch in text) if (ch in '\u0600'..'\u06FF') return true
        return false
    }
}
