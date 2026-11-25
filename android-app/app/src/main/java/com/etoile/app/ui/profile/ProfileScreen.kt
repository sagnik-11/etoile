package com.etoile.app.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etoile.app.R
import com.etoile.app.data.AuthRepository
import com.etoile.app.data.Session
import com.etoile.app.data.api.friendlyMessage
import com.etoile.app.ui.components.ConfirmDialog
import com.etoile.app.ui.components.ErrorText
import com.etoile.app.ui.components.EtoileButton
import com.etoile.app.ui.components.EtoileOutlineButton
import com.etoile.app.ui.components.EtoileTextField
import com.etoile.app.ui.components.GlassCard
import com.etoile.app.ui.components.ScreenColumn
import com.etoile.app.ui.components.ScreenHeader
import com.etoile.app.ui.components.SectionLabel
import com.etoile.app.ui.theme.Etoile
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ProfileViewModel(private val auth: AuthRepository) : ViewModel() {
    var current by mutableStateOf("")
    var new by mutableStateOf("")
    var repeat by mutableStateOf("")
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var done by mutableStateOf(false); private set

    fun changePassword() {
        error = when {
            current.isEmpty() -> "Type your current password."
            new.length < 8 -> "The new password needs at least 8 characters."
            new.length > 72 -> "That's too long (72 characters max)."
            new != repeat -> "The two new passwords don't match."
            new == current -> "That's the same as your current password."
            else -> null
        }
        if (error != null || busy) return
        busy = true
        done = false
        viewModelScope.launch {
            runCatching { auth.changePassword(current, new) }
                .onSuccess { current = ""; new = ""; repeat = ""; done = true }
                .onFailure {
                    error = if ((it as? HttpException)?.code() == 401) "Your current password isn't right." else it.friendlyMessage()
                }
            busy = false
        }
    }

    fun logout() {
        viewModelScope.launch { auth.logout() }
    }
}

/** "You" tab: who's signed in, change password, log out. */
@Composable
fun ProfileScreen(session: Session, vm: ProfileViewModel) {
    val accent = Etoile.accent
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    ScreenColumn {
        ScreenHeader("You")
        Column(
            Modifier.weight(1f).fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(96.dp))
            Text(session.displayName, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Signed in as ${session.role}",
                style = MaterialTheme.typography.labelSmall.copy(color = accent.main),
            )
            Spacer(Modifier.height(28.dp))

            GlassCard {
                SectionLabel("Change password")
                EtoileTextField(
                    vm.current, { vm.current = it }, "Current password", isPassword = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                )
                Spacer(Modifier.height(10.dp))
                EtoileTextField(
                    vm.new, { vm.new = it }, "New password (8+ characters)", isPassword = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                )
                Spacer(Modifier.height(10.dp))
                EtoileTextField(
                    vm.repeat, { vm.repeat = it }, "New password again", isPassword = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                )
                ErrorText(vm.error)
                if (vm.done) {
                    Text(
                        "Password changed ✦",
                        style = MaterialTheme.typography.bodySmall.copy(color = accent.main),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                }
                EtoileButton("Change password", onClick = vm::changePassword, loading = vm.busy)
            }

            Spacer(Modifier.height(20.dp))
            EtoileOutlineButton("Log out", onClick = { confirmLogout = true })
            Spacer(Modifier.height(32.dp))
        }
    }

    if (confirmLogout) ConfirmDialog(
        title = "Log out?",
        text = "This phone stops getting Étoile notifications until you sign in again.",
        confirmLabel = "Log out",
        onConfirm = { confirmLogout = false; vm.logout() },
        onDismiss = { confirmLogout = false },
    )
}
