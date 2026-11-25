package com.etoile.app.ui.dates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import com.etoile.app.data.Role
import com.etoile.app.data.api.DatePlan
import com.etoile.app.data.api.DateType
import com.etoile.app.data.api.DatesApi
import com.etoile.app.data.api.friendlyMessage
import com.etoile.app.push.AppEvent
import com.etoile.app.ui.components.CenteredLoading
import com.etoile.app.ui.components.EmptyState
import com.etoile.app.ui.components.ErrorText
import com.etoile.app.ui.components.GlassCard
import com.etoile.app.ui.components.Pill
import com.etoile.app.ui.components.ScreenColumn
import com.etoile.app.ui.components.ScreenHeader
import com.etoile.app.ui.components.SegmentedChoice
import com.etoile.app.ui.countdown
import com.etoile.app.ui.rememberNow
import com.etoile.app.ui.theme.Etoile
import com.etoile.app.ui.theme.Muted
import com.etoile.app.ui.whenLabel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DatesListViewModel(private val api: DatesApi, events: Flow<AppEvent>) : ViewModel() {
    var upcoming by mutableStateOf<List<DatePlan>>(emptyList()); private set
    var past by mutableStateOf<List<DatePlan>>(emptyList()); private set
    var loaded by mutableStateOf(false); private set
    var refreshing by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var tab by mutableStateOf(0)    // 0 upcoming, 1 past

    init {
        viewModelScope.launch { events.collect { if (it.dateId != null) refresh() } }
    }

    fun refresh() {
        if (refreshing) return
        refreshing = true
        viewModelScope.launch {
            val up = async { runCatching { api.list("upcoming") } }
            val old = async { runCatching { api.list("past") } }
            up.await().onSuccess { upcoming = it }
            old.await().onSuccess { past = it }
            error = (up.await().exceptionOrNull() ?: old.await().exceptionOrNull())?.friendlyMessage()
            loaded = true
            refreshing = false
        }
    }
}

/** "Our dates" tab: upcoming and past dates, and the way into planning a new one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatesListScreen(vm: DatesListViewModel, me: Role, onOpen: (String) -> Unit, onPlan: () -> Unit) {
    LifecycleResumeEffect(vm) {
        vm.refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) { vm.refresh() }   // also when coming back from a date's page
    val accent = Etoile.accent
    val list = if (vm.tab == 0) vm.upcoming else vm.past

    ScreenColumn {
        ScreenHeader("Our dates", actions = {
            IconButton(onClick = onPlan) { Icon(Icons.Outlined.Add, contentDescription = "Plan a date", tint = accent.main) }
        })
        SegmentedChoice(
            options = listOf("Upcoming", "Past"),
            selected = vm.tab,
            onSelect = { vm.tab = it },
            modifier = Modifier.padding(horizontal = 22.dp),
        )
        ErrorText(vm.error, Modifier.padding(horizontal = 22.dp))
        PullToRefreshBox(isRefreshing = vm.refreshing && vm.loaded, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
            when {
                !vm.loaded -> CenteredLoading()
                list.isEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        if (vm.error != null) EmptyState("Couldn't load your dates", "Pull down to try again.")
                        else if (vm.tab == 0) EmptyState("The sky is clear", "Tap + to plan a date.")
                        else EmptyState("No memories yet", "Dates you've had (or cancelled) show up here.")
                    }
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(list, key = { it.id }) { plan ->
                        DateRow(plan, me, showCountdown = vm.tab == 0, onClick = { onOpen(plan.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DateRow(plan: DatePlan, me: Role, showCountdown: Boolean, onClick: () -> Unit) {
    val accent = Etoile.accent
    val status = statusOf(plan, me)
    val now by rememberNow()
    GlassCard(onClick = onClick, highlighted = status.highlighted && plan.isActive && !status.text.startsWith("Confirmed")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(plan.type.icon(), contentDescription = null, tint = accent.main, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(plan.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Pill(status.text, status.highlighted)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(whenLabel(plan), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            if (showCountdown) {
                Text(countdown(plan.start, plan.end, now), style = MaterialTheme.typography.bodySmall.copy(color = accent.main))
            }
        }
        val place = plan.place?.takeIf { it.isNotBlank() && plan.type == DateType.MEETING }
        if (place != null) {
            Spacer(Modifier.height(2.dp))
            Text(place, style = MaterialTheme.typography.bodySmall.copy(color = Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

fun DateType.icon(): ImageVector = when (this) {
    DateType.MOVIE -> Icons.Outlined.Movie
    DateType.MEETING -> Icons.Outlined.LocalCafe
    DateType.TRIP -> Icons.Outlined.Forest
}
