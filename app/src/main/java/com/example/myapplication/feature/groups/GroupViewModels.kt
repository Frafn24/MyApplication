package com.example.myapplication.feature.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.GroupSummary
import com.example.myapplication.data.repository.GroupRepository
import com.example.myapplication.data.sync.SyncCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GroupsUiState(
    val groups: List<GroupSummary> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class GroupsViewModel(
    private val groupRepository: GroupRepository,
    private val syncCoordinator: SyncCoordinator? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            // Local data first
            val localResult = groupRepository.getGroups()
            localResult.onSuccess {
                _uiState.value = GroupsUiState(groups = it)
            }

            // Try fetching from Supabase
            syncCoordinator?.syncGroups()

            // Read updated data from Room
            val refreshedResult = groupRepository.getGroups()
            _uiState.value = refreshedResult.fold(
                onSuccess = { GroupsUiState(groups = it) },
                onFailure = {
                    GroupsUiState(errorMessage = it.message)
                }
            )
        }
    }
}
