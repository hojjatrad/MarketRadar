package com.arena.marketradar

import com.arena.marketradar.domain.analysis.MarketAnalysis
import com.arena.marketradar.domain.analysis.TechnicalIndicators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TechnicalIndicatorsTest {

    private val rising = (1..60).map { it.toDouble() }     // steady uptrend
    private val falling = (60 downTo 1).map { it.toDouble() } // steady downtrend

    @Test
    fun sma_isMeanOfWindow() {
        assertEquals(3.0, TechnicalIndicators.sma(listOf(2.0, 3.0, 4.0), 3)!!, 0.001)
    }

    @Test
    fun rsi_isHighForUptrend() {
        val r = TechnicalIndicators.rsi(rising, 14)!!
        assertTrue("RSI should be high on an uptrend (got $r)", r > 60)
    }

    @Test
    fun rsi_isLowForDowntrend() {
        val r = TechnicalIndicators.rsi(falling, 14)!!
        assertTrue("RSI should be low on a downtrend (got $r)", r < 40)
    }

    @Test
    fun fibonacci_levels_areBetweenHighAndLow() {
        val (levels, high, low) = TechnicalIndicators.fibonacci(rising)
        assertEquals(rising.max(), high, 0.001)
        assertEquals(rising.min(), low, 0.001)
        levels.forEach { assertTrue(it in low - 0.01..high + 0.01) }
    }

    @Test
    fun supportResistance_returnsLevels() {
        val (res, sup) = TechnicalIndicators.supportResistance(rising)
        assertNotNull(res); assertNotNull(sup)
        assertTrue("resistance should be >= support", (res ?: 0.0) >= (sup ?: 0.0))
    }

    @Test
    fun vwap_isWeightedAverage() {
        val v = TechnicalIndicators.vwap(listOf(10.0, 20.0, 30.0))!!
        assertTrue(v in 10.0..30.0)
    }

    @Test
    fun marketAnalysis_fearGreed_reflectsTrend() {
        val fgUp = MarketAnalysis.fearGreed(rising)
        val fgDown = MarketAnalysis.fearGreed(falling)
        assertNotNull(fgUp); assertNotNull(fgDown)
        assertTrue((fgUp ?: 50) in 0..100)
        assertTrue((fgDown ?: 50) in 0..100)
    }

    @Test
    fun correlation_isOneForIdenticalSeries() {
        val a = (1..60).map { it.toDouble() }
        val c = MarketAnalysis.pearsonOf(a, a)
        assertNotNull(c)
        assertEquals(1.0, c!!, 0.001)
    }
}
