package com.jumincho.beatingyesterday.ui.format

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ClockTextTest {

    @Test
    fun `shows minutes and seconds, dropping partial seconds by default`() {
        assertEquals("25:00", clockText(25.minutes))
        assertEquals("00:59", clockText(59.seconds + 900.milliseconds))
    }

    @Test
    fun `rounds partial seconds up for a countdown`() {
        assertEquals("25:00", clockText(25.minutes, roundUp = true))
        assertEquals("24:59", clockText(24.minutes + 58.seconds + 100.milliseconds, roundUp = true))
    }

    @Test
    fun `adds hours when needed`() {
        assertEquals("1:02:03", clockText(1.hours + 2.minutes + 3.seconds))
    }

    @Test
    fun `never shows negative time`() {
        assertEquals("00:00", clockText((-5).seconds))
    }
}
