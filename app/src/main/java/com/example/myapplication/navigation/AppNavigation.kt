package com.example.myapplication.navigation

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.data.remote.AppRepository
import com.example.myapplication.data.remote.runCatchingCancellable
import com.example.myapplication.data.remote.toUserMessage
import com.example.myapplication.feature.auth.ForgotPasswordScreen
import com.example.myapplication.feature.auth.LoginScreen
import com.example.myapplication.feature.auth.SignUpScreen
import com.example.myapplication.feature.balances.BalancesScreen
import com.example.myapplication.feature.expenses.AddExpenseScreen
import com.example.myapplication.feature.expenses.ExpenseDetailsScreen
import com.example.myapplication.feature.expenses.SharedExpensesScreen
import com.example.myapplication.feature.groups.CreateGroupScreen
import com.example.myapplication.feature.groups.GroupDetailsScreen
import com.example.myapplication.feature.groups.GroupsScreen
import com.example.myapplication.feature.groups.ManageMembersScreen
import com.example.myapplication.feature.notifications.NotificationsScreen
import com.example.myapplication.feature.profile.ProfileScreen
import com.example.myapplication.feature.splash.SplashScreen
import com.example.myapplication.notifications.rememberNotificationPermission
import com.example.myapplication.ui.components.StatusMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// isImeVisible: hide the bottom bar while the keyboard is open.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val showBottomBar = currentRoute != null && currentRoute !in SignedOutRoutes && !WindowInsets.isImeVisible
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    // App start: once the splash screen is done, ask "Allow notifications?" (first start only).
    val notificationPermission = rememberNotificationPermission()
    val splashDone = currentRoute != null && currentRoute != Routes.Splash
    LaunchedEffect(splashDone) {
        if (splashDone) notificationPermission.requestOnce()
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    selected = BottomTab.forRoute(currentRoute),
                    onTabClick = { tab -> navController.openTab(tab.route) }
                )
            }
        }
    ) { innerPadding ->
        EdgeSwipeBack(
            // Only where there is a screen to go back to, so a swipe never closes the app.
            enabled = navController.previousBackStackEntry != null && currentRoute != Routes.Splash,
            // Same as the phone's back button.
            onBack = { backDispatcher?.onBackPressed() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            AppNavHost(navController)
        }
    }
}

/** savedStateHandle key for a message shown on the Login screen. */
private const val LOGIN_NOTICE = "login_notice"

/** Screens shown before sign-in: no bottom bar. */
private val SignedOutRoutes = setOf(Routes.Splash, Routes.Login, Routes.SignUp, Routes.ForgotPassword)

/**
 * Opens a bottom bar tab. Groups is always the first screen after sign-in, so the
 * back stack becomes Groups -> tab, and the phone's back button returns to Groups.
 */
private fun NavHostController.openTab(route: String) {
    navigate(route) {
        popUpTo(Routes.Groups)
        launchSingleTop = true
    }
}

@Composable
private fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.Splash
    ) {
        composable(Routes.Splash) {
            SplashScreen(
                checkSignedIn = AppRepository::isSignedIn,
                onFinished = { isSignedIn ->
                    // Not a tap, and must also happen if the app was in the background meanwhile.
                    navController.navigate(if (isSignedIn) Routes.Groups else Routes.Login) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Login) { entry ->
            val request = rememberRequest()
            // Set by Forgot Password after the reset email was sent.
            val notice by entry.savedStateHandle.getStateFlow<String?>(LOGIN_NOTICE, null).collectAsState()
            LoginScreen(
                onSignIn = { email, password ->
                    request.run {
                        AppRepository.signIn(email, password)
                        navController.navigateToGroups(entry)
                    }
                },
                onForgotPassword = { navController.navigateFrom(entry, Routes.ForgotPassword) },
                onSignUp = { navController.navigateFrom(entry, Routes.SignUp) },
                isLoading = request.isLoading,
                message = request.message ?: notice
            )
        }
        composable(Routes.SignUp) { entry ->
            val request = rememberRequest()
            SignUpScreen(
                onSignUp = { name, email, password ->
                    request.run {
                        if (AppRepository.signUp(name, email, password)) {
                            navController.navigateToGroups(entry)
                        } else {
                            request.message = "Check your email to confirm your account, then sign in."
                        }
                    }
                },
                onSignIn = { navController.popFrom(entry) },
                isLoading = request.isLoading,
                message = request.message
            )
        }
        composable(Routes.ForgotPassword) { entry ->
            val request = rememberRequest()
            ForgotPasswordScreen(
                onBack = { navController.popFrom(entry) },
                onSendEmail = { email ->
                    request.run {
                        AppRepository.sendPasswordReset(email)
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            LOGIN_NOTICE,
                            "We sent you an email with a link to reset your password."
                        )
                        navController.popFrom(entry)
                    }
                },
                isLoading = request.isLoading,
                message = request.message
            )
        }
        composable(Routes.Groups) { entry ->
            val groups by loadFromDatabase { AppRepository.getGroups() }
            GroupsScreen(
                groups = groups.data.orEmpty(),
                onGroupClick = { navController.navigateFrom(entry, Routes.groupDetails(it.id)) },
                onCreateGroup = { navController.navigateFrom(entry, Routes.CreateGroup) },
                onProfileClick = { navController.navigateFrom(entry, Routes.Profile) },
                message = groups.error
            )
        }
        composable(Routes.CreateGroup) { entry ->
            val request = rememberRequest()
            CreateGroupScreen(
                onBack = { navController.popFrom(entry) },
                onProfileClick = { navController.navigateFrom(entry, Routes.Profile) },
                onCreateGroup = { title, description, members ->
                    request.run {
                        AppRepository.createGroup(title, description, members)
                        // Back to Groups (as in the navigation model), which shows the new group.
                        navController.popFrom(entry)
                    }
                },
                isLoading = request.isLoading,
                message = request.message
            )
        }
        composable(Routes.GroupDetails) { entry ->
            val groupId = entry.requireArg(Routes.GROUP_ID)
            val group by loadFromDatabase { AppRepository.getGroupDetails(groupId) }
            LoadedScreen(group) { loaded ->
                GroupDetailsScreen(
                    group = loaded,
                    onBack = { navController.popFrom(entry) },
                    onExpenseClick = { navController.navigateFrom(entry, Routes.expenseDetails(it.id)) },
                    onAddExpense = { navController.navigateFrom(entry, Routes.addExpense(groupId)) },
                    onManageMembers = { navController.navigateFrom(entry, Routes.manageMembers(groupId)) }
                )
            }
        }
        composable(Routes.ManageMembers) { entry ->
            val groupId = entry.requireArg(Routes.GROUP_ID)
            var reload by remember { mutableIntStateOf(0) }
            val members by loadFromDatabase(reload) { AppRepository.getGroupMembers(groupId) }
            val request = rememberRequest()
            LoadedScreen(members) { loaded ->
                ManageMembersScreen(
                    members = loaded,
                    onBack = { navController.popFrom(entry) },
                    onProfileClick = { navController.navigateFrom(entry, Routes.Profile) },
                    onAddMember = { email ->
                        request.run {
                            AppRepository.addGroupMember(groupId, email)
                            reload++
                        }
                    },
                    onRemoveMember = { member ->
                        request.run {
                            AppRepository.removeGroupMember(groupId, member.id)
                            reload++
                        }
                    },
                    isLoading = request.isLoading,
                    message = request.message ?: members.error
                )
            }
        }
        composable(Routes.AddExpense) { entry ->
            val groupId = entry.requireArg(Routes.GROUP_ID)
            val members by loadFromDatabase { AppRepository.getExpenseMembers(groupId) }
            val request = rememberRequest()
            LoadedScreen(members) { loaded ->
                AddExpenseScreen(
                    members = loaded,
                    // getExpenseMembers puts the signed-in user first.
                    defaultPayerId = loaded.firstOrNull()?.id,
                    onBack = { navController.popFrom(entry) },
                    onAddExpense = { draft ->
                        request.run {
                            AppRepository.createExpense(groupId, draft)
                            navController.popFrom(entry)
                        }
                    },
                    isLoading = request.isLoading,
                    message = request.message
                )
            }
        }
        composable(Routes.ExpenseDetails) { entry ->
            val expenseId = entry.requireArg(Routes.EXPENSE_ID)
            val expense by loadFromDatabase { AppRepository.getExpenseDetails(expenseId) }
            val delete = rememberRequest()
            LoadedScreen(expense) { loaded ->
                ExpenseDetailsScreen(
                    expense = loaded,
                    onBack = { navController.popFrom(entry) },
                    onEditExpense = { navController.navigateFrom(entry, Routes.editExpense(expenseId)) },
                    onDeleteExpense = {
                        delete.run {
                            AppRepository.deleteExpense(expenseId)
                            // Back to where the expense was opened from, which reloads without it.
                            navController.popFrom(entry)
                        }
                    },
                    isLoading = delete.isLoading,
                    message = delete.message
                )
            }
        }
        composable(Routes.EditExpense) { entry ->
            val expenseId = entry.requireArg(Routes.EXPENSE_ID)
            val editable by loadFromDatabase {
                val (groupId, draft) = AppRepository.getExpenseDraft(expenseId)
                val members = AppRepository.getExpenseMembers(groupId)
                // People who have left the group can no longer be in the split.
                val memberIds = members.map { it.id }.toSet()
                members to draft.copy(splitBetween = draft.splitBetween.filter { it in memberIds })
            }
            val request = rememberRequest()
            LoadedScreen(editable) { (members, draft) ->
                AddExpenseScreen(
                    members = members,
                    initial = draft,
                    screenTitle = "Edit Expense",
                    buttonText = "Save Changes",
                    onBack = { navController.popFrom(entry) },
                    onAddExpense = { updated ->
                        request.run {
                            AppRepository.updateExpense(expenseId, updated)
                            navController.popFrom(entry)
                        }
                    },
                    isLoading = request.isLoading,
                    message = request.message
                )
            }
        }
        composable(Routes.SharedExpenses) { entry ->
            val groupId = entry.requireArg(Routes.GROUP_ID)
            val userId = entry.requireArg(Routes.USER_ID)
            val shared by loadFromDatabase { AppRepository.getSharedExpenses(groupId, userId) }
            LoadedScreen(shared) { loaded ->
                SharedExpensesScreen(
                    shared = loaded,
                    onBack = { navController.popFrom(entry) },
                    onExpenseClick = { navController.navigateFrom(entry, Routes.expenseDetails(it.id)) }
                )
            }
        }
        composable(Routes.Notifications) { entry ->
            var reload by remember { mutableIntStateOf(0) }
            val notifications by loadFromDatabase(reload) { AppRepository.getNotifications() }
            val open = rememberRequest()
            NotificationsScreen(
                notifications = notifications.data.orEmpty(),
                onNotificationClick = { notification ->
                    open.run {
                        if (notification.isUnread) AppRepository.markNotificationRead(notification.id)
                        when {
                            notification.expenseId != null ->
                                navController.navigateFrom(entry, Routes.expenseDetails(notification.expenseId))
                            notification.groupId != null ->
                                navController.navigateFrom(entry, Routes.groupDetails(notification.groupId))
                            else -> reload++
                        }
                    }
                },
                onProfileClick = { navController.navigateFrom(entry, Routes.Profile) },
                message = notifications.error ?: open.message
            )
        }
        composable(Routes.Balances) { entry ->
            val balances by loadFromDatabase { AppRepository.getBalances() }
            val remind = rememberRequest()
            BalancesScreen(
                totalBalanceText = balances.data?.first.orEmpty(),
                groups = balances.data?.second.orEmpty(),
                onRemind = { balance ->
                    remind.run {
                        AppRepository.sendPaymentReminder(balance)
                        remind.message = "Reminder sent to ${balance.personName}"
                    }
                },
                // "View details". BalanceEntry.id is "<group id>/<user id>".
                onEntryClick = { balance ->
                    val (groupId, userId) = balance.id.split("/")
                    navController.navigateFrom(entry, Routes.sharedExpenses(groupId, userId))
                },
                onProfileClick = { navController.navigateFrom(entry, Routes.Profile) },
                message = balances.error ?: remind.message
            )
        }
        composable(Routes.Profile) { entry ->
            val profile by loadFromDatabase { AppRepository.getProfile() }
            val save = rememberRequest()
            val notificationPermission = rememberNotificationPermission()
            LoadedScreen(profile) { loaded ->
                ProfileScreen(
                    profile = loaded,
                    onSaveChanges = { updated ->
                        save.run {
                            AppRepository.saveProfile(loaded, updated)
                            navController.popFrom(entry)
                        }
                    },
                    onSignOut = {
                        save.run {
                            AppRepository.signOut()
                            navController.navigateFrom(entry, Routes.Login) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                    },
                    systemNotificationsAllowed = notificationPermission.isAllowed,
                    onAllowSystemNotifications = notificationPermission::request,
                    isLoading = save.isLoading,
                    message = save.message
                )
            }
        }
    }
}

/*
 * Navigation only happens while the screen that asked for it is the active one.
 * This ignores a second tap on the same button (which would open a screen twice
 * or close two screens), and a save that finishes after the user already left.
 */
private fun NavBackStackEntry.isActive() = lifecycle.currentState == Lifecycle.State.RESUMED

private fun NavHostController.navigateFrom(
    entry: NavBackStackEntry,
    route: String,
    builder: NavOptionsBuilder.() -> Unit = {}
) {
    if (entry.isActive()) navigate(route, builder)
}

private fun NavHostController.popFrom(entry: NavBackStackEntry) {
    if (entry.isActive()) popBackStack()
}

private fun NavHostController.navigateToGroups(entry: NavBackStackEntry) {
    navigateFrom(entry, Routes.Groups) {
        popUpTo(graph.id) { inclusive = true }
    }
}

private fun NavBackStackEntry.requireArg(name: String): String =
    checkNotNull(arguments?.getString(name)) { "Missing navigation argument $name" }

/** Data loaded from the database when a screen is shown, or the error message if it failed. */
private data class Loaded<T>(val data: T? = null, val error: String? = null)

/**
 * Loads when the screen is shown and again whenever [reloadKey] changes.
 * While reloading, and if reloading fails, the previous data stays on screen.
 */
@Composable
private fun <T> loadFromDatabase(reloadKey: Any? = Unit, load: suspend () -> T): State<Loaded<T>> =
    produceState(Loaded(), reloadKey) {
        value = runCatchingCancellable { load() }.fold(
            onSuccess = { Loaded(data = it) },
            onFailure = { Loaded(data = value.data, error = it.toUserMessage()) }
        )
    }

/** Shows a spinner until [loaded] has data, the error if loading failed, and then [content]. */
@Composable
private fun <T> LoadedScreen(loaded: Loaded<T>, content: @Composable (T) -> Unit) {
    val data = loaded.data
    if (data == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            loaded.error?.let { StatusMessage(it) } ?: CircularProgressIndicator()
        }
    } else {
        content(data)
    }
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
