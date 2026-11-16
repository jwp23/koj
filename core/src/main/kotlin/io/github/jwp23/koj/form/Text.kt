package io.github.jwp23.koj.form

import java.math.BigDecimal
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val decimalPattern = Regex("""\d+\.?\d*|\.\d+""")

/** 24-hour "HH:mm": 07:05 -> "07:05". */
fun formatTime(time: LocalTime): String = time.format(timeFormat)

/**
 * A finite decimal number written with digits and at most one ".", or null. Surrounding spaces are
 * ignored.
 */
fun parseDecimal(text: String): Double? {
    val trimmed = text.trim()
    return if (decimalPattern.matches(trimmed)) trimmed.toDouble().takeIf { it.isFinite() }
    else null
}

/** Without a trailing ".0": 5.0 -> "5", 0.25 -> "0.25", 2.5 -> "2.5". */
fun formatDecimal(value: Double): String =
    BigDecimal(value.toString()).stripTrailingZeros().toPlainString()

/** Milligrams as a whole number, rounded half up: 71.09 -> "71 mg", 0.4 -> "0 mg". */
fun formatMg(mg: Double): String = "${Math.round(mg)} mg"

/** L-theanine to caffeine with two decimals ("0.67"), or "–" when there is no caffeine. */
fun formatRatio(ratio: Double?): String =
    if (ratio == null) "–" else String.format(Locale.ROOT, "%.2f", ratio)
