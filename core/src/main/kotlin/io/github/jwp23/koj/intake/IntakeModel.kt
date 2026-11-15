package io.github.jwp23.koj.intake

import java.time.Duration
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.max

/**
 * Estimates how much caffeine and L-theanine is in the body from the logged drinks.
 *
 * A sipped drink is treated as equal small doses, one for each [sipDoseSpacing] of sipping.
 */
class IntakeModel(
    private val caffeine: Kinetics,
    private val theanine: Kinetics,
    private val sipDoseSpacing: Duration = Duration.ofMinutes(1),
) {
    init {
        require(sipDoseSpacing > Duration.ZERO) {
            "Sip dose spacing must be positive, was $sipDoseSpacing"
        }
        require(sipDoseSpacing.toMillis() > 0) {
            "Sip dose spacing must be at least one millisecond, was $sipDoseSpacing"
        }
    }

    /**
     * The amounts in the body at [at]. [drinks] must include drinks from earlier days whose
     * contents have not cleared.
     */
    fun inBody(drinks: List<Drink>, at: Instant): Amounts =
        Amounts(
            caffeineMg = drinks.sumOf { inBody(it.caffeineMg, it, caffeine, at) },
            theanineMg = drinks.sumOf { inBody(it.theanineMg, it, theanine, at) },
        )

    private fun inBody(amountMg: Double, drink: Drink, kinetics: Kinetics, at: Instant): Double {
        val doseTimes = doseTimes(drink)
        val doseMg = amountMg / doseTimes.size
        return doseTimes.sumOf { kinetics.amountInBody(doseMg, Duration.between(it, at)) }
    }

    /** The moments at which the drink's equal doses are taken. */
    private fun doseTimes(drink: Drink): List<Instant> {
        if (drink.style == IntakeStyle.FAST) return listOf(drink.start)
        val finish = drink.finish ?: drink.start.plus(drink.defaultDuration)
        val sipping = Duration.between(drink.start, finish)
        val doses = max(1, ceil(sipping.toMillis().toDouble() / sipDoseSpacing.toMillis()).toInt())
        // Each dose sits at the middle of its equal share of the sipping time.
        return (0 until doses).map { index ->
            drink.start.plusMillis(sipping.toMillis() * (2 * index + 1) / (2 * doses))
        }
    }
}
