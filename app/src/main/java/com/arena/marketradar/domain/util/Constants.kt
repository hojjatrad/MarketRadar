package com.arena.marketradar.domain.util

import com.arena.marketradar.data.model.AssetDefinition
import com.arena.marketradar.data.model.AssetType
import com.arena.marketradar.data.model.MarketScope
import com.arena.marketradar.data.model.PriceUnit

object Constants {
    /** GitHub-agnostic CoinGecko ids we track. */
    val CRYPTO_IDS = listOf(
        "bitcoin", "ethereum", "tether", "binancecoin", "solana", "ripple",
        "dogecoin", "cardano", "chainlink", "the-open-network", "avalanche-2",
        "tron", "litecoin", "uniswap", "cosmos", "polkadot"
    )

    /** Coingecko ids that always have deep history for technical analysis. */
    val DEEP_HISTORY_COINS = listOf("bitcoin", "ethereum", "tether", "binancecoin", "solana")

    /** All fiat symbols (Iran, Toman) fetched from baha24. */
    val IRAN_FIAT = listOf(
        "USD", "EUR", "GBP", "AED", "TRY", "CAD", "AUD", "CHF", "CNY", "JPY", "RUB",
        "MYR", "AFN", "SEK", "NOK", "OMR", "KWD", "DKK", "AZN", "SGD", "THB", "INR"
    )

    /** Global fiat cross rates (USD base) via Frankfurter. */
    val GLOBAL_FIAT = listOf("EUR", "GBP", "AED", "TRY", "CAD", "AUD", "CHF")

    /**
     * Full instrument catalogue. `group` drives section headings in the UI.
     * Crypto items carry a coingecko id + icon; fiat/gold carry none.
     */
    val ASSETS: List<AssetDefinition> = listOf(
        // ---- Crypto (global, USD) ----
        AssetDefinition("BTC", "بیت‌کوین", "Bitcoin", AssetType.CRYPTO, MarketScope.GLOBAL, PriceUnit.USD, "crypto", coingeckoId = "bitcoin", emoji = "₿"),
        AssetDefinition("ETH", "اتریوم", "Ethereum", AssetType.CRYPTO, MarketScope.GLOBAL, PriceUnit.USD, "crypto", coingeckoId = "ethereum", emoji = "Ξ"),
        AssetDefinition("USDT", "تتر", "Tether", AssetType.CRYPTO, MarketScope.GLOBAL, PriceUnit.USD, "crypto", coingeckoId = "tether", emoji = "₮"),
        AssetDefinition("BNB", "بایننس‌کوین", "BNB", AssetType.CRYPTO, MarketScope.GLOBAL, PriceUnit.USD, "crypto", coingeckoId = "binancecoin", emoji = "🟡"),
        AssetDefinition("SOL", "سولانا", "Solana", AssetType.CRYPTO, MarketScope.GLOBAL, PriceUnit.USD, "crypto", coingeckoId = "solana", emoji = "◎"),
        AssetDefinition("XRP", "ریپل", "XRP", AssetType.CRYPTO, MarketScope.GLOBAL, PriceUnit.USD, "crypto", coingeckoId = "ripple", emoji = "✕"),
        AssetDefinition("DOGE", "دوج‌کوین", "Dogecoin", AssetType.CRYPTO, MarketScope.GLOBAL, PriceUnit.USD, "crypto", coingeckoId = "dogecoin", emoji = "🐕"),
        AssetDefinition("ADA", "کاردانو", "Cardano", AssetType.CRYPTO, MarketScope.GLOBAL, PriceUnit.USD, "crypto", coingeckoId = "cardano", emoji = "🔷"),
        AssetDefinition("LINK", "چین‌لینک", "Chainlink", AssetType.CRYPTO, MarketScope.GLOBAL, PriceUnit.USD, "crypto", coingeckoId = "chainlink", emoji = "🔗"),
        AssetDefinition("TON", "تون‌کوین", "Toncoin", AssetType.CRYPTO, MarketScope.GLOBAL, PriceUnit.USD, "crypto", coingeckoId = "the-open-network", emoji = "💎"),
        AssetDefinition("AVAX", "آوالانچ", "Avalanche", AssetType.CRYPTO, MarketScope.GLOBAL, PriceUnit.USD, "crypto", coingeckoId = "avalanche-2", emoji = "🔺"),
        AssetDefinition("TRX", "ترون", "TRON", AssetType.CRYPTO, MarketScope.GLOBAL, PriceUnit.USD, "crypto", coingeckoId = "tron", emoji = "🌊"),

        // ---- Gold & metals ----
        AssetDefinition("XAU", "اونس طلا", "Gold (Ounce)", AssetType.METAL, MarketScope.GLOBAL, PriceUnit.OUNCE, "metal", emoji = "🥇"),
        AssetDefinition("GOL18", "گرم طلا ۱۸ عیار", "Gold Gram 18K", AssetType.METAL, MarketScope.IRAN, PriceUnit.GRAM, "metal", emoji = "🪙"),
        AssetDefinition("EMAMI1", "سکه امامی", "Emami Coin", AssetType.METAL, MarketScope.IRAN, PriceUnit.COIN, "metal", emoji = "🟠"),
        AssetDefinition("AZADI1", "تمام سکه", "Full Azadi Coin", AssetType.METAL, MarketScope.IRAN, PriceUnit.COIN, "metal", emoji = "🟡"),
        AssetDefinition("AZADI1_2", "نیم سکه", "Half Azadi Coin", AssetType.METAL, MarketScope.IRAN, PriceUnit.COIN, "metal", emoji = "🟡"),
        AssetDefinition("AZADI1_4", "ربع سکه", "Quarter Azadi Coin", AssetType.METAL, MarketScope.IRAN, PriceUnit.COIN, "metal", emoji = "🟡"),
        AssetDefinition("AZADI1G", "سکه ۱ گرمی", "1 Gram Coin", AssetType.METAL, MarketScope.IRAN, PriceUnit.COIN, "metal", emoji = "🟡"),
        AssetDefinition("MITHQAL", "مثقال طلا", "Gold Mithqal", AssetType.METAL, MarketScope.IRAN, PriceUnit.COIN, "metal", emoji = "🪙"),

        // ---- Fiat (Iran, Toman) ----
        AssetDefinition("USD", "دلار آمریکا", "US Dollar", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "💵"),
        AssetDefinition("EUR", "یورو", "Euro", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "💶"),
        AssetDefinition("GBP", "پوند انگلیس", "British Pound", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "💷"),
        AssetDefinition("AED", "درهم امارات", "UAE Dirham", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "💰"),
        AssetDefinition("TRY", "لیر ترکیه", "Turkish Lira", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "💴"),
        AssetDefinition("CAD", "دلار کانادا", "Canadian Dollar", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "🪙"),
        AssetDefinition("AUD", "دلار استرالیا", "Australian Dollar", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "🪙"),
        AssetDefinition("CHF", "فرانک سوئیس", "Swiss Franc", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "🏔️"),
        AssetDefinition("CNY", "یوان چین", "Chinese Yuan", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "🀄"),
        AssetDefinition("JPY", "ین ژاپن", "Japanese Yen", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "🇯🇵"),
        AssetDefinition("RUB", "روبل روسیه", "Russian Ruble", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "🪆"),
        AssetDefinition("MYR", "رینگیت مالزی", "Malaysian Ringgit", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "🪙"),
        AssetDefinition("AFN", "افغانی", "Afghani", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "🪙"),
        AssetDefinition("SEK", "کرون سوئد", "Swedish Krona", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "🪙"),
        AssetDefinition("NOK", "کرون نروژ", "Norwegian Krone", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "🪙"),
        AssetDefinition("OMR", "ریال عمان", "Omani Rial", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "🪙"),
        AssetDefinition("KWD", "دینار کویت", "Kuwaiti Dinar", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "🪙"),
        AssetDefinition("DKK", "کرون دانمارک", "Danish Krone", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "🪙"),
        AssetDefinition("AZN", "منات آذربایجان", "Azerbaijani Manat", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "🪙"),
        AssetDefinition("SGD", "دلار سنگاپور", "Singapore Dollar", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "🪙"),
        AssetDefinition("THB", "بات تایلند", "Thai Baht", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "🪙"),
        AssetDefinition("INR", "روپیه هند", "Indian Rupee", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "🪙"),
        AssetDefinition("MEXUSD", "دلار صرافی ملی", "National Exchange Dollar", AssetType.FIAT, MarketScope.IRAN, PriceUnit.TOMAN, "fiat_iran", emoji = "💵"),
    )

    /** A large, searchable list of all selectable assets grouped by market. */
    val ALL_SELECTABLE: List<AssetDefinition> = ASSETS

    /** Default watchlist symbols shown first on the home screen. */
    val DEFAULT_WATCHLIST = listOf("USD", "USDT", "BTC", "XAU", "GOL18", "EMAMI1", "EUR", "ETH")

    /** Maps a tracked symbol to its news keyword key used by the sentiment analyzer. */
    fun newsKey(symbol: String): String = when (symbol) {
        "BTC" -> "BTC"; "ETH" -> "ETH"
        "XAU", "GOL18", "EMAMI1", "AZADI1", "AZADI1_2", "AZADI1_4", "AZADI1G", "MITHQAL" -> "XAU"
        "USD" -> "USD"; "EUR" -> "EUR"; "USDT" -> "USDT"; "SOL" -> "SOL"; "XRP" -> "XRP"
        else -> symbol
    }

    /** Persian display name for any tracked symbol (used when language is Persian). */
    fun nameFa(symbol: String): String = ASSETS.firstOrNull { it.symbol == symbol }?.nameFa ?: symbol

    /** Human-friendly Persian section title for an asset. */
    fun sectionTitle(type: AssetType, scope: MarketScope, lang: String): String = when (type) {
        AssetType.CRYPTO -> if (lang == "fa") "ارز دیجیتال" else "Cryptocurrency"
        AssetType.METAL -> if (lang == "fa") "طلا و فلزات" else "Gold & Metals"
        AssetType.FIAT -> if (lang == "fa") "ارز (بازار ایران)" else "Fiat (Iran Market)"
    }

    /** Group key for the picker: fiat/metal/crypto, sub-grouped by scope. */
    fun groupKey(type: AssetType, scope: MarketScope): String = when (type) {
        AssetType.CRYPTO -> "crypto"
        AssetType.METAL -> if (scope == MarketScope.IRAN) "metal_iran" else "metal_global"
        AssetType.FIAT -> "fiat_iran"
    }
}
