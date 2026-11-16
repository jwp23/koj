package io.github.jwp23.koj.storage

import android.content.Context
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

/**
 * The on-device database. Version 1 until the first release is tagged, with no migrations before
 * then: a build that changes the schema cannot open a database written by an earlier build, so
 * uninstall the app or clear its data after pulling such a change.
 */
@Database(
    entities = [SourceRow::class, DrinkRow::class, CheckInRow::class, SettingsRow::class],
    version = 1,
)
@ColumnTypeConverters(Converters::class)
abstract class KojDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao

    abstract fun drinkDao(): DrinkDao

    abstract fun checkInDao(): CheckInDao

    abstract fun settingsDao(): SettingsDao
}

/** Opens the app's database file. */
fun openKojDatabase(context: Context): KojDatabase =
    Room.databaseBuilder<KojDatabase>(context, "koj.db").setDriver(AndroidSQLiteDriver()).build()
