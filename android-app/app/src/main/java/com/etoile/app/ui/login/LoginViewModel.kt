package com.etoile.app.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etoile.app.data.AuthRepository
import com.etoile.app.data.api.friendlyMessage
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Holds what the login screen shows, and survives screen rotation. */
class LoginViewModel(private val auth: AuthRepository) : ViewModel() {

    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !loading

    fun onEmailChange(value: String) { email = value; error = null }
    fun onPasswordChange(value: String) { password = value; error = null }

    /** On success the saved session changes and the app moves to Home by itself. */
    fun submit() {
        if (!canSubmit) return
        loading = true
        error = null
        viewModelScope.launch {
            try {
                auth.login(email, password)
            } catch (e: Exception) {
                error = if (e is HttpException && e.code() == 401) "That email or password doesn't match."
                else e.friendlyMessage()
            } finally {
                loading = false
            }
        }
    }
}
