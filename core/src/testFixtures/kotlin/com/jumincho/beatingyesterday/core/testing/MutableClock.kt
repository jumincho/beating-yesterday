package com.jumincho.beatingyesterday.core.testing

import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/** A [Clock] for tests whose time only moves when told to. */
class MutableClock(var instant: Instant, private val zone: ZoneId = ZoneId.of("Asia/Seoul")) : Clock() {

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(instant, zone)

    override fun instant(): Instant = instant

    /** Moves the clock forward by [duration]. */
    fun advanceBy(duration: Duration) {
        instant = instant.plus(duration.toJavaDuration())
    }

    companion object {
        /** A clock set to [dateTime] in Asia/Seoul. */
        fun at(dateTime: LocalDateTime): MutableClock {
            val zone = ZoneId.of("Asia/Seoul")
            return MutableClock(dateTime.atZone(zone).toInstant(), zone)
        }
    }
}
