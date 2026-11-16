package com.example.myapplication.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.example.myapplication.data.local.NotificationEntity

/**
 * Posts notifications that arrived while nobody was looking at the Alerts screen,
 * so new ones are visible without opening the screen. Uses the channel and
 * permission from [AppNotifications].
 */
object SystemNotifier {
    /** Keeps one pull from spamming the shade with everything at once. */
    private const val MAX_PER_PULL = 3

    fun postNew(context: Context, notifications: List<NotificationEntity>) {
        if (notifications.isEmpty()) return
        if (!AppNotifications.areAllowed(context)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val manager = NotificationManagerCompat.from(context)
        notifications.take(MAX_PER_PULL).forEach { notification ->
            val systemNotification = NotificationCompat.Builder(
                context,
                AppNotifications.CHANNEL_GROUP_ACTIVITY
            )
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(notification.title)
                .setContentText(notification.message)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

            // The same id overwrites instead of piling up in the shade.
            runCatching { manager.notify(notification.id.hashCode(), systemNotification) }
        }
    }
}
