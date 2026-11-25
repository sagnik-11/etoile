package com.etoile.app.ui.main

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.etoile.app.appContainer
import com.etoile.app.data.Session
import com.etoile.app.push.Notifications
import com.etoile.app.ui.components.StarrySky
import com.etoile.app.ui.dates.DateDetailScreen
import com.etoile.app.ui.dates.DateDetailViewModel
import com.etoile.app.ui.dates.DatesListScreen
import com.etoile.app.ui.dates.DatesListViewModel
import com.etoile.app.ui.dates.PlanDateScreen
import com.etoile.app.ui.dates.PlanDateViewModel
import com.etoile.app.ui.home.HomeScreen
import com.etoile.app.ui.home.HomeViewModel
import com.etoile.app.ui.love.LoveScreen
import com.etoile.app.ui.love.LoveViewModel
import com.etoile.app.ui.profile.ProfileScreen
import com.etoile.app.ui.profile.ProfileViewModel
import com.etoile.app.ui.theme.Dim
import com.etoile.app.ui.theme.Etoile
import com.etoile.app.ui.theme.Hairline
import com.etoile.app.ui.theme.Navy
import kotlinx.serialization.Serializable

/** The four tabs in the bottom bar. */
enum class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME("Home", Icons.Outlined.Home, Icons.Filled.Home),
    DATES("Dates", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    LOVE("Love", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
    PROFILE("You", Icons.Outlined.Person, Icons.Filled.Person),
}

// Pages you can go to (the back button returns from them)
@Serializable private data object TabsRoute
@Serializable private data object PlanRoute
@Serializable private data class DateRoute(val id: String)

/** Everything after login: the tabs, plus the "plan a date" and date pages on top of them. */
@Composable
fun MainScreen(session: Session) {
    val container = LocalContext.current.appContainer()
    val nav = rememberNavController()
    val me = session.role
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }

    // Keyed by user, so logging in as the other person never shows old data
    val uid = session.userId
    val homeVm: HomeViewModel = viewModel(key = "home-$uid") {
        HomeViewModel(container.network.dates, container.network.love, container.auth, container.events)
    }
    val datesVm: DatesListViewModel = viewModel(key = "dates-$uid") {
        DatesListViewModel(container.network.dates, container.events)
    }
    val loveVm: LoveViewModel = viewModel(key = "love-$uid") {
        LoveViewModel(
            container.network.love, container.events,
            onSeen = { container.clearLoveNotification(); homeVm.refresh() },
            appScope = container.appScope,
        )
    }
    val profileVm: ProfileViewModel = viewModel(key = "profile-$uid") { ProfileViewModel(container.auth) }

    // A tapped notification: open its date, or the love tab
    val link by container.deepLink.collectAsStateWithLifecycle()
    LaunchedEffect(link) {
        val l = link ?: return@LaunchedEffect
        container.deepLink.value = null
        when {
            l.isLove -> { nav.popBackStack(TabsRoute, inclusive = false); tab = Tab.LOVE }
            l.dateId != null -> nav.navigate(DateRoute(l.dateId)) { launchSingleTop = true }
        }
    }

    AskForNotifications()

    StarrySky {
        NavHost(
            navController = nav,
            startDestination = TabsRoute,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            composable<TabsRoute> {
                BackHandler(enabled = tab != Tab.HOME) { tab = Tab.HOME }   // back goes Home first
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        Crossfade(targetState = tab, label = "tab") { current ->
                            when (current) {
                                Tab.HOME -> HomeScreen(
                                    session = session,
                                    vm = homeVm,
                                    onPlanDate = { nav.navigate(PlanRoute) },
                                    onOurDates = { tab = Tab.DATES },
                                    onOpenDate = { nav.navigate(DateRoute(it)) },
                                    onLove = { tab = Tab.LOVE },
                                )
                                Tab.DATES -> DatesListScreen(
                                    vm = datesVm,
                                    me = me,
                                    onOpen = { nav.navigate(DateRoute(it)) },
                                    onPlan = { nav.navigate(PlanRoute) },
                                )
                                Tab.LOVE -> LoveScreen(loveVm, me)
                                Tab.PROFILE -> ProfileScreen(session, profileVm)
                            }
                        }
                    }
                    val love = homeVm.loveStatus
                    BottomBar(
                        selected = tab,
                        loveDot = tab != Tab.LOVE && (love.theyNeedYou || love.loveReceived > 0),
                        onSelect = { tab = it },
                    )
                }
            }
            composable<PlanRoute> {
                val vm: PlanDateViewModel = viewModel { PlanDateViewModel(container.network.dates) }
                PlanDateScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onCreated = { id ->
                        homeVm.refresh()
                        nav.navigate(DateRoute(id)) { popUpTo<PlanRoute> { inclusive = true } }
                    },
                )
            }
            composable<DateRoute> { entry ->
                val id = entry.toRoute<DateRoute>().id
                val vm: DateDetailViewModel = viewModel {
                    DateDetailViewModel(container.network.dates, id, container::markDateSeen, container.events)
                }
                DateDetailScreen(vm, me, onBack = { homeVm.refresh(); nav.popBackStack() })
            }
        }
    }
}

/** Android 13+ asks before an app may show notifications. Asked once after login. */
@Composable
private fun AskForNotifications() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifications.canPost(context)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun BottomBar(selected: Tab, loveDot: Boolean, onSelect: (Tab) -> Unit) {
    val accent = Etoile.accent
    Column(
        Modifier.fillMaxWidth().background(Navy.copy(alpha = 0.92f)).navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalDivider(thickness = 0.5.dp, color = Hairline)
        Row(
            Modifier.fillMaxWidth().height(60.dp).widthIn(max = 520.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { t ->
                val isSelected = t == selected
                val color = if (isSelected) accent.main else Dim
                Column(
                    Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,           // no grey ripple; the colour change is enough
                        ) { onSelect(t) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        Icon(if (isSelected) t.selectedIcon else t.icon, contentDescription = t.label, tint = color, modifier = Modifier.size(22.dp))
                        if (t == Tab.LOVE && loveDot) {
                            Box(
                                Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-1).dp)
                                    .size(6.dp).background(accent.main, CircleShape),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(t.label, style = MaterialTheme.typography.labelSmall.copy(color = color, letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 0.3f))
                }
            }
        }
    }
}
