package io.github.jwp23.koj.intake

import java.time.Duration
import kotlin.math.exp
import kotlin.math.ln

/**
 * How quickly one substance enters and leaves the body.
 *
 * A dose is absorbed and eliminated at rates proportional to what remains: a one-compartment model
 * with first-order absorption and first-order elimination.
 */
data class Kinetics(val absorptionHalfLife: Duration, val eliminationHalfLife: Duration) {
    init {
        require(absorptionHalfLife > Duration.ZERO) {
            "Absorption half-life must be positive, was $absorptionHalfLife"
        }
        require(eliminationHalfLife > Duration.ZERO) {
            "Elimination half-life must be positive, was $eliminationHalfLife"
        }
    }

    /**
     * The amount in the body, in the unit of [dose], once [elapsed] has passed since a single dose.
     * Nothing is in the body before the dose is taken.
     */
    fun amountInBody(dose: Double, elapsed: Duration): Double {
        if (elapsed.isNegative) return 0.0
        val minutes = elapsed.inMinutes()
        val absorptionRate = ln(2.0) / absorptionHalfLife.inMinutes()
        // The general formula divides by the difference between the two rates, so equal
        // half-lives need the form the formula approaches as the rates converge.
        if (absorptionHalfLife == eliminationHalfLife) {
            return dose * absorptionRate * minutes * exp(-absorptionRate * minutes)
        }
        val eliminationRate = ln(2.0) / eliminationHalfLife.inMinutes()
        return dose * absorptionRate / (absorptionRate - eliminationRate) *
            (exp(-eliminationRate * minutes) - exp(-absorptionRate * minutes))
    }
}

private const val MILLIS_PER_MINUTE = 60_000.0

private fun Duration.inMinutes(): Double = toMillis() / MILLIS_PER_MINUTE
