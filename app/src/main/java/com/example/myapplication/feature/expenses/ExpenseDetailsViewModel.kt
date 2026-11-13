package com.example.myapplication.feature.expenses

import androidx.lifecycle.ViewModel
import com.example.myapplication.data.model.Expense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ExpenseDetailsUiState(val expense: Expense? = null)

/** Scaffold ViewModel; feature logic is added when ExpenseDetailsScreen is implemented. */
class ExpenseDetailsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ExpenseDetailsUiState())
    val uiState: StateFlow<ExpenseDetailsUiState> = _uiState.asStateFlow()

    fun loadExpense(expenseId: String) {
        // TODO: Load the expense from ExpenseRepository.
    }

    fun saveExpense(expense: Expense) {
        // TODO: Update the expense through ExpenseRepository.
        _uiState.value = ExpenseDetailsUiState(expense)
    }
}
