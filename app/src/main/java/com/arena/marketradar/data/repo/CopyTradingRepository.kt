package com.arena.marketradar.data.repo

import android.content.Context
import com.arena.marketradar.data.model.CopyTradeState
import com.arena.marketradar.data.model.FollowedTrader
import com.arena.marketradar.data.model.Trader
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.random.Random

/**
 * A serverless COPY-TRADING SIMULATOR.
 *
 * It presents a leaderboard of simulated "traders". The user follows a trader
 * and allocates virtual capital. On each [tick] the engine generates a daily
 * return for every trader (seeded by trader-id + day so it's stable through the
 * day but evolves day to day) from their avg return + volatility + random
 * noise. Following traders' equity is updated accordingly and the user's P&L is
 * computed. No real money, no backend — purely educational/demo.
 */
class CopyTradingRepository(context: Context) {

    private val prefs = context.getSharedPreferences("marketradar_copytrade", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val startingBalance = 10_000_000.0   // 10 million (virtual Toman)

    // ---- Roster of simulated traders (deterministic profile) ----
    val roster: List<Trader> = listOf(
        Trader("t_btc1", "آرش — بیت‌کوین ۱", "🐂", "بیت‌کوین / اتریوم", "متوسط", 0.62, 0.45, 2.4, 12840, "متعادل"),
        Trader("t_gold1", "سارا — طلا و سکه", "🥇", "طلا / سکه امامی", "محافظه‌کار", 0.71, 0.18, 0.9, 8021, "کم‌ریسک"),
        Trader("t_fx1", "رضا — دلار و ارز", "💵", "دلار / یورو / تتر", "متوسط", 0.58, 0.22, 1.3, 9635, "متعادل"),
        Trader("t_alt1", "مه‌سا — آلت‌کوین", "🚀", "سولانا / دوج / ریپل", "تهاجمی", 0.54, 0.85, 5.6, 17120, "پرریسک"),
        Trader("t_btc2", "کیان — بیت‌کوین ۲", "⚡", "بیت‌کوین (لانگ/شورت)", "تهاجمی", 0.49, 0.55, 4.1, 23905, "پرریسک"),
        Trader("t_bal1", "نازنین — سبد متعادل", "🧺", "سبد طلا+دلار+بیت‌کوین", "محافظه‌کار", 0.66, 0.30, 1.1, 5540, "کم‌ریسک"),
        Trader("t_eth1", "پویا — اتریوم", "Ξ", "اتریوم / تتر", "متوسط", 0.60, 0.40, 2.9, 6450, "متعادل"),
        Trader("t_scalp", "عارف — اسکالپ", "🔪", "معاملات کوتاه‌مدت", "تهاجمی", 0.52, 0.70, 6.3, 11030, "پرریسک"),
    )

    // ---- in-memory + persisted state ----
    private var follows: List<FollowedTrader> = load()
    private var balance: Double = prefs.getFloat("balance", startingBalance.toFloat()).toDouble()

    fun balance(): Double = balance

    /** Allocates virtual capital to follow a trader. */
    fun follow(traderId: String, amount: Double): CopyTradeState {
        val t = roster.firstOrNull { it.id == traderId } ?: return state()
        if (follows.any { it.traderId == traderId }) return state()
        val clamped = amount.coerceIn(100_000.0, balance)
        follows = follows + FollowedTrader(
            traderId = traderId, name = t.name, emoji = t.emoji,
            strategy = t.strategy, style = t.style,
            allocated = clamped, equity = clamped, pnl = 0.0, pnlPercent = 0.0,
            since = System.currentTimeMillis(),
        )
        balance -= clamped
        persist()
        return tick()
    }

    fun unfollow(traderId: String): CopyTradeState {
        follows.firstOrNull { it.traderId == traderId }?.let { balance += it.equity }
        follows = follows.filterNot { it.traderId == traderId }
        persist()
        return tick()
    }

    fun reset(): CopyTradeState {
        balance = startingBalance
        follows = emptyList()
        persist()
        return state()
    }

    /**
     * Advances the simulation one "day": draws a return for each trader and
     * recomputes followed-equity.
     */
    fun tick(): CopyTradeState {
        val day = System.currentTimeMillis() / (24 * 60 * 60 * 1000L)
        val benchmark = benchmarkToday(day)

        val newFollows = follows.map { f ->
            val t = roster.firstOrNull { it.id == f.traderId } ?: return@map f
            val r = traderDailyReturn(t, day)
            val newEquity = (f.equity * (1 + r / 100.0)).coerceAtLeast(0.3)
            val pnl = newEquity - f.allocated
            f.copy(equity = newEquity, pnl = pnl, pnlPercent = if (f.allocated > 0) pnl / f.allocated * 100.0 else 0.0)
        }
        follows = newFollows
        persist()

        val totalPnl = newFollows.sumOf { it.pnl }
        return CopyTradeState(
            balance = balance,
            totalPnl = totalPnl,
            followers = newFollows,
            winRate = if (newFollows.isNotEmpty()) newFollows.count { it.pnl > 0 }.toDouble() / newFollows.size else 0.0,
            benchmarkReturn = benchmark,
        )
    }

    fun state(): CopyTradeState {
        val totalPnl = follows.sumOf { it.pnl }
        return CopyTradeState(
            balance = balance, totalPnl = totalPnl, followers = follows,
            winRate = if (follows.isNotEmpty()) follows.count { it.pnl > 0 }.toDouble() / follows.size else 0.0,
            benchmarkReturn = benchmarkToday(System.currentTimeMillis() / (24 * 60 * 60 * 1000L)),
        )
    }

    fun equityOf(traderId: String): Double = follows.firstOrNull { it.traderId == traderId }?.equity ?: 0.0

    private fun traderDailyReturn(t: Trader, day: Long): Double {
        val h = t.id.hashCode() * 31 + day * 17
        val rng = Random(h)
        val noise = (rng.nextDouble() - 0.5) * 2.0 * t.volatility
        return (t.avgReturnPct + noise).coerceIn(-t.volatility * 3.0, t.volatility * 3.0)
    }

    private fun benchmarkToday(day: Long): Double {
        val rng = Random(day * 7 + 3)
        return (rng.nextDouble() - 0.35) * 4.0
    }

    private fun load(): MutableList<FollowedTrader> {
        val json = prefs.getString("follows", null) ?: return mutableListOf()
        return try {
            val t = object : TypeToken<MutableList<FollowedTrader>>() {}.type
            gson.fromJson<MutableList<FollowedTrader>>(json, t) ?: mutableListOf()
        } catch (e: Exception) { mutableListOf() }
    }
    private fun persist() {
        prefs.edit().putString("follows", gson.toJson(follows)).apply()
        prefs.edit().putFloat("balance", balance.toFloat()).apply()
    }

    companion object {
        fun monthlyReturn(t: Trader): Double = t.avgReturnPct * 30
    }
}
