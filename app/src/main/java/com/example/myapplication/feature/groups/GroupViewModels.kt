package com.example.myapplication.feature.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.GroupSummary
import com.example.myapplication.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GroupsUiState(
    val groups: List<GroupSummary> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class GroupsViewModel(private val groupRepository: GroupRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()

    init {
        loadGroups()
    }

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = groupRepository.getGroups()
            _uiState.value = result.fold(
                onSuccess = { GroupsUiState(groups = it) },
                onFailure = { GroupsUiState(errorMessage = it.message) }
            )
        }
    }
}
