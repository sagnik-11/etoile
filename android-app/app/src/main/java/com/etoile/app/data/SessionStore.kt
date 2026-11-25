package com.etoile.app.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

private val Context.dataStore by preferencesDataStore(name = "session")

/** Saves the login on the phone (private app storage). */
class SessionStore(private val context: Context) {

    private object Keys {
        val token = stringPreferencesKey("token")
        val userId = stringPreferencesKey("user_id")
        val name = stringPreferencesKey("display_name")
        val role = stringPreferencesKey("role")
        val expiresAt = longPreferencesKey("expires_at")
    }

    /** The saved session, or null when logged out or the token has expired. */
    val session: Flow<Session?> = context.dataStore.data.map { p ->
        val token = p[Keys.token] ?: return@map null
        Session(
            token = token,
            userId = p[Keys.userId] ?: return@map null,
            displayName = p[Keys.name] ?: return@map null,
            role = p[Keys.role]?.let { runCatching { Role.valueOf(it) }.getOrNull() } ?: return@map null,
            expiresAt = p[Keys.expiresAt] ?: 0L,
        ).takeUnless { it.isExpired() }
    }

    suspend fun save(session: Session) {
        context.dataStore.edit { p ->
            p[Keys.token] = session.token
            p[Keys.userId] = session.userId
            p[Keys.name] = session.displayName
            p[Keys.role] = session.role.name
            p[Keys.expiresAt] = session.expiresAt
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    companion object {
        /** Reads "exp" from the middle part of a JWT. Returns 0 if it can't be read. */
        fun expiryOf(token: String): Long = runCatching {
            val payload = token.split(".")[1]
            val json = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
            Json.parseToJsonElement(json).jsonObject["exp"]!!.jsonPrimitive.long
        }.getOrDefault(0L)
    }
}
