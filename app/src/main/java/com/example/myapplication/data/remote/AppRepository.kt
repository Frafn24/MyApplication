package com.example.myapplication.data.remote

import com.example.myapplication.data.model.GroupSummary
import com.example.myapplication.feature.balances.BalanceDirection
import com.example.myapplication.feature.balances.BalanceEntry
import com.example.myapplication.feature.balances.GroupBalances
import com.example.myapplication.feature.expenses.ExpenseDetails
import com.example.myapplication.feature.expenses.ExpenseDraft
import com.example.myapplication.feature.expenses.ExpenseMember
import com.example.myapplication.feature.expenses.ExpenseSplit
import com.example.myapplication.feature.expenses.SharedExpenses
import com.example.myapplication.feature.groups.GroupDetails
import com.example.myapplication.feature.groups.GroupExpenseItem
import com.example.myapplication.feature.groups.GroupMember
import com.example.myapplication.feature.notifications.NotificationItem
import com.example.myapplication.feature.profile.UserProfile
import com.example.myapplication.ui.Format
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * All database and auth calls used by the screens, on top of [SupabaseProvider.client].
 * Results are returned as the screens' own data classes.
 * Row Level Security makes sure each user only gets their own data.
 */
object AppRepository {
    private val client get() = SupabaseProvider.client
    private const val NOTIFICATION_LIMIT = 50L

    // --- Auth (Splash, Login, Sign up, Forgot password) ---

    suspend fun isSignedIn(): Boolean {
        client.auth.awaitInitialization()
        return client.auth.currentSessionOrNull() != null
    }

    suspend fun signIn(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    /** Returns true if the user is signed in, false if they must confirm their email first. */
    suspend fun signUp(name: String, email: String, password: String): Boolean {
        client.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
            // Copied to profiles.full_name by the handle_new_user trigger.
            data = buildJsonObject { put("full_name", name.trim()) }
        }
        return client.auth.currentSessionOrNull() != null
    }

    suspend fun sendPasswordReset(email: String) {
        client.auth.resetPasswordForEmail(email.trim())
    }

    /** Always signs out on this phone, also when the server can't be reached. */
    suspend fun signOut() {
        try {
            client.auth.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Network error: forget the session locally anyway.
            client.auth.clearSession()
        }
    }

    private fun myUserId(): String = checkNotNull(client.auth.currentUserOrNull()?.id) { "You are not signed in." }

    // --- Groups ---

    suspend fun getGroups(): List<GroupSummary> = client.postgrest.rpc("get_my_groups")
        .decodeList<GroupRow>()
        .map { GroupSummary(it.id, it.name, it.memberCount, Format.groupBalance(it.myBalance)) }

    /** Creates the group with the given members (email addresses). Returns the new group's id. */
    suspend fun createGroup(name: String, description: String, members: List<String>): String = client.postgrest.rpc(
        "create_group",
        buildJsonObject {
            put("p_name", name.trim())
            put("p_description", description.trim())
            putJsonArray("p_members") { members.forEach { add(it.trim()) } }
        }
    ).decodeAs<String>()

    /** Group Details: name, description, the user's balance and the group's expenses. */
    suspend fun getGroupDetails(groupId: String): GroupDetails {
        val me = myUserId()
        val group = client.from("groups")
            .select(Columns.list("id", "name", "description")) { filter { eq("id", groupId) } }
            .decodeSingleOrNull<GroupInfoRow>()
            ?: error("This group does not exist or you are no longer a member.")
        val balance = client.postgrest.rpc("get_my_balances").decodeList<BalanceRow>()
            .filter { it.groupId == groupId }
            .sumOf { it.amount }
        val expenses = client.postgrest.rpc("get_group_expenses", buildJsonObject { put("p_group_id", groupId) })
            .decodeList<GroupExpenseRow>()
            .map { row ->
                val payer = if (row.paidBy == me) "You" else row.paidByName
                val share = if (row.myShare > 0) "your share ${Format.dkk(row.myShare)}" else "not in the split"
                GroupExpenseItem(row.id, row.description, Format.dkk(row.amount), "$payer paid · $share")
            }
        return GroupDetails(
            id = group.id,
            name = group.name,
            description = group.description.orEmpty(),
            balanceText = Format.groupBalance(balance),
            expenses = expenses
        )
    }

    // --- Members ---

    suspend fun getGroupMembers(groupId: String): List<GroupMember> =
        client.postgrest.rpc("get_group_members", buildJsonObject { put("p_group_id", groupId) })
            .decodeList<GroupMemberRow>()
            .map { GroupMember(it.userId, it.name, isAdmin = it.isAdmin, isMe = it.isMe) }

    /** Adds a user by email address. Only admins can do this. */
    suspend fun addGroupMember(groupId: String, email: String) {
        client.postgrest.rpc(
            "add_group_member",
            buildJsonObject {
                put("p_group_id", groupId)
                put("p_identifier", email.trim())
            }
        )
    }

    /** Removes a member (admins), or leaves the group when [userId] is the signed-in user. */
    suspend fun removeGroupMember(groupId: String, userId: String) {
        client.from("group_members").delete {
            filter {
                eq("group_id", groupId)
                eq("user_id", userId)
            }
        }
    }

    // --- Expenses ---

    /** Members for the Add/Edit Expense screen; the signed-in user is shown as "You". */
    suspend fun getExpenseMembers(groupId: String): List<ExpenseMember> = getGroupMembers(groupId)
        .sortedByDescending { it.isMe }
        .map { ExpenseMember(it.id, if (it.isMe) "You" else it.name) }

    /** Saves the expense split equally between [ExpenseDraft.splitBetween]. Returns its id. */
    suspend fun createExpense(groupId: String, draft: ExpenseDraft): String = client.postgrest.rpc(
        "create_expense",
        buildJsonObject {
            put("p_group_id", groupId)
            putExpense(draft)
        }
    ).decodeAs<String>()

    suspend fun updateExpense(expenseId: String, draft: ExpenseDraft) {
        client.postgrest.rpc(
            "update_expense",
            buildJsonObject {
                put("p_expense_id", expenseId)
                putExpense(draft)
            }
        )
    }

    private fun JsonObjectBuilder.putExpense(draft: ExpenseDraft) {
        put("p_description", draft.title.trim())
        // As text so the amount is not rounded through a Double.
        put("p_amount", draft.amount.toPlainString())
        put("p_paid_by", draft.paidBy)
        putJsonArray("p_split_with") { draft.splitBetween.forEach { add(it) } }
    }

    suspend fun getExpenseDetails(expenseId: String): ExpenseDetails {
        val me = myUserId()
        val row = getExpenseRow(expenseId)
        return ExpenseDetails(
            title = row.description,
            amount = row.amount.toBigDecimal(),
            paidBy = if (row.paidBy == me) "You" else row.paidByName,
            splits = row.splits.map { ExpenseSplit(if (it.userId == me) "You" else it.name, it.amount.toBigDecimal()) },
            canEdit = row.canEdit,
            canDelete = row.paidBy == me
        )
    }

    /** Only the person who paid the expense can delete it (checked by the database too). */
    suspend fun deleteExpense(expenseId: String) {
        client.postgrest.rpc("delete_expense", buildJsonObject { put("p_expense_id", expenseId) })
    }

    /** The group id and the current values of an expense, for the Edit Expense screen. */
    suspend fun getExpenseDraft(expenseId: String): Pair<String, ExpenseDraft> {
        val row = getExpenseRow(expenseId)
        return row.groupId to ExpenseDraft(
            title = row.description,
            amount = row.amount.toBigDecimal(),
            paidBy = row.paidBy,
            splitBetween = row.splits.map { it.userId }
        )
    }

    /** Balances -> Expense Details: every expense between the user and [otherUserId] in a group. */
    suspend fun getSharedExpenses(groupId: String, otherUserId: String): SharedExpenses {
        val groupName = client.from("groups")
            .select(Columns.list("id", "name")) { filter { eq("id", groupId) } }
            .decodeSingleOrNull<GroupInfoRow>()
            ?.name
            ?: error("This group does not exist or you are no longer a member.")
        // Same fallback as get_my_balances, so the name matches the Balances screen.
        val other = client.from("profiles")
            .select(Columns.list("full_name", "username")) { filter { eq("id", otherUserId) } }
            .decodeSingleOrNull<NameRow>()
        val otherName = other?.fullName?.ifBlank { null } ?: other?.username ?: "Unknown"
        val rows = client.postgrest.rpc(
            "get_shared_expenses",
            buildJsonObject {
                put("p_group_id", groupId)
                put("p_other_user_id", otherUserId)
            }
        ).decodeList<SharedExpenseRow>()

        val total = rows.sumOf { it.effect }
        return SharedExpenses(
            title = "You and $otherName · $groupName",
            totalText = when {
                total > 0 -> "$otherName owes you ${Format.dkk(total)}"
                total < 0 -> "You owe $otherName ${Format.dkk(total)}"
                else -> "You are settled up"
            },
            expenses = rows.map { row ->
                val detail = if (row.paidByMe) {
                    "You paid · $otherName's share ${Format.dkk(row.effect)}"
                } else {
                    "$otherName paid · your share ${Format.dkk(row.effect)}"
                }
                GroupExpenseItem(row.id, row.description, Format.dkk(row.amount), detail)
            }
        )
    }

    private suspend fun getExpenseRow(expenseId: String): ExpenseDetailsRow =
        client.postgrest.rpc("get_expense_details", buildJsonObject { put("p_expense_id", expenseId) })
            .decodeList<ExpenseDetailsRow>()
            .firstOrNull()
            ?: error("This expense was deleted or you can no longer see it.")

    // --- Balances ---

    /** The total and the per-group entries for the Balances screen. */
    suspend fun getBalances(): Pair<String, List<GroupBalances>> {
        val rows = client.postgrest.rpc("get_my_balances").decodeList<BalanceRow>()
        val groups = rows.groupBy { it.groupId }.map { (groupId, groupRows) ->
            GroupBalances(
                groupName = groupRows.first().groupName,
                entries = groupRows.map { row ->
                    BalanceEntry(
                        // "<group id>/<user id>", used by sendPaymentReminder.
                        id = "$groupId/${row.otherUserId}",
                        personName = row.otherName,
                        amountText = Format.dkk(row.amount),
                        direction = if (row.amount > 0) BalanceDirection.OwedToYou else BalanceDirection.YouOwe
                    )
                }
            )
        }
        return Format.signedDkk(rows.sumOf { it.amount }) to groups
    }

    suspend fun sendPaymentReminder(entry: BalanceEntry) {
        val (groupId, userId) = entry.id.split("/")
        client.postgrest.rpc(
            "send_payment_reminder",
            buildJsonObject {
                put("p_group_id", groupId)
                put("p_debtor_id", userId)
            }
        )
    }

    // --- Notifications ---

    suspend fun getNotifications(): List<NotificationItem> {
        val now = Clock.System.now()
        return client.from("notifications")
            .select {
                order("created_at", Order.DESCENDING)
                limit(NOTIFICATION_LIMIT)
            }
            .decodeList<NotificationRow>()
            .map {
                NotificationItem(
                    id = it.id,
                    title = it.title,
                    message = it.message,
                    timeText = Format.relativeTime(it.createdAt, now),
                    isUnread = it.readAt == null,
                    groupId = it.groupId,
                    expenseId = it.expenseId
                )
            }
    }

    suspend fun markNotificationRead(id: String) {
        client.postgrest.rpc("mark_notifications_read", buildJsonObject { putJsonArray("p_ids") { add(id) } })
    }

    // --- Profile ---

    suspend fun getProfile(): UserProfile {
        val user = checkNotNull(client.auth.currentUserOrNull()) { "You are not signed in." }
        val row = client.from("profiles")
            .select { filter { eq("id", user.id) } }
            .decodeSingleOrNull<ProfileRow>()
        return UserProfile(
            name = row?.fullName.orEmpty(),
            email = user.email.orEmpty(),
            phone = row?.phone.orEmpty(),
            notificationsEnabled = row?.notificationsEnabled ?: true
        )
    }

    /** A changed email only takes effect after the user confirms it via the link Supabase sends. */
    suspend fun saveProfile(old: UserProfile, new: UserProfile) {
        val userId = checkNotNull(client.auth.currentUserOrNull()?.id) { "You are not signed in." }
        // A JSON object, not ProfileRow: the client leaves out values equal to a
        // property's default, so turning notifications back on or clearing the
        // name/phone would otherwise never be saved.
        client.from("profiles").update(
            buildJsonObject {
                put("full_name", new.name.trim().ifBlank { null })
                put("phone", new.phone.trim().ifBlank { null })
                put("notifications_enabled", new.notificationsEnabled)
            }
        ) { filter { eq("id", userId) } }
        if (new.email.trim() != old.email) {
            client.auth.updateUser { email = new.email.trim() }
        }
    }
}

@Serializable
private data class GroupRow(
    val id: String,
    val name: String,
    @SerialName("member_count") val memberCount: Int,
    @SerialName("my_balance") val myBalance: Double
)

@Serializable
private data class BalanceRow(
    @SerialName("group_id") val groupId: String,
    @SerialName("group_name") val groupName: String,
    @SerialName("other_user_id") val otherUserId: String,
    @SerialName("other_name") val otherName: String,
    val amount: Double
)

@Serializable
private data class GroupInfoRow(val id: String, val name: String, val description: String? = null)

@Serializable
private data class GroupExpenseRow(
    val id: String,
    val description: String,
    val amount: Double,
    @SerialName("paid_by") val paidBy: String,
    @SerialName("paid_by_name") val paidByName: String,
    @SerialName("my_share") val myShare: Double
)

@Serializable
private data class GroupMemberRow(
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("is_admin") val isAdmin: Boolean,
    @SerialName("is_me") val isMe: Boolean
)

@Serializable
private data class ExpenseDetailsRow(
    @SerialName("group_id") val groupId: String,
    val description: String,
    val amount: Double,
    @SerialName("paid_by") val paidBy: String,
    @SerialName("paid_by_name") val paidByName: String,
    @SerialName("can_edit") val canEdit: Boolean,
    val splits: List<ExpenseSplitRow>
)

@Serializable
private data class ExpenseSplitRow(@SerialName("user_id") val userId: String, val name: String, val amount: Double)

@Serializable
private data class NotificationRow(
    val id: String,
    val title: String,
    val message: String,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("read_at") val readAt: Instant? = null,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("expense_id") val expenseId: String? = null
)

@Serializable
private data class NameRow(@SerialName("full_name") val fullName: String? = null, val username: String? = null)

@Serializable
private data class SharedExpenseRow(
    val id: String,
    val description: String,
    val amount: Double,
    @SerialName("paid_by_me") val paidByMe: Boolean,
    val effect: Double
)

@Serializable
private data class ProfileRow(
    @SerialName("full_name") val fullName: String? = null,
    val phone: String? = null,
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean = true
)

/** Turns an error from Supabase (or the network) into a message for the user. */
fun Throwable.toUserMessage(): String = when (this) {
    is AuthRestException -> when (errorCode) {
        AuthErrorCode.InvalidCredentials -> "Wrong email or password."
        AuthErrorCode.EmailNotConfirmed -> "Please confirm your email before signing in."
        AuthErrorCode.UserAlreadyExists -> "An account with this email already exists."
        AuthErrorCode.WeakPassword -> "The password is too weak."
        AuthErrorCode.OverEmailSendRateLimit -> "Too many emails sent. Please try again later."
        else -> description.orEmpty().ifBlank { GENERIC_ERROR }
    }
    // Errors raised on purpose by our database functions have a readable message.
    is PostgrestRestException -> if (code == "P0001") error else GENERIC_ERROR
    is HttpRequestException, is IOException -> "No connection. Check your internet and try again."
    is IllegalStateException -> message ?: GENERIC_ERROR
    else -> GENERIC_ERROR
}

private const val GENERIC_ERROR = "Something went wrong. Please try again."

/** Like [runCatching], but does not swallow coroutine cancellation. */
suspend fun <T> runCatchingCancellable(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
