package io.github.jwp23.koj.intake

/** An amount of each tracked substance, either dosed or in the body at one moment. */
data class Amounts(val caffeineMg: Double, val theanineMg: Double) {
    /**
     * L-theanine divided by caffeine, or null when there is no caffeine to divide by.
     *
     * For amounts dosed this is the taken ratio; for amounts in the body it is the in-body ratio.
     */
    val theanineToCaffeineRatio: Double?
        get() = if (caffeineMg == 0.0) null else theanineMg / caffeineMg
}
