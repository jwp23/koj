package io.github.jwp23.koj

import io.github.jwp23.koj.storage.CheckInStore
import io.github.jwp23.koj.storage.DrinkStore
import io.github.jwp23.koj.storage.KojDatabase
import io.github.jwp23.koj.storage.SettingsStore
import io.github.jwp23.koj.storage.SourceStore
import java.time.Clock

/** Everything the screens share. Built once by the Application. */
class AppContainer(database: KojDatabase, val clock: Clock = Clock.systemDefaultZone()) {
    val sources = SourceStore(database.sourceDao())
    val drinks = DrinkStore(database.drinkDao())
    val checkIns = CheckInStore(database.checkInDao())
    val settings = SettingsStore(database.settingsDao())
}
