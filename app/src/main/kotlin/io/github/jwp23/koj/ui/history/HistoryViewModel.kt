package io.github.jwp23.koj.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jwp23.koj.graph.DayGraph
import io.github.jwp23.koj.graph.dayBounds
import io.github.jwp23.koj.graph.dayGraph
import io.github.jwp23.koj.history.HistoryDay
import io.github.jwp23.koj.history.historyDays
import io.github.jwp23.koj.record.LoggedDrink
import io.github.jwp23.koj.record.Settings
import io.github.jwp23.koj.storage.CheckInStore
import io.github.jwp23.koj.storage.DrinkStore
import io.github.jwp23.koj.storage.SettingsStore
import io.github.jwp23.koj.ui.minuteTicks
import java.time.Clock
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Every day with something logged, newest first, and what is needed to draw each day's graph. */
data class HistoryState(
    val now: Instant,
    val settings: Settings,
    val days: List<HistoryDay>,
    val allDrinks: List<LoggedDrink>,
) {
    /**
     * The graph of [day], from every stored drink so that earlier days carry over. It uses the
     * current settings, so a changed half-life redraws every day. A day that has not started by
     * [now] (a clock set back) is drawn as of its own start.
     */
    fun graph(day: HistoryDay): DayGraph {
        val dayStart = dayBounds(day.date, day.zone).first
        return dayGraph(day.date, day.zone, maxOf(now, dayStart), settings, allDrinks, day.checkIns)
    }
}

/** Reads everything that was logged, so that any day can be shown and any entry opened. */
class HistoryViewModel(
    drinks: DrinkStore,
    checkIns: CheckInStore,
    settings: SettingsStore,
    clock: Clock,
    ticks: Flow<Unit> = minuteTicks(),
) : ViewModel() {
    /** Null until the first values arrive. */
    val state: StateFlow<HistoryState?> =
        combine(settings.settings(), drinks.drinks(), checkIns.checkIns(), ticks) {
                current,
                allDrinks,
                allCheckIns,
                _ ->
                HistoryState(
                    now = clock.instant(),
                    settings = current,
                    days = historyDays(allDrinks, allCheckIns),
                    allDrinks = allDrinks,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/**
 * Runs [write] and reports whether it went through. False means the stored entry it changes no
 * longer exists, such as one deleted since it was loaded; a cancellation still propagates.
 */
internal suspend fun entryStillStored(write: suspend () -> Unit): Boolean =
    try {
        write()
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: IllegalStateException) {
        false
    }
