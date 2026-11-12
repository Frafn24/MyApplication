package com.example.myapplication.navigation

// Route names use PascalCase (Routes.Login) by team convention.
@Suppress("ktlint:standard:property-naming")
object Routes {
    const val Splash = "splash"
    const val Login = "login"
    const val SignUp = "sign_up"
    const val ForgotPassword = "forgot_password"
    const val Groups = "groups"
    const val Profile = "profile"
    const val Balances = "balances"
    const val Notifications = "notifications"
    const val CreateGroup = "create_group"

    // Routes with an id. Use the functions below to build them.
    const val GroupDetails = "group/{groupId}"
    const val ManageMembers = "group/{groupId}/members"
    const val AddExpense = "group/{groupId}/add_expense"
    const val ExpenseDetails = "expense/{expenseId}"
    const val EditExpense = "expense/{expenseId}/edit"

    // Balances -> "View details": the expenses between you and one person in a group.
    const val SharedExpenses = "balance/{groupId}/{userId}"

    const val GROUP_ID = "groupId"
    const val EXPENSE_ID = "expenseId"
    const val USER_ID = "userId"

    fun groupDetails(groupId: String) = "group/$groupId"
    fun manageMembers(groupId: String) = "group/$groupId/members"
    fun addExpense(groupId: String) = "group/$groupId/add_expense"
    fun expenseDetails(expenseId: String) = "expense/$expenseId"
    fun editExpense(expenseId: String) = "expense/$expenseId/edit"
    fun sharedExpenses(groupId: String, userId: String) = "balance/$groupId/$userId"
}
