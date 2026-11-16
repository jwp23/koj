package io.github.jwp23.koj.record

import io.github.jwp23.koj.intake.DefaultKinetics
import io.github.jwp23.koj.intake.Kinetics
import java.time.Duration
import java.time.LocalTime

/**
 * The user's settings.
 *
 * The defaults are what a fresh install starts with. The check-in defaults (45 minutes and 3 hours
 * after a drink, a cap of 6 a day) come from docs/designs/reminders.md; the morning, quiet, and bed
 * times and the size of a sip come from docs/designs/app-overview.md.
 */
data class Settings(
    val caffeine: Kinetics = DefaultKinetics.CAFFEINE,
    val theanine: Kinetics = DefaultKinetics.L_THEANINE,
    val afterDrinkCheckIns: List<Duration> = listOf(Duration.ofMinutes(45), Duration.ofHours(3)),
    val fixedCheckInTimes: List<LocalTime> = emptyList(),
    val dailyCheckInCap: Int = 6,
    val morningTime: LocalTime = LocalTime.of(8, 0),
    val quietTime: LocalTime = LocalTime.of(20, 0),
    val bedtime: LocalTime = LocalTime.of(21, 0),
    val sipServings: Double = 0.1,
) {
    init {
        require(dailyCheckInCap >= 0) {
            "Daily check-in cap must not be negative, was $dailyCheckInCap"
        }
        afterDrinkCheckIns.forEach {
            require(it.isPositive) { "After-drink check-in delay must be above zero, was $it" }
        }
        require(sipServings.isFinite() && sipServings > 0.0) {
            "Sip must be finite and above zero, was $sipServings"
        }
    }
}
