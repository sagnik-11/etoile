package com.etoile.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.etoile.app.data.Role
import com.etoile.app.data.Session
import com.etoile.app.data.api.DatePlan
import com.etoile.app.data.api.DateStatus
import com.etoile.app.data.api.DateType
import com.etoile.app.data.api.LoveStatus
import com.etoile.app.ui.components.Pill
import com.etoile.app.ui.components.StarrySky
import com.etoile.app.ui.countdown
import com.etoile.app.ui.dates.statusOf
import com.etoile.app.ui.greeting
import com.etoile.app.ui.rememberNow
import com.etoile.app.ui.todayLabel
import com.etoile.app.ui.whenLabel
import com.etoile.app.ui.theme.Danger
import com.etoile.app.ui.theme.Dim
import com.etoile.app.ui.theme.Etoile
import com.etoile.app.ui.theme.EtoileTheme
import com.etoile.app.ui.theme.Glass
import com.etoile.app.ui.theme.Hairline
import com.etoile.app.ui.theme.Muted
import java.time.Duration
import java.time.Instant

/** Home tab: greeting, the next date with a live countdown, quick tiles and the love pill. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    session: Session,
    vm: HomeViewModel,
    onPlanDate: () -> Unit,
    onOurDates: () -> Unit,
    onOpenDate: (String) -> Unit,
    onLove: () -> Unit,
) {
    // Reload every time the app comes back to the front
    LifecycleResumeEffect(vm) {
        vm.refresh()
        onPauseOrDispose { }
    }
    PullToRefreshBox(
        isRefreshing = vm.refreshing && vm.loaded,
        onRefresh = vm::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        HomeContent(
            name = session.displayName,
            me = session.role,
            next = vm.next,
            upcomingCount = vm.upcoming.size,
            // Invites the other person sent that I haven't answered yet
            invites = vm.upcoming.filter { it.status == DateStatus.PROPOSED && it.proposedBy != session.role },
            love = vm.loveStatus,
            loaded = vm.loaded,
            error = vm.error,
            onPlanDate = onPlanDate,
            onOurDates = onOurDates,
            onOpenDate = onOpenDate,
            onLove = onLove,
        )
    }
}

@Composable
private fun HomeContent(
    name: String,
    me: Role,
    next: DatePlan?,
    upcomingCount: Int,
    invites: List<DatePlan>,
    love: LoveStatus,
    loaded: Boolean,
    error: String?,
    onPlanDate: () -> Unit,
    onOurDates: () -> Unit,
    onOpenDate: (String) -> Unit,
    onLove: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)              // stays neat on tablets
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(todayLabel(), style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${greeting()},\n$name",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                Spacer(Modifier.width(12.dp))
                LovePill(love, onLove)
            }

            if (error != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "$error  Pull down to try again.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Danger),
                )
            }

            if (invites.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                InvitesBanner(
                    count = invites.size,
                    // One invite opens it straight away; several open the list
                    onClick = { if (invites.size == 1) onOpenDate(invites.first().id) else onOurDates() },
                )
            }

            Spacer(Modifier.height(if (invites.isNotEmpty()) 14.dp else 28.dp))
            NextDateCard(next, me, loaded, failed = error != null, onPlanDate, onOpenDate)

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile(
                    icon = Icons.Outlined.Add,
                    title = "Plan a date",
                    subtitle = "Movie, meeting or trip",
                    onClick = onPlanDate,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                Tile(
                    icon = Icons.Outlined.CalendarMonth,
                    title = "Our dates",
                    subtitle = when {
                        upcomingCount == 0 && error != null -> "Couldn't load"
                        upcomingCount == 0 -> "Nothing coming up"
                        upcomingCount == 1 -> "1 coming up"
                        else -> "$upcomingCount coming up"
                    },
                    onClick = onOurDates,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** "💌 2 invites waiting for your answer", so new invites never hide behind a sooner date. */
@Composable
private fun InvitesBanner(count: Int, onClick: () -> Unit) {
    val accent = Etoile.accent
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accent.soft)
            .border(BorderStroke(0.5.dp, accent.line), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (count == 1) "💌  1 invite waiting for your answer" else "💌  $count invites waiting for your answer",
            style = MaterialTheme.typography.bodyMedium.copy(color = accent.main),
            modifier = Modifier.weight(1f),
        )
        Text("→", style = MaterialTheme.typography.bodyMedium.copy(color = accent.main))
    }
}

/* ---------- Next date card ---------- */

@Composable
private fun NextDateCard(plan: DatePlan?, me: Role, loaded: Boolean, failed: Boolean, onPlanDate: () -> Unit, onOpenDate: (String) -> Unit) {
    val accent = Etoile.accent
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Glass)
            .border(BorderStroke(0.5.dp, accent.line), RoundedCornerShape(22.dp))
            .then(if (plan != null) Modifier.clickable { onOpenDate(plan.id) } else Modifier)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("NEXT UNDER THE STARS", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            if (plan != null) statusOf(plan, me).let { Pill(it.text, it.highlighted) }
        }
        Spacer(Modifier.height(18.dp))

        when {
            plan != null -> NextDateDetails(plan)
            !loaded -> Box(Modifier.fillMaxWidth().heightIn(min = 90.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(22.dp), color = accent.main, strokeWidth = 1.5.dp)
            }
            // Couldn't reach the server: don't pretend there are no dates
            failed -> Column {
                Text("Couldn't load your dates", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("Pull down to try again.", style = MaterialTheme.typography.bodyMedium.copy(color = Muted))
            }
            else -> Column {
                Text("The sky is clear", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("No dates planned yet.", style = MaterialTheme.typography.bodyMedium.copy(color = Muted))
                Spacer(Modifier.height(16.dp))
                Text(
                    "Plan one  →",
                    style = MaterialTheme.typography.labelLarge.copy(color = accent.main),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onPlanDate).padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun NextDateDetails(plan: DatePlan) {
    val accent = Etoile.accent
    val now by rememberNow()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(plan.type.icon(), contentDescription = null, tint = accent.main, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            plan.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (plan.type == DateType.MOVIE && plan.decision?.title == null) {
        Spacer(Modifier.height(4.dp))
        Text("Movie still to choose", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 30.dp))
    }
    Spacer(Modifier.height(12.dp))
    Text(whenLabel(plan), style = MaterialTheme.typography.bodyMedium.copy(color = Muted))
    val place = plan.place?.takeIf { it.isNotBlank() && plan.type != DateType.TRIP }
    if (place != null) {
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Place, contentDescription = null, tint = Dim, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(place, style = MaterialTheme.typography.bodyMedium.copy(color = Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    Spacer(Modifier.height(18.dp))
    HorizontalDivider(thickness = 0.5.dp, color = Hairline)
    Spacer(Modifier.height(16.dp))
    Text(
        countdown(plan.start, plan.end, now),
        style = MaterialTheme.typography.headlineSmall.copy(color = accent.main),
    )
}

/* ---------- Small pieces ---------- */

@Composable
private fun Tile(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = Etoile.accent
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Glass)
            .border(BorderStroke(0.5.dp, Hairline), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = accent.main, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall)
    }
}

/** Heart with ×N love received. Glows when the other person needs love. */
@Composable
private fun LovePill(love: LoveStatus, onClick: () -> Unit) {
    val accent = Etoile.accent
    val active = love.theyNeedYou || love.loveReceived > 0
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (love.theyNeedYou) accent.soft else Glass)
            .border(BorderStroke(0.5.dp, if (active) accent.line else Hairline), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (active) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "Love",
            tint = if (active) accent.main else Muted,
            modifier = Modifier.size(15.dp),
        )
        val label = when {
            love.theyNeedYou -> "needs you"
            love.loveReceived > 0 -> "×${love.loveReceived}"
            love.youAreWaiting -> "waiting"
            else -> null
        }
        if (label != null) {
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.bodySmall.copy(color = if (active) accent.main else Muted))
        }
    }
}

private fun DateType.icon(): ImageVector = when (this) {
    DateType.MOVIE -> Icons.Outlined.Movie
    DateType.MEETING -> Icons.Outlined.LocalCafe
    DateType.TRIP -> Icons.Outlined.Forest
}

/* ---------- Previews ---------- */

private val sampleDate = DatePlan(
    id = "1", type = DateType.MOVIE, status = DateStatus.PROPOSED, proposedBy = Role.HER,
    startsAt = Instant.now().plus(Duration.ofHours(52)).toString(), place = "PVR Mani Square",
)

@Composable
private fun PreviewHome(role: Role, plan: DatePlan?, love: LoveStatus) {
    EtoileTheme(role) {
        StarrySky {
            HomeContent(
                name = if (role == Role.HIM) "Sagnik" else "Preetika", me = role, next = plan,
                upcomingCount = if (plan == null) 0 else 3,
                invites = if (plan == null) emptyList() else listOf(plan, plan), love = love, loaded = true, error = null,
                onPlanDate = {}, onOurDates = {}, onOpenDate = {}, onLove = {},
            )
        }
    }
}

@Preview(name = "HIM · needs answer", showSystemUi = true)
@Composable
private fun HomeHimPreview() = PreviewHome(Role.HIM, sampleDate, LoveStatus(loveReceived = 12))

@Preview(name = "HER · waiting", showSystemUi = true)
@Composable
private fun HomeHerPreview() = PreviewHome(Role.HER, sampleDate.copy(reschedule = null), LoveStatus(theyNeedYou = true))

@Preview(name = "Empty sky", showSystemUi = true)
@Composable
private fun HomeEmptyPreview() = PreviewHome(Role.HIM, null, LoveStatus())
