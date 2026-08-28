package com.arena.marketradar.domain.util

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * On-device translation of English text to Persian using Google's public
 * translate endpoint (no API key, no backend). Results are cached in memory so
 * the same string is never re-translated. Used to show EVERY news headline in
 * Persian when the app language is Persian.
 *
 * Note: this is a free/fair-use endpoint; if it is unavailable the app falls
 * back to the original English text gracefully.
 */
object TranslationHelper {

    private val cache = ConcurrentHashMap<String, String>()
    private const val MAX_LEN = 500  // truncate very long headlines

    suspend fun translateToFa(text: String): String? {
        val t = text.take(MAX_LEN)
        if (t.isBlank()) return text
        cache[t]?.let { return it }
        return runCatching {
            val enc = URLEncoder.encode(t, "UTF-8")
            val url = URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=fa&dt=t&q=$enc")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) MarketRadar")
            conn.setRequestProperty("Accept", "application/json")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val result = parse(body)
            if (!result.isNullOrBlank()) { cache[t] = result; result } else null
        }.getOrNull()
    }

    /** Google gtx response is a nested JSON array; extract the joined translated text. */
    private fun parse(body: String): String? = try {
        val root = JsonParser.parseString(body).asJsonArray
        val inner = root[0].asJsonArray
        val sb = StringBuilder()
        for (i in 0 until inner.size()) {
            val seg = inner[i].asJsonArray
            if (seg.size() > 0) sb.append(seg[0].asString)
        }
        sb.toString().ifBlank { null }
    } catch (e: Exception) { null }
}
