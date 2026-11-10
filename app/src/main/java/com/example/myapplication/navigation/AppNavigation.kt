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
import com.example.myapplication.feature.splash.SplashScreen
// behold dine eksisterende imports til LoginScreen, SignUpScreen og ForgotPasswordScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Splash
    ) {
        composable(Routes.Splash) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Routes.Login) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                }
            )
        }
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
                    GroupSummary("2", "Roommates", 3, "You owe 100 DKK")
                ),
                onGroupClick = { /* TODO */ },
                onCreateGroup = { /* TODO */ },
                onProfileClick = { navController.navigate(Routes.Profile) },
                onBalancesClick = { navController.navigate(Routes.Balances) },
                onAlertsClick = { navController.navigate(Routes.Notifications) }
            )
        }
        composable(Routes.Notifications) {
            NotificationsScreen(
                notifications = listOf(
                    NotificationItem("1", "New Expense", "Alex added Dinner - 600 DKK", "10 min ago"),
                    NotificationItem("2", "Payment Reminder", "You owe John 100 DKK", "Yesterday"),
                    NotificationItem("3", "Group Updated", "Sarah Joined Summer Trip", "2 days ago")
                ),
                onNotificationClick = { /* TODO: åbn notifikation */ },
                onProfileClick = { navController.navigate(Routes.Profile) }
            )
        }
        composable(Routes.Balances) {
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
                onProfileClick = { navController.navigate(Routes.Profile) }
            )
        }
        composable(Routes.Profile) {
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
