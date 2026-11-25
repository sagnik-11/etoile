package com.etoile.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.etoile.app.data.Role
import com.etoile.app.push.Notifications
import com.etoile.app.ui.components.StarrySky
import com.etoile.app.ui.main.MainScreen
import com.etoile.app.ui.login.LoginScreen
import com.etoile.app.ui.theme.EtoileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light status-bar icons on our dark sky
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val container = appContainer()
        if (savedInstanceState == null) openFromNotification(intent)
        setContent {
            val state by container.session.collectAsStateWithLifecycle()
            val role = (state as? SessionState.LoggedIn)?.session?.role
            // Gold for HIM, rose for HER, decided by who is logged in
            EtoileTheme(role = role) {
                Crossfade(targetState = state, label = "session") { s ->
                    when (s) {
                        SessionState.Loading -> WelcomeScreen()
                        SessionState.LoggedOut -> LoginScreen()
                        is SessionState.LoggedIn -> MainScreen(s.session)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openFromNotification(intent)
    }
}

/** Opening the app by tapping a notification: remember where it should go. */
private fun ComponentActivity.openFromNotification(intent: Intent?) {
    val type = intent?.getStringExtra(Notifications.EXTRA_TYPE) ?: return
    appContainer().deepLink.value = DeepLink(type, intent.getStringExtra(Notifications.EXTRA_DATE_ID))
}

/** Shown for a moment while the saved login is read from the phone. */
@Composable
fun WelcomeScreen() {
    StarrySky {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(140.dp),
            )
            Text("Étoile", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(6.dp))
            Text("under the same stars", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Preview(name = "HIM · gold", showSystemUi = true)
@Composable
private fun WelcomeHimPreview() {
    EtoileTheme(role = Role.HIM) { WelcomeScreen() }
}

@Preview(name = "HER · rose", showSystemUi = true)
@Composable
private fun WelcomeHerPreview() {
    EtoileTheme(role = Role.HER) { WelcomeScreen() }
}
