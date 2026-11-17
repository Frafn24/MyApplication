package com.example.myapplication.feature.groups

import androidx.lifecycle.ViewModel
import com.example.myapplication.data.model.Expense
import com.example.myapplication.data.model.GroupMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GroupDetailsUiState(
    val groupName: String = "",
    val members: List<GroupMember> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val balanceText: String = ""
)

/** Scaffold ViewModel; feature logic is added when GroupDetailsScreen is implemented. */
class GroupDetailsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GroupDetailsUiState())
    val uiState: StateFlow<GroupDetailsUiState> = _uiState.asStateFlow()

    fun loadGroup(groupId: String) {
        // TODO: Load the group, members and expenses from GroupRepository.
    }
}
