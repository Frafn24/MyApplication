package com.example.myapplication.feature.balances

import androidx.lifecycle.ViewModel
import com.example.myapplication.data.model.Balance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BalancesUiState(
    val balances: List<Balance> = emptyList(),
)

class BalancesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BalancesUiState())
    val uiState: StateFlow<BalancesUiState> = _uiState.asStateFlow()

    fun loadBalances() {
        // TODO: Load balances from BalanceRepository.
    }
}
