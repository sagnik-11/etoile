package com.etoile.app.ui.dates

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.etoile.app.data.Role
import com.etoile.app.data.api.DatePlan
import com.etoile.app.data.api.DateStatus
import com.etoile.app.data.api.DateType
import com.etoile.app.ui.components.CenteredLoading
import com.etoile.app.ui.components.ConfirmDialog
import com.etoile.app.ui.components.DateTimeField
import com.etoile.app.ui.components.ErrorText
import com.etoile.app.ui.components.EtoileButton
import com.etoile.app.ui.components.EtoileOutlineButton
import com.etoile.app.ui.components.GlassCard
import com.etoile.app.ui.components.Pill
import com.etoile.app.ui.components.ScreenColumn
import com.etoile.app.ui.components.ScreenHeader
import com.etoile.app.ui.components.SectionLabel
import com.etoile.app.ui.countdown
import com.etoile.app.ui.label
import com.etoile.app.ui.pronoun
import com.etoile.app.ui.rememberNow
import com.etoile.app.ui.subject
import com.etoile.app.ui.theme.Danger
import com.etoile.app.ui.theme.Etoile
import com.etoile.app.ui.theme.Hairline
import com.etoile.app.ui.theme.Muted
import com.etoile.app.ui.theme.NavyRaised
import com.etoile.app.ui.whenLabel
import java.time.Instant

/** A single date: details, the actions allowed right now, the reschedule flow and the movie section. */
@Composable
fun DateDetailScreen(vm: DateDetailViewModel, me: Role, onBack: () -> Unit) {
    val plan = vm.plan
    ScreenColumn {
        ScreenHeader(
            title = plan?.title ?: "Date",
            subtitle = plan?.type?.label()?.uppercase(),
            onBack = onBack,
        )
        if (plan == null) {
            if (vm.error != null) {
                ErrorText(vm.error, Modifier.padding(horizontal = 22.dp))
                EtoileOutlineButton("Try again", onClick = vm::load, modifier = Modifier.padding(22.dp))
            } else CenteredLoading()
            return@ScreenColumn
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            // An open reschedule comes first, before anything else
            if (plan.reschedule != null && plan.isActive) {
                RescheduleCard(plan, me, vm)
                Spacer(Modifier.height(14.dp))
            }
            DetailsCard(plan, me)
            ErrorText(vm.error)
            Actions(plan, me, vm)
            if (plan.type == DateType.MOVIE) {
                Spacer(Modifier.height(22.dp))
                MovieSection(plan, me, vm)
            }
            Spacer(Modifier.height(32.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

/* ---------- Details ---------- */

@Composable
private fun DetailsCard(plan: DatePlan, me: Role) {
    val accent = Etoile.accent
    val now by rememberNow()
    val uri = LocalUriHandler.current
    val status = statusOf(plan, me)
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(plan.type.icon(), contentDescription = null, tint = accent.main, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (plan.proposedBy == me) "You planned this" else "${me.other().subject()} planned this",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            Pill(status.text, status.highlighted)
        }
        Spacer(Modifier.height(16.dp))
        Text(whenLabel(plan), style = MaterialTheme.typography.titleSmall)
        if (plan.isActive) {
            Spacer(Modifier.height(4.dp))
            Text(countdown(plan.start, plan.end, now), style = MaterialTheme.typography.headlineSmall.copy(color = accent.main))
        }
        val place = plan.place?.takeIf { it.isNotBlank() }
        if (place != null) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Place, contentDescription = null, tint = Muted, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(place, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
        }
        val link = plan.mapsLink?.takeIf { it.isNotBlank() }
        if (link != null) {
            Row(
                Modifier.padding(top = 6.dp).clip(RoundedCornerShape(8.dp))
                    .clickable { runCatching { uri.openUri(link) } }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Open in Maps", style = MaterialTheme.typography.labelLarge.copy(color = accent.main))
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = accent.main, modifier = Modifier.size(14.dp))
            }
        }
        plan.activity?.takeIf { it.isNotBlank() && plan.type == DateType.MEETING }?.let {
            Spacer(Modifier.height(14.dp))
            SectionLabel("The plan")
            Text(it, style = MaterialTheme.typography.bodyLarge)
        }
        plan.itinerary?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Hairline)
            Spacer(Modifier.height(14.dp))
            SectionLabel("Itinerary")
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/* ---------- Actions ---------- */

@Composable
private fun Actions(plan: DatePlan, me: Role, vm: DateDetailViewModel) {
    if (!plan.isActive || plan.hasStarted()) return
    var confirmReject by rememberSaveable { mutableStateOf(false) }
    var confirmReschedule by rememberSaveable { mutableStateOf(false) }
    var sorry by rememberSaveable { mutableStateOf(false) }
    val iProposed = plan.proposedBy == me

    Spacer(Modifier.height(10.dp))
    when (plan.status) {
        DateStatus.PROPOSED -> if (iProposed) {
            Text(
                "Waiting for ${me.other().pronoun()} to answer.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
            EtoileOutlineButton("Withdraw invite", onClick = { sorry = true })
        } else {
            EtoileButton("Accept 🥰", onClick = vm::accept, loading = vm.busy)
            Spacer(Modifier.height(10.dp))
            EtoileOutlineButton("Can't make it", onClick = { confirmReject = true })
        }
        DateStatus.ACCEPTED -> {
            if (plan.reschedule == null) {
                EtoileOutlineButton("Reschedule", onClick = { confirmReschedule = true })
                Spacer(Modifier.height(10.dp))
            }
            CancelLink { sorry = true }
        }
        else -> Unit
    }

    if (confirmReject) ConfirmDialog(
        title = "Decline this ${plan.type.label().lowercase()}?",
        text = "${me.other().subject()} will be told you can't make it.",
        confirmLabel = "Decline",
        onConfirm = { confirmReject = false; vm.reject() },
        onDismiss = { confirmReject = false },
    )
    if (confirmReschedule) ConfirmDialog(
        title = "Ask to reschedule?",
        text = "${me.other().subject()} will keep getting nudged until ${if (me.other() == Role.HER) "she" else "he"} sees it. Then you two talk and pick a new time.",
        confirmLabel = "Ask",
        onConfirm = { confirmReschedule = false; vm.requestReschedule() },
        onDismiss = { confirmReschedule = false },
    )
    if (sorry) SorryDialog(
        withdraw = plan.status == DateStatus.PROPOSED,
        busy = vm.busy,
        onConfirm = { count -> vm.cancel(count) { sorry = false } },
        onDismiss = { sorry = false },
    )
}

@Composable
private fun CancelLink(onClick: () -> Unit) {
    Text(
        "Cancel date",
        style = MaterialTheme.typography.labelLarge.copy(color = Danger.copy(alpha = 0.85f)),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(10.dp),
    )
}

/** Cancelling costs 5 sorrys: tap "Sorry" five times, then confirm. */
@Composable
private fun SorryDialog(withdraw: Boolean, busy: Boolean, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val accent = Etoile.accent
    var count by rememberSaveable { mutableIntStateOf(0) }
    val bump by animateFloatAsState(if (count % 2 == 0) 1f else 1.08f, spring(Spring.DampingRatioMediumBouncy), label = "bump")
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(NavyRaised)
                .border(BorderStroke(0.5.dp, Hairline), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (withdraw) "Withdraw the invite?" else "Cancel the date?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("That'll cost you 5 sorrys 🥺", style = MaterialTheme.typography.bodyMedium.copy(color = Muted))
            Spacer(Modifier.height(22.dp))
            AnimatedContent(targetState = count, label = "sorry") { n ->
                Text(if (n == 0) "…" else "sorry ×$n", style = MaterialTheme.typography.headlineSmall.copy(color = accent.main))
            }
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .scale(bump)
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(accent.soft)
                    .border(BorderStroke(0.5.dp, accent.line), CircleShape)
                    .clickable(enabled = count < 5) { count++ },
                contentAlignment = Alignment.Center,
            ) {
                Text("Sorry", style = MaterialTheme.typography.titleMedium.copy(color = accent.main))
            }
            Spacer(Modifier.height(22.dp))
            EtoileButton(
                if (withdraw) "Withdraw" else "Cancel the date",
                onClick = { onConfirm(count) },
                enabled = count >= 5,
                loading = busy,
            )
            Spacer(Modifier.height(8.dp))
            EtoileOutlineButton("Keep it", onClick = onDismiss)
        }
    }
}

/* ---------- Reschedule ---------- */

@Composable
private fun RescheduleCard(plan: DatePlan, me: Role, vm: DateDetailViewModel) {
    val r = plan.reschedule ?: return
    val iAsked = r.requestedBy == me
    val acknowledged = r.acknowledgedAt != null
    var newStart by rememberSaveable { mutableStateOf<Long?>(null) }
    var newEnd by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmCancel by rememberSaveable { mutableStateOf(false) }
    val other = me.other()

    GlassCard(highlighted = true) {
        SectionLabel("Reschedule")
        Text(
            "We need to talk...",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        )
        when {
            !acknowledged && !iAsked ->
                EtoileButton("Yeah, yeah... coming", onClick = vm::acknowledgeReschedule, loading = vm.busy)
            !acknowledged && iAsked -> Text(
                "Waiting for ${other.pronoun()} to see it…",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            else -> {
                if (iAsked) {
                    Text(
                        "${other.subject()} saw it: \"Yeah, yeah... coming\"",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    )
                }
                Text(
                    "Agreed on a new time? Either of you can set it.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                DateTimeField(
                    if (plan.type == DateType.TRIP) "New start" else "New time",
                    newStart?.let(Instant::ofEpochMilli),
                    onChange = { newStart = it.toEpochMilli() },
                )
                if (plan.type == DateType.TRIP) {
                    Spacer(Modifier.height(10.dp))
                    DateTimeField("New end", newEnd?.let(Instant::ofEpochMilli), onChange = { newEnd = it.toEpochMilli() })
                }
                Spacer(Modifier.height(14.dp))
                val ready = newStart != null && (plan.type != DateType.TRIP || newEnd != null)
                EtoileButton(
                    "Set new time",
                    enabled = ready,
                    loading = vm.busy,
                    onClick = {
                        vm.setNewTime(Instant.ofEpochMilli(newStart!!), newEnd?.let(Instant::ofEpochMilli)) {
                            newStart = null; newEnd = null
                        }
                    },
                )
            }
        }
        if (iAsked) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Never mind, keep the old time",
                style = MaterialTheme.typography.labelLarge.copy(color = Muted),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { confirmCancel = true }.padding(8.dp),
            )
        }
    }

    if (confirmCancel) ConfirmDialog(
        title = "Cancel the reschedule?",
        text = "The date stays on ${whenLabel(plan)}.",
        confirmLabel = "Keep old time",
        onConfirm = { confirmCancel = false; vm.cancelReschedule() },
        onDismiss = { confirmCancel = false },
    )
}
