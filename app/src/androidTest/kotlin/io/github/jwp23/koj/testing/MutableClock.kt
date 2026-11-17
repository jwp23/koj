package io.github.jwp23.koj.testing

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** A clock a test can move. */
class MutableClock(var now: Instant, zone: ZoneId = ZoneOffset.UTC) : Clock() {
    private var currentZone = zone

    override fun instant(): Instant = now

    override fun getZone(): ZoneId = currentZone

    /** Moves the clock to [zone], as when the device's time zone changes. */
    fun moveTo(zone: ZoneId) {
        currentZone = zone
    }

    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)
}
