package com.etoile.app.ui.dates

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.etoile.app.data.Role
import com.etoile.app.data.api.DatePlan
import com.etoile.app.data.api.DateStatus
import com.etoile.app.data.api.Decision
import com.etoile.app.data.api.DecisionMethod
import com.etoile.app.data.api.Suggestion
import com.etoile.app.ui.components.ConfirmDialog
import com.etoile.app.ui.components.EtoileButton
import com.etoile.app.ui.components.EtoileOutlineButton
import com.etoile.app.ui.components.GlassCard
import com.etoile.app.ui.components.Pill
import com.etoile.app.ui.components.SectionLabel
import com.etoile.app.ui.possessive
import com.etoile.app.ui.pronoun
import com.etoile.app.ui.subject
import com.etoile.app.ui.theme.Dim
import com.etoile.app.ui.theme.Etoile
import com.etoile.app.ui.theme.Hairline
import com.etoile.app.ui.theme.Muted
import com.etoile.app.ui.theme.NavyRaised

private const val MAX_SUGGESTIONS_EACH = 2

/** Labels for how the movie got decided. */
private fun DecisionMethod.label(): String = when (this) {
    DecisionMethod.ONLY_ONE -> "The only one in the running"
    DecisionMethod.VOTE -> "You both picked it ✨"
    DecisionMethod.AGREED_NARROWED -> "We talked, we agreed 🤝"
    DecisionMethod.AGREED_OTHER -> "Okay fine, this one 😤"
    DecisionMethod.RANDOM -> "The app has spoken 🎲 It's final."
}

private val Decision.isAgreed get() = method == DecisionMethod.AGREED_NARROWED || method == DecisionMethod.AGREED_OTHER

/**
 * Movie part of a movie date: the shortlist (movies in the running), secret voting,
 * the "time for a call" state after a split vote, the agreed / random decision, and the shelf.
 */
@Composable
fun MovieSection(plan: DatePlan, me: Role, vm: DateDetailViewModel) {
    val decision = plan.decision ?: Decision()
    val voting = plan.voting
    val other = me.other()
    val locked = decision.isFinal
    val open = plan.status == DateStatus.ACCEPTED && !plan.hasStarted() && !locked   // movies can still change
    val decided = decision.movieId != null && decision.method != DecisionMethod.ONLY_ONE
    val canVote = open && decision.movieId == null && !decision.isSplit && plan.shortlist.size >= 2 &&
        !(voting?.voted(me) == true && voting.voted(other))
    val slotsUsed = plan.shortlist.count { it.suggestedBy == me && !(decision.isAgreed && it.id == decision.movieId) }

    var adding by rememberSaveable { mutableStateOf(false) }
    var indecisive by rememberSaveable { mutableStateOf(false) }
    var agreeing by rememberSaveable { mutableStateOf(false) }
    var agreeingNew by rememberSaveable { mutableStateOf(false) }
    var confirmRandom by rememberSaveable { mutableStateOf(false) }
    var showShelf by rememberSaveable { mutableStateOf(false) }

    SectionLabel("The movie")

    // ----- Where the decision stands -----
    when {
        decision.movieId != null -> DecidedCard(decision)
        decision.isSplit -> GlassCard(highlighted = true) {
            Text("Votes split 🤔", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Time for a call 📞 Then record what you agreed on.", style = MaterialTheme.typography.bodyMedium.copy(color = Muted))
            if (open) {
                Spacer(Modifier.height(16.dp))
                EtoileButton("We talked… 🤝", onClick = { agreeing = true }, loading = vm.busy)
                Spacer(Modifier.height(8.dp))
                EtoileOutlineButton("Still stuck? Random pick 🎲", onClick = { confirmRandom = true })
            }
        }
        plan.shortlist.size >= 2 && voting != null -> VotingStatus(me, voting.voted(me), voting.voted(other))
        plan.status == DateStatus.PROPOSED -> Hint("Movies can be added once the date is accepted.")
        plan.shortlist.isEmpty() -> Hint("No movies yet. Suggest one!")
    }
    Spacer(Modifier.height(12.dp))

    // ----- Shortlist -----
    plan.shortlist.forEach { movie ->
        MovieCard(
            movie = movie,
            me = me,
            badges = buildList {
                if (movie.id == decision.movieId) add("The pick ✦" to true)
                if (decision.isSplit && (movie.id == decision.herPickId || movie.id == decision.himPickId)) add("Narrowed" to true)
                if (voting?.voteOf(me) == movie.id) add("Your vote" to true)
                if (voting?.voteOf(other) == movie.id) add("${other.possessive()} vote" to false)
            },
            primary = if (canVote && voting?.voteOf(me) != movie.id) ("Vote" to { vm.vote(movie.id) }) else null,
            secondary = if (open && (movie.suggestedBy == me || (decision.isAgreed && movie.id == decision.movieId)))
                ("Remove" to { vm.shelve(movie.id) }) else null,
        )
        Spacer(Modifier.height(10.dp))
    }

    // ----- Suggest -----
    if (open && plan.status == DateStatus.ACCEPTED) {
        if (slotsUsed < MAX_SUGGESTIONS_EACH) {
            AddLink("Suggest a movie · ${MAX_SUGGESTIONS_EACH - slotsUsed} left") {
                if (decided) indecisive = true else adding = true
            }
        } else Hint("You have 2 movies in the running. Remove one to suggest another.")
    }

    // ----- Shelf -----
    if (plan.shelf.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { showShelf = !showShelf }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ON THE SHELF · ${plan.shelf.size}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Icon(if (showShelf) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, tint = Muted)
        }
        if (showShelf) {
            Spacer(Modifier.height(6.dp))
            plan.shelf.forEach { movie ->
                MovieCard(
                    movie = movie,
                    me = me,
                    badges = emptyList(),
                    faded = true,
                    primary = if (open && slotsUsed < MAX_SUGGESTIONS_EACH) ("Bring back" to { vm.bringBack(movie.id) }) else null,
                    secondary = null,
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    // ----- Dialogs -----
    if (indecisive) ConfirmDialog(
        title = "Indecisive much… 👀",
        text = "You already have a movie. Adding another reopens the vote.",
        confirmLabel = "Yes, add one",
        onConfirm = { indecisive = false; adding = true },
        onDismiss = { indecisive = false },
    )
    if (adding) SuggestionFormDialog(
        heading = "Suggest a movie",
        confirmLabel = "Suggest",
        busy = vm.busy,
        serverError = vm.error,
        onDismiss = { adding = false; vm.clearError() },
        onSubmit = { vm.suggest(it) { adding = false } },
    )
    if (agreeing) AgreeDialog(
        plan = plan,
        busy = vm.busy,
        onPick = { id -> vm.agreeOn(id) { agreeing = false } },
        onNew = { agreeing = false; agreeingNew = true },
        onDismiss = { agreeing = false },
    )
    if (agreeingNew) SuggestionFormDialog(
        heading = "Okay fine, this one 😤",
        confirmLabel = "That's the one",
        busy = vm.busy,
        serverError = vm.error,
        onDismiss = { agreeingNew = false; vm.clearError() },
        onSubmit = { vm.agreeOnNew(it) { agreeingNew = false } },
    )
    if (confirmRandom) {
        val herPick = plan.shortlist.firstOrNull { it.id == decision.herPickId }?.title ?: "Her pick"
        val himPick = plan.shortlist.firstOrNull { it.id == decision.himPickId }?.title ?: "His pick"
        ConfirmDialog(
            title = "Let the app decide? 🎲",
            text = "$herPick (her vote) has a 60% chance, $himPick (his vote) 40%. The result is final, no take-backs.",
            confirmLabel = "Roll it",
            onConfirm = { confirmRandom = false; vm.randomPick() },
            onDismiss = { confirmRandom = false },
        )
    }
}

@Composable
private fun DecidedCard(decision: Decision) {
    val accent = Etoile.accent
    GlassCard(highlighted = true) {
        Text(if (decision.isFinal) "FINAL PICK" else "THE PICK", style = MaterialTheme.typography.labelSmall.copy(color = accent.main))
        Spacer(Modifier.height(6.dp))
        Text(decision.title ?: "", style = MaterialTheme.typography.headlineSmall)
        decision.method?.let {
            Spacer(Modifier.height(4.dp))
            Text(it.label(), style = MaterialTheme.typography.bodyMedium.copy(color = Muted))
        }
    }
}

@Composable
private fun VotingStatus(me: Role, iVoted: Boolean, theyVoted: Boolean) {
    val other = me.other()
    val text = when {
        iVoted && !theyVoted -> "You voted ✓ Waiting for ${other.pronoun()}. You can still change your vote."
        !iVoted && theyVoted -> "${other.subject()} has voted 🤫 Your turn."
        else -> "Vote for one movie. Votes stay secret until you've both voted."
    }
    GlassCard(highlighted = !iVoted) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovieCard(
    movie: Suggestion,
    me: Role,
    badges: List<Pair<String, Boolean>>,
    primary: Pair<String, () -> Unit>?,
    secondary: Pair<String, () -> Unit>?,
    faded: Boolean = false,
) {
    val accent = Etoile.accent
    val uri = LocalUriHandler.current
    var expanded by rememberSaveable(movie.id) { mutableStateOf(false) }
    GlassCard(modifier = Modifier.animateContentSize()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${movie.title} (${movie.releaseYear})",
                    style = MaterialTheme.typography.titleSmall.copy(color = if (faded) Muted else MaterialTheme.typography.titleSmall.color),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (movie.suggestedBy == me) "Your idea" else "${movie.suggestedBy.possessive()} idea",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (!movie.youtubeUrl.isNullOrBlank()) {
                Icon(
                    Icons.Outlined.PlayCircle,
                    contentDescription = "Watch trailer",
                    tint = accent.main,
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(50)).clickable { runCatching { uri.openUri(movie.youtubeUrl) } }.padding(2.dp),
                )
            }
        }
        if (badges.isNotEmpty() || movie.genres.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                badges.forEach { (text, hi) -> Pill(text, highlighted = hi) }
                movie.genres.forEach { Pill(it.label, color = Dim) }
            }
        }
        movie.synopsis?.takeIf { it.isNotBlank() }?.let { synopsis ->
            Spacer(Modifier.height(10.dp))
            Text(
                synopsis,
                style = MaterialTheme.typography.bodyMedium.copy(color = Muted),
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { expanded = !expanded },
            )
        }
        if (primary != null || secondary != null) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                primary?.let { (label, action) -> SmallAction(label, accent.main, action) }
                secondary?.let { (label, action) -> SmallAction(label, Muted, action) }
            }
        }
    }
}

@Composable
private fun SmallAction(label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge.copy(color = color),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(BorderStroke(0.5.dp, color.copy(alpha = 0.4f)), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@Composable
private fun AddLink(text: String, onClick: () -> Unit) {
    val accent = Etoile.accent
    Row(
        Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = accent.main, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelLarge.copy(color = accent.main))
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
}

/** After the call: pick one of the narrowed movies (🤝), any other movie (😤), or a brand-new one. */
@Composable
private fun AgreeDialog(plan: DatePlan, busy: Boolean, onPick: (String) -> Unit, onNew: () -> Unit, onDismiss: () -> Unit) {
    val accent = Etoile.accent
    val d = plan.decision ?: Decision()
    val narrowed = plan.shortlist.filter { it.id == d.herPickId || it.id == d.himPickId }
    val others = (plan.shortlist - narrowed.toSet()) + plan.shelf
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .widthIn(max = 440.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(NavyRaised)
                .border(BorderStroke(0.5.dp, Hairline), RoundedCornerShape(24.dp))
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
        ) {
            Text("What did you agree on?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            SectionLabel("We talked, we agreed 🤝")
            narrowed.forEach { ChoiceRow(it.title, enabled = !busy) { onPick(it.id) } }
            if (others.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SectionLabel("Okay fine, this one 😤")
                others.forEach { ChoiceRow(it.title, enabled = !busy) { onPick(it.id) } }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "+ A different movie",
                style = MaterialTheme.typography.labelLarge.copy(color = accent.main),
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = !busy, onClick = onNew).padding(vertical = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Cancel",
                style = MaterialTheme.typography.labelLarge.copy(color = Muted),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onDismiss).padding(8.dp),
            )
        }
    }
}

@Composable
private fun ChoiceRow(title: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(BorderStroke(0.5.dp, Hairline), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}
