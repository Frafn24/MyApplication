package com.example.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.data.AppContainer
import com.example.myapplication.data.AppViewModelFactory
import com.example.myapplication.feature.auth.ForgotPasswordScreen
import com.example.myapplication.feature.auth.ForgotPasswordViewModel
import com.example.myapplication.feature.auth.LoginScreen
import com.example.myapplication.feature.auth.LoginViewModel
import com.example.myapplication.feature.auth.SignUpScreen
import com.example.myapplication.feature.auth.SignUpViewModel
import com.example.myapplication.feature.groups.GroupsScreen
import com.example.myapplication.feature.groups.GroupsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AppNavigation(container: AppContainer) {
    val navController = rememberNavController()
    val factory = remember { AppViewModelFactory(container) }

    NavHost(
        navController = navController,
        startDestination = Routes.Login
    ) {
        composable(Routes.Login) {
            val loginViewModel: LoginViewModel = viewModel(factory = factory)
            val uiState by loginViewModel.uiState.collectAsState()
            LoginScreen(
                uiState = uiState,
                onEmailChanged = loginViewModel::updateEmail,
                onPasswordChanged = loginViewModel::updatePassword,
                onLogin = loginViewModel::login,
                onSignIn = {
                    navController.navigate(Routes.Groups) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                },
                onForgotPassword = { navController.navigate(Routes.ForgotPassword) },
                onSignUp = { navController.navigate(Routes.SignUp) }
            )
        }
        composable(Routes.SignUp) {
            val signUpViewModel: SignUpViewModel = viewModel(factory = factory)
            val uiState by signUpViewModel.uiState.collectAsState()
            SignUpScreen(
                uiState = uiState,
                onNameChanged = signUpViewModel::updateName,
                onEmailChanged = signUpViewModel::updateEmail,
                onPasswordChanged = signUpViewModel::updatePassword,
                onConfirmPasswordChanged = signUpViewModel::updateConfirmPassword,
                onTermsChanged = signUpViewModel::updateAcceptTerms,
                onNewsChanged = signUpViewModel::updateAcceptNews,
                onSignUpSubmit = signUpViewModel::signUp,
                onSignUp = {
                    navController.navigate(Routes.Groups) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                },
                onSignIn = { navController.popBackStack() }
            )
        }
        composable(Routes.ForgotPassword) {
            val forgotPasswordViewModel: ForgotPasswordViewModel = viewModel(factory = factory)
            val uiState by forgotPasswordViewModel.uiState.collectAsState()
            ForgotPasswordScreen(
                uiState = uiState,
                onEmailChanged = forgotPasswordViewModel::updateEmail,
                onBack = { navController.popBackStack() },
                onSendEmail = forgotPasswordViewModel::sendPasswordReset,
                onEmailSent = { navController.popBackStack() },
            )
        }
        composable(Routes.Groups) {
            val groupsViewModel: GroupsViewModel = viewModel(factory = factory)
            val uiState by groupsViewModel.uiState.collectAsState()
            GroupsScreen(
                groups = uiState.groups,
                onGroupClick = { /* TODO */ },
                onCreateGroup = { /* TODO */ }
            )
        }
    }
}
