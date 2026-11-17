package com.example.myapplication.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccessful: Boolean = false
)

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateEmail(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun updatePassword(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun login() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Email and password are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            _uiState.value = repository.login(state.email, state.password).fold(
                onSuccess = { state.copy(isLoading = false, loginSuccessful = true) },
                onFailure = { state.copy(isLoading = false, errorMessage = it.message) }
            )
        }
    }
}

data class SignUpUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val acceptTerms: Boolean = false,
    val acceptNews: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val signUpSuccessful: Boolean = false
) {
    val canSubmit: Boolean
        get() = name.isNotBlank() &&
            email.contains("@") &&
            password.length >= 6 &&
            password == confirmPassword &&
            acceptTerms
}

class SignUpViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun updateName(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }
    fun updateEmail(value: String) {
        _uiState.value = _uiState.value.copy(email = value)
    }
    fun updatePassword(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }
    fun updateConfirmPassword(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value)
    }
    fun updateAcceptTerms(value: Boolean) {
        _uiState.value = _uiState.value.copy(acceptTerms = value)
    }
    fun updateAcceptNews(value: Boolean) {
        _uiState.value = _uiState.value.copy(acceptNews = value)
    }

    fun signUp() {
        val state = _uiState.value
        if (!state.canSubmit) {
            _uiState.value = state.copy(errorMessage = "Please complete all required fields")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            _uiState.value = repository.register(state.name, state.email, state.password).fold(
                onSuccess = { state.copy(isLoading = false, signUpSuccessful = true) },
                onFailure = { state.copy(isLoading = false, errorMessage = it.message) }
            )
        }
    }
}

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val emailSent: Boolean = false
)

class ForgotPasswordViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun updateEmail(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun sendPasswordReset() {
        val state = _uiState.value
        if (!state.email.contains("@")) {
            _uiState.value = state.copy(errorMessage = "Enter a valid email address")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true)
            _uiState.value = repository.sendPasswordReset(state.email).fold(
                onSuccess = { state.copy(isLoading = false, emailSent = true) },
                onFailure = { state.copy(isLoading = false, errorMessage = it.message) }
            )
        }
    }
}
