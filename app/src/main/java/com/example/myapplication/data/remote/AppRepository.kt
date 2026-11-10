package com.example.myapplication.data.remote

import com.example.myapplication.feature.balances.BalanceDirection
import com.example.myapplication.feature.balances.BalanceEntry
import com.example.myapplication.feature.balances.GroupBalances
import com.example.myapplication.feature.groups.GroupSummary
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
import io.github.jan.supabase.postgrest.query.Order
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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

    // --- Groups ---

    suspend fun getGroups(): List<GroupSummary> = client.postgrest.rpc("get_my_groups")
        .decodeList<GroupRow>()
        .map { GroupSummary(it.id, it.name, it.memberCount, Format.groupBalance(it.myBalance)) }

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
            .map { NotificationItem(it.id, it.title, it.message, Format.relativeTime(it.createdAt, now)) }
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
        client.from("profiles").update(
            ProfileRow(
                fullName = new.name.trim().ifBlank { null },
                phone = new.phone.trim().ifBlank { null },
                notificationsEnabled = new.notificationsEnabled
            )
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
private data class NotificationRow(
    val id: String,
    val title: String,
    val message: String,
    @SerialName("created_at") val createdAt: Instant
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
