package io.github.jwp23.koj.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jwp23.koj.graph.DayGraph
import io.github.jwp23.koj.graph.carryOver
import io.github.jwp23.koj.graph.dayBounds
import io.github.jwp23.koj.graph.dayGraph
import io.github.jwp23.koj.intake.Amounts
import io.github.jwp23.koj.intake.IntakeModel
import io.github.jwp23.koj.record.LoggedDrink
import io.github.jwp23.koj.record.Source
import io.github.jwp23.koj.storage.CheckInStore
import io.github.jwp23.koj.storage.DrinkStore
import io.github.jwp23.koj.storage.SettingsStore
import io.github.jwp23.koj.storage.SourceStore
import io.github.jwp23.koj.ui.minuteTicks
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayState(
    val now: Instant,
    val zone: ZoneId,
    val inBody: Amounts,
    val sources: List<Source>,
    val inProgress: List<LoggedDrink>,
    val graph: DayGraph,
)

class TodayViewModel(
    sources: SourceStore,
    private val drinks: DrinkStore,
    checkIns: CheckInStore,
    settings: SettingsStore,
    private val clock: Clock,
    ticks: Flow<Unit> = minuteTicks(),
) : ViewModel() {
    /** Null until the first values arrive. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<TodayState?> =
        combine(settings.settings(), ticks) { current, _ -> current }
            .flatMapLatest { current ->
                val zone = clock.zone
                val today = clock.instant().atZone(zone).toLocalDate()
                val (dayStart, dayEnd) = dayBounds(today, zone)
                combine(
                    sources.sources(),
                    drinks.drinksTakenBetween(dayStart - current.carryOver(), dayEnd),
                    drinks.drinksInProgress(),
                    checkIns.checkInsBetween(dayStart, dayEnd),
                ) { allSources, recent, inProgress, todaysCheckIns ->
                    val now = clock.instant()
                    TodayState(
                        now = now,
                        zone = zone,
                        inBody =
                            IntakeModel(current.caffeine, current.theanine)
                                .inBody(recent.map { it.toDrink() }, now),
                        sources = allSources,
                        inProgress = inProgress,
                        graph =
                            dayGraph(
                                today,
                                zone,
                                now,
                                current,
                                recent,
                                todaysCheckIns,
                            ),
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Logs a drink of one serving of [source], starting now. */
    fun logOneServing(source: Source) {
        viewModelScope.launch { drinks.save(source.logged(1.0, clock.instant(), clock.zone)) }
    }

    /** Marks [drink] finished now, or at its start if the clock reads earlier than that. */
    fun finish(drink: LoggedDrink) {
        viewModelScope.launch {
            drinks.save(drink.copy(finish = maxOf(clock.instant(), drink.start)))
        }
    }
}
