package com.etoile.app.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.etoile.app.R
import com.etoile.app.appContainer
import com.etoile.app.ui.components.EtoileButton
import com.etoile.app.ui.components.EtoileTextField
import com.etoile.app.ui.components.StarrySky
import com.etoile.app.ui.theme.Danger

@Composable
fun LoginScreen() {
    val container = LocalContext.current.appContainer()
    val vm: LoginViewModel = viewModel { LoginViewModel(container.auth) }
    val focus = LocalFocusManager.current

    StarrySky {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()                       // moves up when the keyboard opens
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 420.dp),   // stays neat on big screens
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(40.dp))
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(110.dp),
                )
                Text("Étoile", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(4.dp))
                Text("Sign in to your sky", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(36.dp))

                EtoileTextField(
                    value = vm.email,
                    onValueChange = vm::onEmailChange,
                    label = "Email",
                    enabled = !vm.loading,
                    isError = vm.error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )
                Spacer(Modifier.height(12.dp))
                EtoileTextField(
                    value = vm.password,
                    onValueChange = vm::onPasswordChange,
                    label = "Password",
                    isPassword = true,
                    enabled = !vm.loading,
                    isError = vm.error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); vm.submit() }),
                )

                Text(
                    text = vm.error ?: " ",
                    color = Danger,
                    style = MaterialTheme.typography.bodySmall.copy(color = Danger),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp),
                )

                EtoileButton(
                    text = "Sign in",
                    onClick = { focus.clearFocus(); vm.submit() },
                    loading = vm.loading,
                    enabled = vm.canSubmit || vm.loading,
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
