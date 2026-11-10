package com.example.myapplication.ui

import java.util.TimeZone
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun `amounts drop trailing zeros and the sign`() {
        assertEquals("100 DKK", Format.dkk(100.0))
        assertEquals("12.5 DKK", Format.dkk(-12.5))
        assertEquals("1,250.75 DKK", Format.dkk(1250.75))
    }

    @Test
    fun `signed amounts show direction`() {
        assertEquals("+ 150 DKK", Format.signedDkk(150.0))
        assertEquals("- 100 DKK", Format.signedDkk(-100.0))
        assertEquals("0 DKK", Format.signedDkk(0.0))
    }

    @Test
    fun `group balance text`() {
        assertEquals("You are owed 250 DKK", Format.groupBalance(250.0))
        assertEquals("You owe 100 DKK", Format.groupBalance(-100.0))
        assertEquals("All settled up", Format.groupBalance(0.0))
    }

    @Test
    fun `relative time`() {
        val now = Instant.parse("2026-11-10T12:00:00Z")
        fun ago(text: String, delta: kotlin.time.Duration) =
            assertEquals(text, Format.relativeTime(now - delta, now, TimeZone.getTimeZone("UTC")))

        ago("Just now", 30.seconds)
        ago("10 min ago", 10.minutes)
        ago("3 h ago", 3.hours)
        ago("Yesterday", 30.hours)
        ago("2 days ago", 2.days)
        ago("1 Nov 2026", 9.days)
    }
}
