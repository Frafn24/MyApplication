package com.example.myapplication.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.data.remote.AppRepository
import com.example.myapplication.data.remote.runCatchingCancellable
import com.example.myapplication.data.remote.toUserMessage
import com.example.myapplication.feature.auth.ForgotPasswordScreen
import com.example.myapplication.feature.auth.LoginScreen
import com.example.myapplication.feature.auth.SignUpScreen
import com.example.myapplication.feature.balances.BalancesScreen
import com.example.myapplication.feature.groups.GroupsScreen
import com.example.myapplication.feature.notifications.NotificationsScreen
import com.example.myapplication.feature.profile.ProfileScreen
import com.example.myapplication.feature.splash.SplashScreen
import com.example.myapplication.ui.components.StatusMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Splash
    ) {
        composable(Routes.Splash) {
            SplashScreen(
                checkSignedIn = AppRepository::isSignedIn,
                onFinished = { isSignedIn ->
                    navController.navigate(if (isSignedIn) Routes.Groups else Routes.Login) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Login) {
            val request = rememberRequest()
            LoginScreen(
                onSignIn = { email, password ->
                    request.run {
                        AppRepository.signIn(email, password)
                        navController.navigateToGroups()
                    }
                },
                onForgotPassword = { navController.navigate(Routes.ForgotPassword) },
                onSignUp = { navController.navigate(Routes.SignUp) },
                isLoading = request.isLoading,
                message = request.message
            )
        }
        composable(Routes.SignUp) {
            val request = rememberRequest()
            SignUpScreen(
                onSignUp = { name, email, password ->
                    request.run {
                        if (AppRepository.signUp(name, email, password)) {
                            navController.navigateToGroups()
                        } else {
                            request.message = "Check your email to confirm your account, then sign in."
                        }
                    }
                },
                onSignIn = { navController.popBackStack() },
                isLoading = request.isLoading,
                message = request.message
            )
        }
        composable(Routes.ForgotPassword) {
            val request = rememberRequest()
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() },
                onSendEmail = { email ->
                    request.run {
                        AppRepository.sendPasswordReset(email)
                        navController.popBackStack()
                    }
                },
                isLoading = request.isLoading,
                message = request.message
            )
        }
        composable(Routes.Groups) {
            val groups by loadFromDatabase { AppRepository.getGroups() }
            GroupsScreen(
                groups = groups.data.orEmpty(),
                onGroupClick = { /* TODO */ },
                onCreateGroup = { /* TODO */ },
                onProfileClick = { navController.navigate(Routes.Profile) },
                onBalancesClick = { navController.navigate(Routes.Balances) },
                onAlertsClick = { navController.navigate(Routes.Notifications) },
                message = groups.error
            )
        }
        composable(Routes.Notifications) {
            val notifications by loadFromDatabase { AppRepository.getNotifications() }
            NotificationsScreen(
                notifications = notifications.data.orEmpty(),
                onNotificationClick = { /* TODO: åbn notifikation */ },
                onProfileClick = { navController.navigate(Routes.Profile) },
                message = notifications.error
            )
        }
        composable(Routes.Balances) {
            val balances by loadFromDatabase { AppRepository.getBalances() }
            val remind = rememberRequest()
            BalancesScreen(
                totalBalanceText = balances.data?.first.orEmpty(),
                groups = balances.data?.second.orEmpty(),
                onRemind = { entry ->
                    remind.run {
                        AppRepository.sendPaymentReminder(entry)
                        remind.message = "Reminder sent to ${entry.personName}"
                    }
                },
                onEntryClick = { /* TODO: åbn detaljer */ },
                onProfileClick = { navController.navigate(Routes.Profile) },
                message = balances.error ?: remind.message
            )
        }
        composable(Routes.Profile) {
            val profile by loadFromDatabase { AppRepository.getProfile() }
            val save = rememberRequest()
            val loaded = profile.data
            if (loaded == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    profile.error?.let { StatusMessage(it) } ?: CircularProgressIndicator()
                }
            } else {
                ProfileScreen(
                    profile = loaded,
                    onSaveChanges = { updated ->
                        save.run {
                            AppRepository.saveProfile(loaded, updated)
                            navController.popBackStack()
                        }
                    },
                    message = save.message
                )
            }
        }
    }
}

private fun NavHostController.navigateToGroups() {
    navigate(Routes.Groups) {
        popUpTo(graph.id) { inclusive = true }
    }
}

/** Data loaded from the database when a screen is shown, or the error message if it failed. */
private data class Loaded<T>(val data: T? = null, val error: String? = null)

@Composable
private fun <T> loadFromDatabase(load: suspend () -> T): State<Loaded<T>> = produceState(Loaded()) {
    value = runCatchingCancellable { load() }.fold(
        onSuccess = { Loaded(data = it) },
        onFailure = { Loaded(error = it.toUserMessage()) }
    )
}

/** Runs one database/auth request at a time for a button and keeps its loading and error state. */
private class Request(private val scope: CoroutineScope) {
    var isLoading by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    fun run(block: suspend () -> Unit) {
        if (isLoading) return
        isLoading = true
        message = null
        scope.launch {
            runCatchingCancellable { block() }.onFailure { message = it.toUserMessage() }
            isLoading = false
        }
    }
}

@Composable
private fun rememberRequest(): Request {
    val scope = rememberCoroutineScope()
    return remember { Request(scope) }
}
