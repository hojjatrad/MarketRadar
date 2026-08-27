package com.arena.marketradar.domain.analysis

/**
 * Lightweight, on-device sentiment scoring for news headlines.
 * Uses a bilingual (Farsi + English) financial keyword lexicon. This is a
 * pragmatic lexical model — it is not a neural net — and is used only as one
 * input to the forecast, alongside technical indicators.
 */
class SentimentAnalyzer {

    private val positive = setOf(
        // English
        "surge", "rally", "soar", "jump", "gain", "rise", "record", "high", "bull", "bullish",
        "boost", "growth", "increase", "optimism", "profit", "upgrad", "breakout", "peak",
        "strong", "beat", "outperform", "support", "recover", "rebound", "boom", "up",
        // Farsi
        "صعود", "رشد", "سود", "افزایش", "گران", "صعودی", "بازگشت", "رونق", "بهبود", "ظرفیت",
        "رکورد", "بالا", "برد", "خبر خوب", "قوت", "بازار صعودی", "ارزش افزوده", "سودده", "پشتوانه",
    )

    private val negative = setOf(
        // English
        "drop", "fall", "crash", "plunge", "tumble", "slide", "decline", "bear", "bearish",
        "slump", "loss", "downgrad", "fear", "risk", "selloff", "dip", "weak", "worst",
        "warning", "volatile", "uncertain", "inflation", "recession", "sanction", "ban",
        "crisis", "down", "collapse", "resistance",
        // Farsi
        "سقوط", "افت", "کاهش", "ارزان", "بحران", "نگرانی", "ریسک", "ضعیف", "تحریم", "ممنوع",
        "تورم", "رکود", "خطر", "فروش", "زیان", "نزولی", "سقوط قیمت", "تصفیه", "نوسان", "بی‌ثباتی",
    )

    private val assetWords = mapOf(
        "BTC" to listOf("bitcoin", "بیت‌کوین", "بیت کوین"),
        "ETH" to listOf("ethereum", "اتریوم"),
        "XAU" to listOf("gold", "طلا", "اونس", "gold price"),
        "USD" to listOf("dollar", "دلار", "usd", "dollar rate", "exchange rate", "دلار آمریکا"),
        "EUR" to listOf("euro", "یورو"),
        "USDT" to listOf("tether", "تتر"),
        "SOL" to listOf("solana", "سولانا"),
        "XRP" to listOf("ripple", "ریپل"),
        "SEKKE" to listOf("سکه", "sekke", "coin"),
        "OIL" to listOf("oil", "نفت"),
    )

    fun score(text: String): Double {
        val tokens = tokenize(text)
        if (tokens.isEmpty()) return 0.0
        var pos = 0
        var neg = 0
        for (t in tokens) {
            when {
                positive.contains(t) -> pos++
                negative.contains(t) -> neg++
            }
        }
        val total = pos + neg
        if (total == 0) return 0.0
        return (pos - neg).toDouble() / total
    }

    fun label(s: Double): String = when {
        s > 0.15 -> "+"
        s < -0.15 -> "-"
        else -> "0"
    }

    fun assetsIn(text: String): List<String> {
        val lower = text.lowercase()
        return assetWords.filter { (_, words) -> words.any { lower.contains(it.lowercase()) } }.keys.toList()
    }

    private fun tokenize(text: String): List<String> {
        // Match both English words and contiguous Persian characters.
        val re = Regex("[a-z]+|[\\u0600-\\u06FF]+")
        return re.findAll(text.lowercase()).map { it.value }.toList()
    }
}
