package com.example.myapplication.notifications

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Notification setup and the "Allow notifications?" permission.
 *
 * Android 13+ (API 33) needs the POST_NOTIFICATIONS permission and asks with a
 * system popup, like iOS. Android 12 and older allow notifications by default.
 * If the user taps "Don't allow" twice, Android stops showing the popup; then the
 * only way to turn notifications on is the app's page in the phone's settings.
 */
object AppNotifications {
    /** Channel for group, expense and reminder notifications. Android 8+ needs a channel to show anything. */
    const val CHANNEL_GROUP_ACTIVITY = "group_activity"

    private const val PREFS = "notifications"
    private const val KEY_ASKED = "permission_asked"

    /** Safe to call on every app start; creating an existing channel does nothing. */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_GROUP_ACTIVITY,
            "Groups and expenses",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "New expenses, new group members and payment reminders" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** True if the app may show notifications: permission granted and not turned off in the phone's settings. */
    fun areAllowed(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    internal fun needsRuntimePermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    internal fun wasAsked(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ASKED, false)

    internal fun markAsked(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_ASKED, true) }
    }

    /** The app's notification page in the phone's settings. */
    internal fun openSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        }
        if (context.findActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

/** Whether notifications are allowed, kept up to date, and ways to ask for them. */
@Stable
class NotificationPermission internal constructor(
    private val context: Context,
    private val allowed: MutableState<Boolean>,
    private val launcher: ActivityResultLauncher<String>
) {
    /** Re-checked every time the app comes back to the foreground (e.g. from the phone's settings). */
    val isAllowed: Boolean get() = allowed.value

    /** Shows the system popup on app start, but only the first time ever. */
    fun requestOnce() {
        if (!isAllowed && AppNotifications.needsRuntimePermission(context) && !AppNotifications.wasAsked(context)) {
            ask()
        }
    }

    /**
     * When the user turns notifications on in the app: shows the system popup if
     * Android still allows it, otherwise opens the phone's notification settings.
     */
    fun request() {
        if (isAllowed) return
        val activity = context.findActivity()
        val canShowPopup = AppNotifications.needsRuntimePermission(context) &&
            (
                !AppNotifications.wasAsked(context) ||
                    (
                        activity != null &&
                            ActivityCompat.shouldShowRequestPermissionRationale(
                                activity,
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        )
                )
        if (canShowPopup) ask() else AppNotifications.openSettings(context)
    }

    private fun ask() {
        AppNotifications.markAsked(context)
        // Only reached on Android 13+ (needsRuntimePermission checks the version).
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
fun rememberNotificationPermission(): NotificationPermission {
    val context = LocalContext.current
    val allowed = remember { mutableStateOf(AppNotifications.areAllowed(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed.value = AppNotifications.areAllowed(context)
    }
    LifecycleResumeEffect(Unit) {
        allowed.value = AppNotifications.areAllowed(context)
        onPauseOrDispose { }
    }
    return remember(launcher) { NotificationPermission(context, allowed, launcher) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
