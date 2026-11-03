package com.example.myapplication.feature.groups

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CreateGroupUiState(
    val title: String = "",
    val description: String = "",
    val memberNames: List<String> = emptyList(),
)

/** Scaffold ViewModel; feature logic is added when CreateGroupScreen is implemented. */
class CreateGroupViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    fun updateTitle(value: String) { _uiState.value = _uiState.value.copy(title = value) }
    fun updateDescription(value: String) { _uiState.value = _uiState.value.copy(description = value) }

    fun addMember(name: String) {
        if (name.isNotBlank()) {
            _uiState.value = _uiState.value.copy(memberNames = _uiState.value.memberNames + name)
        }
    }

    fun createGroup() {
        // TODO: Send group data to GroupRepository when the screen is connected.
    }
}
