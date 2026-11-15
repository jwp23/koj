package io.github.jwp23.koj.intake

import java.time.Duration

/**
 * Starting values for the half-life settings, taken from published studies of healthy adults. They
 * are population averages; the user is expected to adjust them.
 */
object DefaultKinetics {
    val CAFFEINE =
        Kinetics(
            // "Caffeine is rapidly and completely absorbed in humans, with 99 percent being
            // absorbed within 45 minutes of ingestion." With first-order absorption, 99 percent
            // absorbed means 1 percent left, which takes log2(100) = 6.64 half-lives, so one
            // absorption half-life is 45 / 6.64 = 6.8 minutes, rounded to 7. This is for a
            // drink; capsules are absorbed several times more slowly.
            // Institute of Medicine (2001), Caffeine for the Sustainment of Mental Task
            // Performance, chapter 2. https://www.ncbi.nlm.nih.gov/books/NBK223808/
            absorptionHalfLife = Duration.ofMinutes(7),
            // "The mean half-life of caffeine in plasma of healthy individuals is about 5
            // hours. However, caffeine's elimination half-life may range between 1.5 and 9.5
            // hours." Same source.
            eliminationHalfLife = Duration.ofHours(5),
        )

    val L_THEANINE =
        Kinetics(
            // "half-lives of absorption and elimination were approximately 15 and 65 min
            // respectively." The same study reports a lag of about 10 minutes before
            // absorption starts, which the model leaves out.
            // van der Pijl, Chen and Mulder (2010), Human disposition of L-theanine in tea or
            // aqueous solution, Journal of Functional Foods 2(4):239-244.
            // https://doi.org/10.1016/j.jff.2010.08.001
            absorptionHalfLife = Duration.ofMinutes(15),
            eliminationHalfLife = Duration.ofMinutes(65),
        )
}
