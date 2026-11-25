package com.etoile.app.push

import com.etoile.app.SessionState
import com.etoile.app.appContainer
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch

/**
 * Receives pushes from Firebase.
 * - App in the background: Android shows normal pushes by itself; we only get the quiet ones here.
 * - App open: we get everything here, show it ourselves, and tell open screens to refresh.
 */
class EtoilePushService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val container = applicationContext.appContainer()
        if (container.session.value !is SessionState.LoggedIn) return   // registered after login instead
        container.appScope.launch { container.push.register(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val container = applicationContext.appContainer()

        if (data["silent"] == "true") {
            // Love sent again before it was seen: update the text quietly, no buzz
            Notifications.show(
                this, tag = data["tag"] ?: "love",
                title = data["title"].orEmpty(), body = data["body"].orEmpty(),
                data = data, quiet = true,
            )
        } else {
            val n = message.notification
            if (n != null) {
                Notifications.show(
                    this, tag = n.tag ?: data["type"] ?: "etoile",
                    title = n.title.orEmpty(), body = n.body.orEmpty(),
                    data = data, quiet = false,
                )
            }
        }
        container.events.tryEmit(AppEvent(type = data["type"], dateId = data["dateId"]))
    }
}

/** "Something changed on the server" signal, so open screens can reload. */
data class AppEvent(val type: String?, val dateId: String?)
