package com.etoile.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.etoile.app.data.Role
import com.etoile.app.data.api.DatePlan
import com.etoile.app.data.api.DateType
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Time and wording helpers shared by all screens. Times show in the phone's own time zone. */

private val dayFormat = DateTimeFormatter.ofPattern("EEEE · d MMM", Locale.ENGLISH)
private val whenFormat = DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a", Locale.ENGLISH)
private val shortFormat = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

private fun String.lowerAmPm() = replace("AM", "am").replace("PM", "pm")

fun Instant.local(): ZonedDateTime = atZone(ZoneId.systemDefault())

fun todayLabel(): String = LocalDate.now().format(dayFormat).uppercase(Locale.ENGLISH)

fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}

/** "Wed, 26 Nov · 7:30 pm" */
fun formatWhen(instant: Instant): String = instant.local().format(whenFormat).lowerAmPm()

fun formatTime(instant: Instant): String = instant.local().format(timeFormat).lowerAmPm()

fun formatShortDate(instant: Instant): String = instant.local().format(shortFormat)

/** A date's time for cards. Trips over several days show "Fri, 28 Nov – Sun, 30 Nov". */
fun whenLabel(plan: DatePlan): String {
    val start = plan.start.local()
    val end = plan.end?.local()
    return if (plan.type == DateType.TRIP && end != null && end.toLocalDate() != start.toLocalDate())
        "${start.format(shortFormat)} – ${end.format(shortFormat)}"
    else formatWhen(plan.start)
}

/** "in 2d 4h", "in 3h 20m", "in 12 min", or "happening now". */
fun countdown(start: Instant, end: Instant?, now: Instant): String {
    if (!now.isBefore(start)) return if (end != null && now.isBefore(end)) "happening now" else "starting now"
    val totalMinutes = Duration.between(now, start).toMinutes().coerceAtLeast(1)
    val days = totalMinutes / (60 * 24)
    val hours = (totalMinutes / 60) % 24
    val minutes = totalMinutes % 60
    return when {
        days > 0 -> "in ${days}d ${hours}h"
        hours > 0 -> "in ${hours}h ${minutes}m"
        else -> "in $minutes min"
    }
}

/** The current time, updated every 20 seconds so countdowns stay live. */
@Composable
fun rememberNow(): State<Instant> = produceState(Instant.now()) {
    while (true) {
        delay(20_000)
        value = Instant.now()
    }
}

/** "her" / "him" for the other person. */
fun Role.pronoun(): String = if (this == Role.HER) "her" else "him"

/** "Her" / "His". */
fun Role.possessive(): String = if (this == Role.HER) "Her" else "His"

/** "She" / "He" for the other person. */
fun Role.subject(): String = if (this == Role.HER) "She" else "He"

fun DateType.label(): String = when (this) {
    DateType.MOVIE -> "Movie"
    DateType.MEETING -> "Meeting"
    DateType.TRIP -> "Trip"
}
