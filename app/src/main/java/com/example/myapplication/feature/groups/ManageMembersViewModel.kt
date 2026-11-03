package com.example.myapplication.feature.groups

import androidx.lifecycle.ViewModel
import com.example.myapplication.data.model.GroupMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ManageMembersUiState(
    val members: List<GroupMember> = emptyList(),
)

/** Scaffold ViewModel; feature logic is added when ManageMembersScreen is implemented. */
class ManageMembersViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ManageMembersUiState())
    val uiState: StateFlow<ManageMembersUiState> = _uiState.asStateFlow()

    fun addMember(name: String) {
        if (name.isNotBlank()) {
            val member = GroupMember("new-member", name)
            _uiState.value = _uiState.value.copy(members = _uiState.value.members + member)
        }
    }
}
