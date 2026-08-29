package com.arena.marketradar.data.model

/** A simulated (paper) trader shown in the copy-trading leaderboard. */
data class Trader(
    val id: String,
    val name: String,
    val emoji: String,
    val strategy: String,      // e.g. "بیت‌کوین / طلا"
    val style: String,         // محافظه‌کار / متوسط / تهاجمی
    val winRate: Double,       // 0..1
    val avgReturnPct: Double,  // average daily return %
    val volatility: Double,    // daily volatility % (drives randomness)
    val followers: Int,
    val riskLabel: String,     // کم‌ریسک / متعادل / پرریسک
)

/** A trader the user currently follows, with allocated virtual capital. */
data class FollowedTrader(
    val traderId: String,
    val name: String,
    val emoji: String,
    val strategy: String,
    val style: String,
    val allocated: Double,       // virtual capital allocated to this trader
    val equity: Double,          // current virtual value of the position
    val pnl: Double,             // equity - allocated
    val pnlPercent: Double,      // pnl / allocated * 100
    val since: Long,             // when the user started following
)

/** Overall copy-trading simulation state (virtual wallet). */
data class CopyTradeState(
    val balance: Double,             // unallocated virtual cash
    val totalPnl: Double,            // sum of P&L across followers
    val followers: List<FollowedTrader>,
    val winRate: Double,             // aggregate win-rate of followed traders
    val benchmarkReturn: Double,     // e.g. BTC buy&hold comparison
)
