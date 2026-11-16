package io.github.jwp23.koj.form

import io.github.jwp23.koj.intake.IntakeStyle
import io.github.jwp23.koj.record.Source
import java.time.Duration
import java.time.LocalTime

/** The fields of [SourceForm] that can be wrong. */
enum class SourceField {
    NAME,
    CAFFEINE,
    THEANINE,
    DURATION,
}

/**
 * What the user has entered for a source. Amounts and the sipping time are text as typed; the usual
 * time is chosen in a picker, so it is a time and cannot be badly written.
 */
data class SourceForm(
    val name: String = "",
    val caffeineMg: String = "",
    val theanineMg: String = "",
    val style: IntakeStyle = IntakeStyle.FAST,
    val durationMinutes: String = "",
    val usualTime: LocalTime? = null,
) {
    /** The fields that cannot be saved as typed. Empty when the form is valid. */
    fun errors(): Set<SourceField> = buildSet {
        if (name.isBlank()) add(SourceField.NAME)
        if (amount(caffeineMg) == null) add(SourceField.CAFFEINE)
        if (amount(theanineMg) == null) add(SourceField.THEANINE)
        if (style == IntakeStyle.SIPPED && minutes() == null) add(SourceField.DURATION)
    }

    /** The source this form describes. Call only when [errors] is empty. */
    fun toSource(id: Long = 0): Source =
        Source(
            id = id,
            name = name.trim(),
            caffeineMgPerServing = amount(caffeineMg)!!,
            theanineMgPerServing = amount(theanineMg)!!,
            style = style,
            defaultDuration =
                if (style == IntakeStyle.SIPPED) Duration.ofMinutes(minutes()!!) else Duration.ZERO,
            usualTime = usualTime,
        )

    /** A blank amount is 0; null when the text is not an amount. */
    private fun amount(text: String): Double? = if (text.isBlank()) 0.0 else parseDecimal(text)

    private fun minutes(): Long? =
        durationMinutes
            .trim()
            .takeIf { it.all(Char::isAsciiDigit) }
            ?.toLongOrNull()
            ?.takeIf { it >= 1 }
}

private fun Char.isAsciiDigit() = this in '0'..'9'

fun Source.toForm(): SourceForm =
    SourceForm(
        name = name,
        caffeineMg = formatDecimal(caffeineMgPerServing),
        theanineMg = formatDecimal(theanineMgPerServing),
        style = style,
        durationMinutes =
            if (style == IntakeStyle.SIPPED) defaultDuration.toMinutes().toString() else "",
        usualTime = usualTime,
    )
