package com.jumincho.beatingyesterday.core.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinDuration

/**
 * The system clock in the device's *current* default time zone.
 *
 * Unlike [Clock.systemDefaultZone], which captures the zone once, this follows time-zone changes
 * made while the app is running, so day boundaries stay local.
 */
object DeviceClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = Clock.system(zone)

    override fun instant(): Instant = Instant.now()
}

/** The current local date according to this clock. */
fun Clock.today(): LocalDate = LocalDate.now(this)

/**
 * Emits the current local date, and again whenever it changes.
 *
 * The flow wakes at the next midnight, and at least every [recheckInterval] so that manual clock
 * or time-zone changes are picked up too.
 */
fun Clock.observeToday(recheckInterval: Duration = 1.minutes): Flow<LocalDate> = flow {
    while (true) {
        val now = ZonedDateTime.now(this@observeToday)
        emit(now.toLocalDate())
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        val untilMidnight = java.time.Duration.between(now, nextMidnight).toKotlinDuration()
        delay(minOf(untilMidnight, recheckInterval))
    }
}.distinctUntilChanged()
