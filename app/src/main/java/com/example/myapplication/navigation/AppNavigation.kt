package com.example.myapplication.navigation

import android.content.Context
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.data.AppContainer
import com.example.myapplication.data.AppViewModelFactory
import com.example.myapplication.data.model.AppNotification
import com.example.myapplication.data.model.BalanceDirection
import com.example.myapplication.data.model.BalanceEntry
import com.example.myapplication.data.model.BalancesSummary
import com.example.myapplication.data.model.Expense
import com.example.myapplication.data.model.User
import com.example.myapplication.data.network.NetworkMonitor
import com.example.myapplication.data.remote.AppRepository
import com.example.myapplication.data.remote.runCatchingCancellable
import com.example.myapplication.data.remote.toUserMessage
import com.example.myapplication.feature.auth.ForgotPasswordScreen
import com.example.myapplication.feature.auth.ForgotPasswordViewModel
import com.example.myapplication.feature.auth.LoginScreen
import com.example.myapplication.feature.auth.LoginViewModel
import com.example.myapplication.feature.auth.SignUpScreen
import com.example.myapplication.feature.auth.SignUpViewModel
import com.example.myapplication.feature.balances.BalanceDirection as FeatureBalanceDirection
import com.example.myapplication.feature.balances.BalanceEntry as FeatureBalanceEntry
import com.example.myapplication.feature.balances.BalancesScreen
import com.example.myapplication.feature.balances.GroupBalances as FeatureGroupBalances
import com.example.myapplication.feature.expenses.AddExpenseScreen
import com.example.myapplication.feature.expenses.ExpenseDetailsScreen
import com.example.myapplication.feature.expenses.ExpenseMember
import com.example.myapplication.feature.expenses.SharedExpenses
import com.example.myapplication.feature.expenses.SharedExpensesScreen
import com.example.myapplication.feature.groups.CreateGroupScreen
import com.example.myapplication.feature.groups.GroupDetails
import com.example.myapplication.feature.groups.GroupDetailsScreen
import com.example.myapplication.feature.groups.GroupMember
import com.example.myapplication.feature.groups.GroupsScreen
import com.example.myapplication.feature.groups.GroupsViewModel
import com.example.myapplication.feature.groups.ManageMembersScreen
import com.example.myapplication.feature.notifications.NotificationItem
import com.example.myapplication.feature.notifications.NotificationsScreen
import com.example.myapplication.feature.profile.ProfileScreen
import com.example.myapplication.feature.profile.UserProfile
import com.example.myapplication.feature.splash.SplashScreen
import com.example.myapplication.notifications.SystemNotifier
import com.example.myapplication.notifications.rememberNotificationPermission
import com.example.myapplication.ui.components.OfflineBanner
import com.example.myapplication.ui.components.StatusMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// isImeVisible: hide the bottom bar while the keyboard is open.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppNavigation(container: AppContainer) {
    val navController = rememberNavController()
    val factory = remember { AppViewModelFactory(container) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val networkMonitor = remember(context) { NetworkMonitor(context) }
    val isOnline by networkMonitor.isOnline.collectAsStateWithLifecycle(initialValue = true)

    // The Alerts badge; updates by itself when rows are marked read or pulled.
    val unreadAlerts by container.notificationRepository
        .observeUnreadCount()
        .collectAsStateWithLifecycle(initialValue = 0)

    // App start and reconnect: pull the news in the background so the badge
    // and system notifications appear without opening each screen.
    LaunchedEffect(isOnline) {
        if (isOnline && container.authRepository.currentUserIdOrNull() != null) {
            pullNews(container, context)
        }
    }

    // Coming back to the foreground does the same, so another phone's changes
    // show up just by picking the phone up again.
    LifecycleResumeEffect(Unit) {
        if (isOnline && container.authRepository.currentUserIdOrNull() != null) {
            scope.launch { pullNews(container, context) }
        }
        onPauseOrDispose { }
    }

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
                    onTabClick = { tab -> navController.openTab(tab.route) },
                    unreadAlerts = unreadAlerts
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
            Column(modifier = Modifier.fillMaxSize()) {
                OfflineBanner(visible = !isOnline)

                Box(
                    modifier = Modifier.fillMaxSize().weight(1f)
                ) {
                    AppNavHost(navController, factory, container, isOnline)
                }
            }
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

/**
 * Best-effort pull of everything that can carry news from other users, so the
 * Alerts badge and system notifications appear without opening each screen.
 */
private suspend fun pullNews(container: AppContainer, context: Context) {
    val coordinator = container.syncCoordinator ?: return
    runCatching { coordinator.syncGroups() }
    runCatching { coordinator.syncBalances() }
    runCatching { coordinator.syncProfile() }
    runCatching {
        coordinator.syncNotifications().getOrNull()
            ?.let { SystemNotifier.postNew(context, it) }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    factory: AppViewModelFactory,
    container: AppContainer,
    isOnline: Boolean
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Splash
    ) {
        composable(Routes.Splash) {
            SplashScreen(
                checkSignedIn = {
                    val signedIn = container.authRepository.isSignedIn()
                    if (signedIn) {
                        container.localUserData?.ensureOwner(
                            container.authRepository.currentUserIdOrNull()
                        )
                    }
                    signedIn
                },
                onFinished = { isSignedIn ->
                    // Not a tap, and must also happen if the app was in the background meanwhile.
                    navController.navigate(if (isSignedIn) Routes.Groups else Routes.Login) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Login) { entry ->
            val loginViewModel: LoginViewModel = viewModel(factory = factory)
            val uiState by loginViewModel.uiState.collectAsStateWithLifecycle()

            LoginScreen(
                uiState = uiState,
                onEmailChanged = loginViewModel::updateEmail,
                onPasswordChanged = loginViewModel::updatePassword,
                onLogin = loginViewModel::login,
                onSignIn = {
                    navController.navigateToGroups(entry)
                },
                onForgotPassword = {
                    navController.navigate(Routes.ForgotPassword)
                },
                onSignUp = {
                    navController.navigate(Routes.SignUp)
                }
            )
        }
        composable(Routes.SignUp) { entry ->
            val signUpViewModel: SignUpViewModel = viewModel(factory = factory)
            val uiState by signUpViewModel.uiState.collectAsStateWithLifecycle()

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
                    navController.navigateToGroups(entry)
                },
                onSignIn = {
                    navController.popFrom(entry)
                }
            )
        }
        composable(Routes.ForgotPassword) { entry ->
            val forgotPasswordViewModel: ForgotPasswordViewModel =
                viewModel(factory = factory)
            val uiState by forgotPasswordViewModel.uiState.collectAsStateWithLifecycle()

            ForgotPasswordScreen(
                uiState = uiState,
                onEmailChanged = forgotPasswordViewModel::updateEmail,
                onBack = {
                    navController.popFrom(entry)
                },
                onSendEmail = forgotPasswordViewModel::sendPasswordReset,
                onEmailSent = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(
                            LOGIN_NOTICE,
                            "We sent you an email with a link to reset your password."
                        )

                    navController.popFrom(entry)
                }
            )
        }
        composable(Routes.Groups) { entry ->
            val groupsViewModel: GroupsViewModel = viewModel(factory = factory)
            val uiState by groupsViewModel.uiState.collectAsStateWithLifecycle()

            LifecycleResumeEffect(Unit) {
                groupsViewModel.loadGroups()
                onPauseOrDispose { }
            }

            GroupsScreen(
                groups = uiState.groups,
                isLoading = uiState.isLoading,
                onGroupClick = {
                    navController.navigateFrom(
                        entry,
                        Routes.groupDetails(it.id)
                    )
                },
                onCreateGroup = {
                    navController.navigateFrom(entry, Routes.CreateGroup)
                },
                onProfileClick = {
                    navController.navigateFrom(entry, Routes.Profile)
                },
                message = uiState.errorMessage
            )
        }
        composable(Routes.CreateGroup) { entry ->
            val request = rememberRequest()
            CreateGroupScreen(
                onBack = { navController.popFrom(entry) },
                onProfileClick = { navController.navigateFrom(entry, Routes.Profile) },
                onCreateGroup = { title, description, members ->
                    request.run {
                        container.groupRepository
                            .createGroup(title, description, members)
                            .getOrThrow()
                        // Back to Groups, which shows the group from Room right away.
                        navController.popFrom(entry)
                    }
                },
                isLoading = request.isLoading,
                message = request.message
            )
        }
        composable(Routes.GroupDetails) { entry ->
            val groupId = entry.requireArg(Routes.GROUP_ID)
            val detailsFlow = remember(groupId) {
                container.groupRepository
                    .observeDetails(groupId)
                    .map { Loaded(data = it) }
            }
            val group by detailsFlow.collectAsStateWithLifecycle(initialValue = Loaded<GroupDetails>())

            LaunchedEffect(groupId) {
                // Best effort: refresh from the server; offline we show the Room copy.
                container.syncCoordinator?.syncGroup(groupId)
            }

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
            val me = container.authRepository.currentUserIdOrNull()
            val membersFlow = remember(groupId, me) {
                container.groupRepository
                    .observeMembers(groupId)
                    .map { list ->
                        list.map {
                            GroupMember(
                                it.id,
                                it.name,
                                it.isAdmin,
                                isMe = it.id == me,
                                isPending = it.isPending,
                                isFailed = it.isFailed
                            )
                        }
                    }
            }
            val members by membersFlow.collectAsStateWithLifecycle(initialValue = emptyList())
            LaunchedEffect(groupId) {
                // Best effort: refresh from the server; offline we show the Room copy.
                container.syncCoordinator?.syncGroup(groupId)
            }

            val request = rememberRequest()
            ManageMembersScreen(
                members = members,
                onBack = { navController.popFrom(entry) },
                onProfileClick = { navController.navigateFrom(entry, Routes.Profile) },
                onAddMember = { email ->
                    request.run {
                        container.groupRepository
                            .inviteMember(groupId, email)
                            .getOrThrow()
                    }
                },
                onRemoveMember = { member ->
                    request.run {
                        container.groupRepository
                            .removeMember(groupId, member.id)
                            .getOrThrow()
                    }
                },
                onCancelInvite = { member ->
                    request.run {
                        container.groupRepository.cancelInvite(groupId, member.name).getOrThrow()
                    }
                },
                isLoading = request.isLoading,
                message = request.message
            )
        }
        composable(Routes.AddExpense) { entry ->
            val groupId = entry.requireArg(Routes.GROUP_ID)
            val me = container.authRepository.currentUserIdOrNull()

            LaunchedEffect(groupId) {
                // Best effort: refresh from the server; offline we show the Room copy.
                container.syncCoordinator?.syncGroup(groupId)
            }

            val members by loadFromDatabase {
                container.groupRepository.getMembers(groupId).getOrThrow()
                    .sortedByDescending { it.id == me }
                    .map { ExpenseMember(it.id, if (it.id == me) "You" else it.name) }
            }
            val request = rememberRequest()
            LoadedScreen(members) { loaded ->
                AddExpenseScreen(
                    members = loaded,
                    // getExpenseMembers puts the signed-in user first.
                    defaultPayerId = loaded.firstOrNull()?.id,
                    onBack = { navController.popFrom(entry) },
                    onAddExpense = { draft ->
                        request.run {
                            val expense = Expense(
                                id = "expense-${System.currentTimeMillis()}",
                                groupId = groupId,
                                title = draft.title,
                                amountInCents = draft.amount
                                    .movePointRight(2)
                                    .longValueExact(),
                                paidBy = draft.paidBy,
                                splitBetween = draft.splitBetween
                            )

                            container.expenseRepository
                                .addExpense(expense)
                                .getOrThrow()

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
            val expense by loadFromDatabase {
                container.expenseRepository.getExpenseDetails(expenseId).getOrThrow()
            }
            val delete = rememberRequest()
            LoadedScreen(expense) { loaded ->
                ExpenseDetailsScreen(
                    expense = loaded,
                    onBack = { navController.popFrom(entry) },
                    onEditExpense = { navController.navigateFrom(entry, Routes.editExpense(expenseId)) },
                    onDeleteExpense = {
                        delete.run {
                            container.expenseRepository.deleteExpense(expenseId).getOrThrow()
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
                container.expenseRepository.getExpenseEdit(expenseId).getOrThrow()
            }
            val request = rememberRequest()
            LoadedScreen(editable) { loaded ->
                AddExpenseScreen(
                    members = loaded.members,
                    initial = loaded.draft,
                    screenTitle = "Edit Expense",
                    buttonText = "Save Changes",
                    onBack = { navController.popFrom(entry) },
                    onAddExpense = { updated ->
                        request.run {
                            container.expenseRepository
                                .updateExpense(
                                    Expense(
                                        id = expenseId,
                                        groupId = loaded.groupId,
                                        title = updated.title,
                                        amountInCents = updated.amount
                                            .movePointRight(2)
                                            .longValueExact(),
                                        paidBy = updated.paidBy,
                                        splitBetween = updated.splitBetween
                                    )
                                )
                                .getOrThrow()

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
            // Load from cache immediately, sync in background.
            val sharedFlow = remember(groupId, userId) {
                container.expenseRepository
                    .observeSharedExpenses(groupId, userId)
                    .map { Loaded(data = it) }
            }
            val shared by sharedFlow.collectAsStateWithLifecycle(initialValue = Loaded<SharedExpenses>())

            LaunchedEffect(groupId) {
                container.syncCoordinator?.syncGroupByRemoteId(groupId)
            }

            LoadedScreen(shared) { loaded ->
                SharedExpensesScreen(
                    shared = loaded,
                    onBack = { navController.popFrom(entry) },
                    onExpenseClick = { navController.navigateFrom(entry, Routes.expenseDetails(it.id)) }
                )
            }
        }
        composable(Routes.Notifications) { entry ->
            // Load from cache immediately, sync in background.
            val notificationsFlow = remember(entry) {
                container.notificationRepository
                    .observeNotifications()
                    .map { Loaded(data = it) }
            }
            val notifications by notificationsFlow.collectAsStateWithLifecycle(
                initialValue = Loaded<List<AppNotification>>(emptyList())
            )

            LaunchedEffect(Unit) {
                container.syncCoordinator?.syncNotifications()
            }

            val open = rememberRequest()
            LoadedScreen(notifications) { loaded ->
                NotificationsScreen(
                    notifications = loaded.map { n ->
                        NotificationItem(n.id, n.title, n.message, n.timeLabel, n.isUnread, n.groupId, n.expenseId)
                    },
                    onNotificationClick = { notification ->
                        open.run {
                            if (notification.isUnread) {
                                container.notificationRepository.markNotificationRead(notification.id).getOrThrow()
                            }
                            when {
                                notification.expenseId != null ->
                                    navController.navigateFrom(entry, Routes.expenseDetails(notification.expenseId))
                                notification.groupId != null ->
                                    navController.navigateFrom(entry, Routes.groupDetails(notification.groupId))
                            }
                        }
                    },
                    onProfileClick = { navController.navigateFrom(entry, Routes.Profile) },
                    message = open.message
                )
            }
        }
        composable(Routes.Balances) { entry ->
            // Load from cache immediately, sync in background.
            val balancesFlow = remember(entry) {
                container.balanceRepository
                    .observeBalances()
                    .map { Loaded(data = it) }
            }
            val balances by balancesFlow.collectAsStateWithLifecycle(
                initialValue = Loaded<BalancesSummary>(BalancesSummary("", emptyList()))
            )

            LaunchedEffect(Unit) {
                container.syncCoordinator?.syncBalances()
            }

            val remind = rememberRequest()
            LoadedScreen(balances) { loaded ->
                BalancesScreen(
                    totalBalanceText = loaded.totalText,
                    groups = loaded.groups.map { gb ->
                        FeatureGroupBalances(
                            gb.groupName,
                            gb.entries.map { e ->
                                FeatureBalanceEntry(
                                    e.id,
                                    e.personName,
                                    e.amountText,
                                    when (e.direction) {
                                        BalanceDirection.OwedToYou -> FeatureBalanceDirection.OwedToYou
                                        BalanceDirection.YouOwe -> FeatureBalanceDirection.YouOwe
                                    },
                                    e.isPending,
                                    e.isFailed
                                )
                            }
                        )
                    },
                    onRemind = { balance ->
                        remind.run {
                            val dataEntry = BalanceEntry(
                                balance.id,
                                balance.personName,
                                balance.amountText,
                                when (balance.direction) {
                                    FeatureBalanceDirection.OwedToYou -> BalanceDirection.OwedToYou
                                    FeatureBalanceDirection.YouOwe -> BalanceDirection.YouOwe
                                }
                            )
                            container.balanceRepository.sendPaymentReminder(dataEntry)
                            remind.message = if (isOnline) {
                                "Reminder sent to ${balance.personName}"
                            } else {
                                "Reminder queued — sent to ${balance.personName} when you're online"
                            }
                        }
                    },
                    onEntryClick = { balance ->
                        val (groupId, userId) = balance.id.split("/")
                        navController.navigateFrom(entry, Routes.sharedExpenses(groupId, userId))
                    },
                    onProfileClick = { navController.navigateFrom(entry, Routes.Profile) },
                    message = remind.message
                )
            }
        }
        composable(Routes.Profile) { entry ->
            // Load from cache immediately, sync in background.
            val profileFlow = remember(entry) {
                container.profileRepository
                    .observeProfile()
                    .map { Loaded(data = it) }
            }
            val profile by profileFlow.collectAsStateWithLifecycle(initialValue = Loaded<UserProfile>())

            LaunchedEffect(Unit) {
                container.syncCoordinator?.syncProfile()
            }

            val save = rememberRequest()
            val notificationPermission = rememberNotificationPermission()
            LoadedScreen(profile) { loaded ->
                ProfileScreen(
                    profile = loaded,
                    onSaveChanges = { updated ->
                        save.run {
                            if (updated.email.trim() != loaded.email && !isOnline) {
                                error("Email can only be changed when you're online.")
                            }

                            container.profileRepository
                                .updateProfile(
                                    User(
                                        id = "", // replaced with the signed-in id by the repository
                                        name = updated.name,
                                        email = updated.email,
                                        phone = updated.phone,
                                        notificationsEnabled = updated.notificationsEnabled
                                    )
                                )
                                .getOrThrow()

                            if (updated.email.trim() != loaded.email) {
                                // Email changes go straight to the server: Supabase sends a
                                // confirmation link, so they cannot be queued while offline.
                                AppRepository.saveProfile(loaded, updated)
                            }

                            navController.popFrom(entry)
                        }
                    },
                    onSignOut = {
                        save.run {
                            container.localUserData?.clear()
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
