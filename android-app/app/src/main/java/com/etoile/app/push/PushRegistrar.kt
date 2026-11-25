package com.etoile.app.push

import android.content.Context
import android.util.Log
import com.etoile.app.data.api.DeviceRequest
import com.etoile.app.data.api.NotificationsApi
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Gets this phone's push address (FCM token) and tells notification-service about it. */
class PushRegistrar(private val context: Context, private val api: NotificationsApi) {

    /** False when app/google-services.json wasn't added: the app works, just without pushes. */
    val available: Boolean get() = FirebaseApp.getApps(context).isNotEmpty()

    suspend fun register() {
        val token = currentToken() ?: return
        runCatching { api.register(DeviceRequest(token)) }
            .onFailure { Log.w(TAG, "Couldn't register for pushes: ${it.message}") }
    }

    /** Called with a new token from Firebase (it changes now and then). */
    suspend fun register(token: String) {
        runCatching { api.register(DeviceRequest(token)) }
    }

    suspend fun unregister() {
        val token = currentToken() ?: return
        api.unregister(DeviceRequest(token))
    }

    private suspend fun currentToken(): String? {
        if (!available) return null
        return suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                cont.resume(if (task.isSuccessful) task.result else null)
            }
        }
    }

    private companion object {
        const val TAG = "Push"
    }
}
