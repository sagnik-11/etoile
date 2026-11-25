package com.etoile.app.data

import com.etoile.app.data.api.AuthApi
import com.etoile.app.data.api.AuthResponse
import com.etoile.app.data.api.ChangePasswordRequest
import com.etoile.app.data.api.LoginRequest

class AuthRepository(
    private val api: AuthApi,
    private val store: SessionStore,
    /** Runs just before logging out, while the token still works (stops pushes to this phone). */
    private val beforeLogout: suspend () -> Unit = {},
) {

    /** Logs in and saves the session. Throws on failure (wrong password, no internet...). */
    suspend fun login(email: String, password: String): Session =
        save(api.login(LoginRequest(email.trim(), password)))

    /** Changes the password and keeps you logged in with the fresh token. */
    suspend fun changePassword(current: String, new: String): Session =
        save(api.changePassword(ChangePasswordRequest(current, new)))

    suspend fun logout() {
        runCatching { beforeLogout() }   // best effort: never block logging out
        store.clear()
    }

    private suspend fun save(response: AuthResponse): Session {
        val session = Session(
            token = response.token,
            userId = response.userId,
            displayName = response.displayName,
            role = response.role,
            expiresAt = SessionStore.expiryOf(response.token),
        )
        store.save(session)
        return session
    }
}
