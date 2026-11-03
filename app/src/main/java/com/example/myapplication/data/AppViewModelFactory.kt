package com.example.myapplication.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.feature.auth.ForgotPasswordViewModel
import com.example.myapplication.feature.auth.LoginViewModel
import com.example.myapplication.feature.auth.SignUpViewModel
import com.example.myapplication.feature.groups.GroupsViewModel

class AppViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(LoginViewModel::class.java) -> LoginViewModel(container.authRepository)
        modelClass.isAssignableFrom(SignUpViewModel::class.java) -> SignUpViewModel(container.authRepository)
        modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java) -> ForgotPasswordViewModel(container.authRepository)
        modelClass.isAssignableFrom(GroupsViewModel::class.java) -> GroupsViewModel(container.groupRepository)
        else -> error("Unknown ViewModel class: ${modelClass.name}")
    } as T
}
