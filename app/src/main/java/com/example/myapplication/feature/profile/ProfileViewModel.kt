package com.example.myapplication.feature.profile

import androidx.lifecycle.ViewModel
import com.example.myapplication.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileUiState(
    val profile: User? = null,
)

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        // TODO: Load profile from ProfileRepository.
    }

    fun saveProfile(profile: User) {
        // TODO: Save profile through ProfileRepository.
        _uiState.value = ProfileUiState(profile)
    }
}
