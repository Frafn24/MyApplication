package com.example.myapplication.ui

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private const val WEEK_DAYS = 7

/** Pure formatting helpers for amounts and times shown in the UI. */
object Format {
    private val amountFormat = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))

    /** `100 DKK`, `12.5 DKK`. The sign is dropped; callers say who owes whom. */
    fun dkk(amount: Double): String = "${amountFormat.format(abs(amount))} DKK"

    /** `+ 150 DKK`, `- 100 DKK`, `0 DKK`. */
    fun signedDkk(amount: Double): String = when {
        amount > 0 -> "+ ${dkk(amount)}"
        amount < 0 -> "- ${dkk(amount)}"
        else -> dkk(0.0)
    }

    /** Text for a group card, from the user's point of view. */
    fun groupBalance(amount: Double): String = when {
        amount > 0 -> "You are owed ${dkk(amount)}"
        amount < 0 -> "You owe ${dkk(amount)}"
        else -> "All settled up"
    }

    /** `Just now`, `10 min ago`, `3 h ago`, `Yesterday`, `4 days ago`, then a date. */
    fun relativeTime(time: Instant, now: Instant, zone: TimeZone = TimeZone.getDefault()): String {
        val elapsed: Duration = now - time
        return when {
            elapsed < 1.minutes -> "Just now"
            elapsed < 1.hours -> "${elapsed.inWholeMinutes} min ago"
            elapsed < 1.days -> "${elapsed.inWholeHours} h ago"
            elapsed < 2.days -> "Yesterday"
            elapsed < WEEK_DAYS.days -> "${elapsed.inWholeDays} days ago"
            // java.time needs API 26; minSdk is 24.
            else -> SimpleDateFormat("d MMM yyyy", Locale.US)
                .apply { timeZone = zone }
                .format(Date(time.toEpochMilliseconds()))
        }
    }
}
