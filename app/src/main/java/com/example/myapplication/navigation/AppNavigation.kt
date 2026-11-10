package com.example.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.feature.auth.ForgotPasswordScreen
import com.example.myapplication.feature.auth.LoginScreen
import com.example.myapplication.feature.auth.SignUpScreen
import com.example.myapplication.feature.groups.GroupSummary
import com.example.myapplication.feature.groups.GroupsScreen
// behold dine eksisterende imports til LoginScreen, SignUpScreen og ForgotPasswordScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onSignIn = {
                    navController.navigate(Routes.GROUPS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onSignUp = { navController.navigate(Routes.SIGN_UP) }
            )
        }
        composable(Routes.SIGN_UP) {
            SignUpScreen(
                onSignUp = {
                    navController.navigate(Routes.GROUPS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onSignIn = { navController.popBackStack() }
            )
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() },
                onSendEmail = { navController.popBackStack() }
            )
        }
        composable(Routes.GROUPS) {
            GroupsScreen(
                groups = listOf(
                    GroupSummary("1", "Summer Trip", 4, "You are owed 250 DKK"),
                    GroupSummary("2", "Roommates", 3, "You owe 100 DKK")
                ),
                onGroupClick = { /* TODO */ },
                onCreateGroup = { /* TODO */ }
            )
        }
    }
}
