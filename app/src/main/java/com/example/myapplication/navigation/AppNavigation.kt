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
        startDestination = Routes.Login
    ) {
        composable(Routes.Login) {
            LoginScreen(
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
            SignUpScreen(
                onSignUp = {
                    navController.navigate(Routes.Groups) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                },
                onSignIn = { navController.popBackStack() }
            )
        }
        composable(Routes.ForgotPassword) {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() },
                onSendEmail = { navController.popBackStack() }
            )
        }
        composable(Routes.Groups) {
            GroupsScreen(
                groups = listOf(
                    GroupSummary("1", "Summer Trip", 4, "You are owed 250 DKK"),
                    GroupSummary("2", "Roommates", 3, "You owe 100 DKK"),
                ),
                onGroupClick = { /* TODO */ },
                onCreateGroup = { /* TODO */ }
            )
        }
    }
}
