package com.arena.marketradar.ui.screens.copytrading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arena.marketradar.MarketRadarApplication
import com.arena.marketradar.data.model.CopyTradeState
import com.arena.marketradar.data.model.Trader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CopyTradingUiState(
    val loading: Boolean = true,
    val roster: List<Trader> = emptyList(),
    val followedIds: Set<String> = emptySet(),
    val state: CopyTradeState = CopyTradeState(0.0, 0.0, emptyList(), 0.0, 0.0),
    val message: String? = null,
)

class CopyTradingViewModel(private val app: MarketRadarApplication) : ViewModel() {
    private val repo = com.arena.marketradar.data.repo.CopyTradingRepository(app)
    private val _state = MutableStateFlow(CopyTradingUiState())
    val state: StateFlow<CopyTradingUiState> = _state

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            // Advance the simulation a bit so P&L feels live.
            val s = repo.tick()
            _state.update {
                CopyTradingUiState(
                    loading = false,
                    roster = repo.roster,
                    followedIds = s.followers.map { it.traderId }.toSet(),
                    state = s,
                )
            }
        }
    }

    fun follow(trader: Trader, amount: Double) {
        repo.follow(trader.id, amount)
        refresh()
    }

    fun unfollow(traderId: String) {
        repo.unfollow(traderId)
        refresh()
    }

    fun reset() {
        repo.reset()
        refresh()
    }
}
