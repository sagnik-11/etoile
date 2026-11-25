package com.etoile.app.ui.dates

import com.etoile.app.data.Role
import com.etoile.app.data.api.DatePlan
import com.etoile.app.data.api.DateStatus
import com.etoile.app.ui.pronoun

/** Where a date stands, from my point of view. [highlighted] = I need to do something. */
data class StatusText(val text: String, val highlighted: Boolean)

fun statusOf(plan: DatePlan, me: Role): StatusText {
    val r = plan.reschedule
    return when {
        plan.status == DateStatus.CANCELLED ->
            StatusText(if (plan.cancelledBy == me) "You cancelled" else "Cancelled by ${plan.cancelledBy?.pronoun() ?: "them"}", false)
        plan.status == DateStatus.REJECTED ->
            StatusText(if (plan.proposedBy == me) "Declined by ${me.other().pronoun()}" else "You declined", false)
        r != null && r.acknowledgedAt == null ->
            if (r.requestedBy == me) StatusText("Asked to reschedule", false) else StatusText("Wants to reschedule", true)
        r != null -> StatusText("Picking a new time", true)
        plan.status == DateStatus.PROPOSED ->
            if (plan.proposedBy == me) StatusText("Waiting for ${me.other().pronoun()}", false) else StatusText("Needs your answer", true)
        plan.hasStarted() -> StatusText("Happening now", true)
        else -> StatusText("Confirmed ✦", true)
    }
}
