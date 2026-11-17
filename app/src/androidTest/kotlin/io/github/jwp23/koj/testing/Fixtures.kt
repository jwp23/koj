package io.github.jwp23.koj.testing

import io.github.jwp23.koj.intake.IntakeStyle
import io.github.jwp23.koj.record.Source
import java.time.Duration

/** A fast source, drunk in one go. */
val espresso =
    Source(
        name = "Espresso",
        caffeineMgPerServing = 80.0,
        theanineMgPerServing = 0.0,
        style = IntakeStyle.FAST,
    )

/** A sipped source that takes an hour. */
val greenTea =
    Source(
        name = "Green tea",
        caffeineMgPerServing = 30.0,
        theanineMgPerServing = 20.0,
        style = IntakeStyle.SIPPED,
        defaultDuration = Duration.ofMinutes(60),
    )
