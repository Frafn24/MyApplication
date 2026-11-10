package com.example.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.feature.auth.ForgotPasswordScreen
import com.example.myapplication.feature.auth.LoginScreen
import com.example.myapplication.feature.auth.SignUpScreen
import com.example.myapplication.feature.balances.BalanceDirection
import com.example.myapplication.feature.balances.BalanceEntry
import com.example.myapplication.feature.balances.BalancesScreen
import com.example.myapplication.feature.balances.GroupBalances
import com.example.myapplication.feature.groups.GroupSummary
import com.example.myapplication.feature.groups.GroupsScreen
import com.example.myapplication.feature.notifications.NotificationItem
import com.example.myapplication.feature.notifications.NotificationsScreen
import com.example.myapplication.feature.profile.ProfileScreen
import com.example.myapplication.feature.profile.UserProfile
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
                onCreateGroup = { /* TODO */ },
                onProfileClick = { navController.navigate(Routes.PROFILE) },
                onBalancesClick = { navController.navigate(Routes.BALANCES) },
                onAlertsClick = { navController.navigate(Routes.NOTIFICATIONS) }
            )
        }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(
                notifications = listOf(
                    NotificationItem("1", "New Expense", "Alex added Dinner - 600 DKK", "10 min ago"),
                    NotificationItem("2", "Payment Reminder", "You owe John 100 DKK", "Yesterday"),
                    NotificationItem("3", "Group Updated", "Sarah Joined Summer Trip", "2 days ago")
                ),
                onNotificationClick = { /* TODO: åbn notifikation */ },
                onProfileClick = { navController.navigate(Routes.PROFILE) }
            )
        }
        composable(Routes.BALANCES) {
            BalancesScreen(
                totalBalanceText = "+ 150 DKK",
                groups = listOf(
                    GroupBalances(
                        "Summer Trip",
                        listOf(
                            BalanceEntry("1", "Alex", "100 DKK", BalanceDirection.OwedToYou),
                            BalanceEntry("2", "Sarah", "150 DKK", BalanceDirection.OwedToYou)
                        )
                    ),
                    GroupBalances(
                        "Roommates",
                        listOf(BalanceEntry("3", "John", "100 DKK", BalanceDirection.YouOwe))
                    )
                ),
                onRemind = { /* TODO: send påmindelse */ },
                onEntryClick = { /* TODO: åbn detaljer */ },
                onProfileClick = { navController.navigate(Routes.PROFILE) }
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                profile = UserProfile(
                    name = "John Doe",
                    email = "johndoe@test.com",
                    phone = "+45 12 34 56 78",
                    notificationsEnabled = false
                ),
                onSaveChanges = {
                    /* TODO: gem profil */
                    navController.popBackStack()
                }
            )
        }
    }
}
