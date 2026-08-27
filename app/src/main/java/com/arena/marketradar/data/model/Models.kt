package com.arena.marketradar.data.model

/** Category of a market instrument. */
enum class AssetType { FIAT, METAL, CRYPTO }

/** Which market the price is quoted in (Iran local vs. global). */
enum class MarketScope { IRAN, GLOBAL }

/** Display / quoting unit. */
enum class PriceUnit { TOMAN, RIAL, USD, EUR, GRAM, OUNCE, COIN }

/** Unified, market-agnostic price point used across the whole app. */
data class MarketPrice(
    val symbol: String,
    val nameFa: String,
    val nameEn: String,
    val type: AssetType,
    val scope: MarketScope,
    val unit: PriceUnit,
    val price: Double,
    val priceUsd: Double? = null,
    val change24h: Double? = null,
    val changePercent24h: Double? = null,
    val high24h: Double? = null,
    val low24h: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val iconUrl: String? = null,
    val source: String,
) {
    val numericValue: Double get() = price
}

/** Directional signal produced by the forecast engine. */
enum class Signal { BULLISH, BEARISH, NEUTRAL }

enum class ConfidenceLevel { LOW, MEDIUM, HIGH }

/** A forecast/signal for one instrument. */
data class ForecastItem(
    val symbol: String,
    val nameFa: String,
    val nameEn: String,
    val signal: Signal,
    val probability: Int,
    val horizon: String,
    val expectedMovePercent: Double,
    val confidence: ConfidenceLevel,
    val summaryFa: String,
    val summaryEn: String,
    val factors: List<String>,
    val dataPoints: Int,
    val sentiment: Double,
    // ---- Extended (v1.2) ----
    val prob24h: Int? = null,
    val prob72h: Int? = null,
    val prob1w: Int? = null,
    val accuracy: Double? = null,          // backtested hit-rate (0..1)
    val fearGreed: Int? = null,            // 0..100
    val rsi: Double? = null,
    val macdHist: Double? = null,
    val resistance: Double? = null,
    val support: Double? = null,
    val evidence: List<Evidence> = emptyList(),
    val correlations: List<Correlation> = emptyList(),
)

/** A single weighted piece of evidence behind a signal (for transparency). */
data class Evidence(
    val label: String,
    val score: Double,     // -1..+1
    val weight: Double,    // 0..1 (importance)
    val positive: Boolean,
)

/** Correlation of this asset with another tracked asset. */
data class Correlation(
    val symbol: String,
    val nameEn: String,
    val value: Double,     // -1..+1
)


/** News article with on-device sentiment and detected language. */
data class NewsItem(
    val title: String,
    val link: String,
    val source: String,
    val published: Long,
    val sentiment: Double,
    val sentimentLabel: String,
    val assets: List<String>,
    val isPersian: Boolean = false,
)

enum class AlertCondition { ABOVE, BELOW, CROSS_ABOVE, CROSS_BELOW }

/** A user defined price alert. */
data class AlertItem(
    val id: String,
    val symbol: String,
    val nameFa: String,
    val condition: AlertCondition,
    val targetPrice: Double,
    val unit: PriceUnit,
    val enabled: Boolean,
    /** Previous observed price, used to detect a genuine cross. */
    val lastPrice: Double? = null,
)

/** Description of a trackable instrument (used to build the home list). */
data class AssetDefinition(
    val symbol: String,
    val nameFa: String,
    val nameEn: String,
    val type: AssetType,
    val scope: MarketScope,
    val unit: PriceUnit,
    val group: String,
    val coingeckoId: String? = null,
    val iconUrl: String? = null,
    val emoji: String,
)

/** One point of price history. */
data class PricePoint(val timestamp: Long, val value: Double)

/** A user holding in the portfolio. */
data class Holding(
    val id: String = java.util.UUID.randomUUID().toString(),
    val symbol: String,
    val nameFa: String,
    val unit: PriceUnit,
    val quantity: Double,
    val buyPrice: Double,
    val addedAt: Long = System.currentTimeMillis(),
    val category: String = "سایر",   // short / long / سایر
    val fees: Double = 0.0,
)

/** A single row of computed portfolio results for a holding. */
data class PortfolioRow(
    val holding: Holding,
    val currentPrice: Double,
    val costValue: Double,
    val marketValue: Double,
    val pnl: Double,
    val pnlPercent: Double,
)

/** A macro-economic / market event for the calendar. */
data class EconEvent(
    val title: String,
    val country: String,
    val impact: String,          // HIGH / MEDIUM / LOW
    val date: Long,
    val tagFa: String,
    val tagEn: String,
)

/** A virtual (paper) trade in the trading simulator. */
data class PaperTrade(
    val id: String = java.util.UUID.randomUUID().toString(),
    val symbol: String,
    val nameFa: String,
    val unit: PriceUnit,
    val side: TradeSide,
    val qty: Double,
    val entryPrice: Double,
    val time: Long = System.currentTimeMillis(),
)

enum class TradeSide { BUY, SELL }

/** Live result of a paper trade. */
data class PaperTradeRow(
    val trade: PaperTrade,
    val currentPrice: Double,
    val pnl: Double,
    val pnlPercent: Double,
)
