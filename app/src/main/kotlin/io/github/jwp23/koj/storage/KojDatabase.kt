package io.github.jwp23.koj.storage

import android.content.Context
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

/** The on-device database. Version 1 until the first release is tagged. */
@Database(entities = [SourceRow::class], version = 1)
@ColumnTypeConverters(Converters::class)
abstract class KojDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
}

/** Opens the app's database file. */
fun openKojDatabase(context: Context): KojDatabase =
    Room.databaseBuilder<KojDatabase>(context, "koj.db").setDriver(AndroidSQLiteDriver()).build()
