package io.github.jwp23.koj

import io.github.jwp23.koj.storage.CheckInStore
import io.github.jwp23.koj.storage.DrinkStore
import io.github.jwp23.koj.storage.KojDatabase
import io.github.jwp23.koj.storage.SettingsStore
import io.github.jwp23.koj.storage.SourceStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * [base]'s instants, in the zone [zone] returns on each call, so the zone follows a change of the
 * device's time zone while the app runs.
 */
class DeviceZoneClock(private val base: Clock, private val zone: () -> ZoneId) : Clock() {
    override fun instant(): Instant = base.instant()

    override fun getZone(): ZoneId = zone()

    override fun withZone(zone: ZoneId): Clock = base.withZone(zone)
}

/** Everything the screens share. Built once by the Application. */
class AppContainer(
    database: KojDatabase,
    val clock: Clock = DeviceZoneClock(Clock.systemUTC(), ZoneId::systemDefault),
) {
    val sources = SourceStore(database.sourceDao())
    val drinks = DrinkStore(database.drinkDao())
    val checkIns = CheckInStore(database.checkInDao())
    val settings = SettingsStore(database.settingsDao())
}
