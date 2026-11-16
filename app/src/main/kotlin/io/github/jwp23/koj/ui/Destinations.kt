package io.github.jwp23.koj.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Today : NavKey

@Serializable data object History : NavKey

@Serializable data object Sources : NavKey

/** Named AppSettings because Settings is the stored record. */
@Serializable data object AppSettings : NavKey

/** A destination on the navigation bar. */
class BarDestination(val key: NavKey, val label: String)

/** The four destinations on the navigation bar, in order. */
val barDestinations =
    listOf(
        BarDestination(Today, "Today"),
        BarDestination(History, "History"),
        BarDestination(Sources, "Sources"),
        BarDestination(AppSettings, "Settings"),
    )
