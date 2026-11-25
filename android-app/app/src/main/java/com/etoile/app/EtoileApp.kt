package com.etoile.app

import android.app.Application
import android.content.Context
import com.etoile.app.data.AuthRepository
import com.etoile.app.data.Session
import com.etoile.app.data.SessionStore
import com.etoile.app.data.api.Network
import com.etoile.app.data.api.SeenRequest
import com.etoile.app.push.AppEvent
import com.etoile.app.push.Notifications
import com.etoile.app.push.PushRegistrar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Starts once per app launch and holds the shared objects. */
class EtoileApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannel(this)
        container = AppContainer(this)
    }
}

/** One place that creates and shares the app's objects (simple manual dependency injection). */
class AppContainer(private val context: Context) {
    val appScope = CoroutineScope(SupervisorJob())

    val sessionStore = SessionStore(context)

    /** Current session for the whole app. Starts as [Loading] until the phone storage is read. */
    val session: StateFlow<SessionState> = sessionStore.session
        .map { if (it == null) SessionState.LoggedOut else SessionState.LoggedIn(it) }
        .stateIn(appScope, SharingStarted.Eagerly, SessionState.Loading)

    val network = Network(tokenProvider = { (session.value as? SessionState.LoggedIn)?.session?.token })

    val push = PushRegistrar(context, network.notifications)

    val auth = AuthRepository(network.auth, sessionStore, beforeLogout = { push.unregister() })

    /** Fired when a push arrives while the app is open, so screens reload. */
    val events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 16)

    /** Where a tapped notification wants to go. Cleared once the app has opened it. */
    val deepLink = MutableStateFlow<DeepLink?>(null)

    init {
        // Each time someone logs in (or the app starts logged in), register this phone for pushes
        appScope.launch {
            session.map { (it as? SessionState.LoggedIn)?.session?.userId }
                .distinctUntilChanged()
                .collect { userId -> if (userId != null) push.register() }
        }
    }

    /** Opening a date: clear its notification and reset its merged count on the server. */
    fun markDateSeen(dateId: String) {
        Notifications.cancel(context, "date-$dateId")
        appScope.launch { runCatching { network.notifications.seen(SeenRequest("date-$dateId")) } }
    }

    fun clearLoveNotification() = Notifications.cancel(context, "love")
}

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val session: Session) : SessionState
}

/** A place to open: a date's page, or the love tab. */
data class DeepLink(val type: String?, val dateId: String?) {
    val isLove: Boolean get() = type == "LOVE" || type == "NEED_LOVE"
}

/** Get the container from any screen: `appContainer()` */
fun Context.appContainer(): AppContainer = (applicationContext as EtoileApp).container
