package com.etoile.app.ui.love

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etoile.app.data.Role
import com.etoile.app.data.api.LoveApi
import com.etoile.app.data.api.LoveStatus
import com.etoile.app.data.api.SendLoveRequest
import com.etoile.app.data.api.friendlyMessage
import com.etoile.app.push.AppEvent
import com.etoile.app.ui.components.ErrorText
import com.etoile.app.ui.components.EtoileOutlineButton
import com.etoile.app.ui.components.GlassCard
import com.etoile.app.ui.components.ScreenColumn
import com.etoile.app.ui.components.ScreenHeader
import com.etoile.app.ui.pronoun
import com.etoile.app.ui.subject
import com.etoile.app.ui.theme.Etoile
import com.etoile.app.ui.theme.Muted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val BURST_PAUSE_MS = 1200L   // taps closer together than this are sent as one burst
private const val MAX_TAPS_PER_SEND = 100  // server limit per request

class LoveViewModel(
    private val api: LoveApi,
    events: Flow<AppEvent>,
    private val onSeen: () -> Unit,     // clears the notification and refreshes the home badge
    private val appScope: CoroutineScope, // outlives this screen, for taps still waiting when you leave
) : ViewModel() {

    /** What was waiting for you when you opened the tab (kept on screen after it's marked seen). */
    var received by mutableStateOf(LoveStatus()); private set
    var youAreWaiting by mutableStateOf(false); private set
    var pendingTaps by mutableIntStateOf(0); private set
    var sentThisVisit by mutableIntStateOf(0); private set
    var needBusy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    private var burstJob: Job? = null

    init {
        viewModelScope.launch {
            events.collect { if (it.type == "LOVE" || it.type == "NEED_LOVE") open() }
        }
    }

    /** Shows what's new, then marks it seen (highlight off, Need-love reminders stop). */
    fun open() {
        viewModelScope.launch {
            runCatching { api.status() }.onSuccess { s ->
                if (s.loveReceived > 0 || s.theyNeedYou) received = s
                youAreWaiting = s.youAreWaiting
            }
            runCatching { api.seen() }.onSuccess { youAreWaiting = it.youAreWaiting }
            onSeen()
        }
    }

    /** One heart tap. Quick taps are grouped and sent as one burst after a short pause. */
    fun tap() {
        pendingTaps++
        burstJob?.cancel()
        if (pendingTaps >= MAX_TAPS_PER_SEND) {
            sendBurst()
        } else {
            burstJob = viewModelScope.launch {
                delay(BURST_PAUSE_MS)
                sendBurst()
            }
        }
    }

    private fun sendBurst() {
        val taps = pendingTaps
        if (taps == 0) return
        pendingTaps = 0
        viewModelScope.launch {
            runCatching { api.send(SendLoveRequest(taps)) }
                .onSuccess { sentThisVisit += taps; error = null }
                .onFailure { error = it.friendlyMessage(); pendingTaps += taps }
        }
    }

    fun needLove() {
        if (needBusy) return
        needBusy = true
        viewModelScope.launch {
            runCatching { api.need() }
                .onSuccess { youAreWaiting = true; error = null }
                .onFailure { error = it.friendlyMessage() }
            needBusy = false
        }
    }

    override fun onCleared() {
        // Leaving with taps still waiting: send them anyway
        if (pendingTaps > 0) {
            val taps = pendingTaps
            appScope.launch { runCatching { api.send(SendLoveRequest(taps)) } }
        }
    }
}

/** Love tab: a big heart to tap (as many times as you like), and "Need love". */
@Composable
fun LoveScreen(vm: LoveViewModel, me: Role) {
    val accent = Etoile.accent
    val other = me.other()
    val haptics = LocalHapticFeedback.current
    val bursts = remember { mutableStateListOf<FloatingHeartSpec>() }
    var pressCount by remember { mutableIntStateOf(0) }
    val pop by animateFloatAsState(
        targetValue = if (pressCount % 2 == 0) 1f else 1.12f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "pop",
    )

    LaunchedEffect(Unit) { vm.open() }   // each time the tab opens = love seen

    ScreenColumn {
        ScreenHeader("Love")
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val r = vm.received
            if (r.theyNeedYou) {
                GlassCard(highlighted = true) {
                    Text("${other.subject()} is missing you 🥺", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Send some love?", style = MaterialTheme.typography.bodyMedium.copy(color = Muted))
                }
                Spacer(Modifier.height(10.dp))
            }
            if (r.loveReceived > 0) {
                Text(
                    "${other.subject()} sent you love 💗 ×${r.loveReceived}",
                    style = MaterialTheme.typography.bodyLarge.copy(color = accent.main),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            Spacer(Modifier.height(36.dp))
            // ----- The heart -----
            Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                bursts.forEach { spec -> key(spec.id) { FloatingHeart(spec) { bursts.remove(spec) } } }
                Box(
                    Modifier
                        .size(150.dp)
                        .graphicsLayer { scaleX = pop; scaleY = pop }
                        .background(accent.soft, CircleShape)
                        .border(BorderStroke(0.5.dp, accent.line), CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            pressCount++
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            bursts.add(FloatingHeartSpec(id = System.nanoTime(), dx = Random.nextInt(-70, 70).toFloat()))
                            vm.tap()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Send love", tint = accent.main, modifier = Modifier.size(64.dp))
                }
            }
            val total = vm.sentThisVisit + vm.pendingTaps
            AnimatedVisibility(visible = total > 0, enter = fadeIn(), exit = fadeOut()) {
                Text("×$total", style = MaterialTheme.typography.headlineSmall.copy(color = accent.main))
            }
            Text(
                if (total == 0) "Tap the heart. As many times as you want." else if (vm.pendingTaps > 0) "sending…" else "sent 💗",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(48.dp))
            // ----- Need love -----
            Column(Modifier.widthIn(max = 360.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                EtoileOutlineButton(
                    if (vm.youAreWaiting) "Need love, again 🥺" else "Need love 🥺",
                    onClick = vm::needLove,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (vm.youAreWaiting) "Waiting for ${other.pronoun()} to see it…" else "${other.subject()} gets a nudge until ${if (other == Role.HER) "she" else "he"} opens it.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
            ErrorText(vm.error)
            Spacer(Modifier.height(32.dp))
        }
    }
}

data class FloatingHeartSpec(val id: Long, val dx: Float)

/** A small heart that floats up from the big one and fades away. */
@Composable
private fun FloatingHeart(spec: FloatingHeartSpec, onDone: () -> Unit) {
    val accent = Etoile.accent
    val progress = remember { Animatable(0f) }
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
        onDone()
    }
    Icon(
        Icons.Filled.Favorite,
        contentDescription = null,
        tint = accent.star,
        modifier = Modifier
            .size(22.dp)
            .graphicsLayer {
                val p = progress.value
                translationX = with(density) { (spec.dx * p).dp.toPx() }
                translationY = with(density) { (-120 * p).dp.toPx() }
                alpha = 1f - p
                scaleX = 0.7f + 0.6f * p
                scaleY = 0.7f + 0.6f * p
            },
    )
}
