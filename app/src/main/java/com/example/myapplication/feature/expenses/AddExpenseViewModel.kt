package com.example.myapplication.feature.expenses

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AddExpenseUiState(
    val title: String = "",
    val amount: String = "",
    val paidBy: String = "You",
    val selectedMembers: Set<String> = emptySet(),
)

class AddExpenseViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    fun updateTitle(value: String) { _uiState.value = _uiState.value.copy(title = value) }
    fun updateAmount(value: String) { _uiState.value = _uiState.value.copy(amount = value) }
    fun updatePaidBy(value: String) { _uiState.value = _uiState.value.copy(paidBy = value) }

    fun toggleMember(member: String) {
        val selected = _uiState.value.selectedMembers.toMutableSet()
        if (!selected.add(member)) selected.remove(member)
        _uiState.value = _uiState.value.copy(selectedMembers = selected)
    }

    fun addExpense() {
        // TODO: Validate and save through ExpenseRepository.
    }
}
