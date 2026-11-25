package com.etoile.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.etoile.app.ui.theme.Cream
import com.etoile.app.ui.theme.Dim
import com.etoile.app.ui.theme.Etoile
import com.etoile.app.ui.theme.Glass
import com.etoile.app.ui.theme.Hairline
import com.etoile.app.ui.theme.Muted
import com.etoile.app.ui.theme.Navy

/** Thin-bordered text field that lights up in the accent colour when focused. */
@Composable
fun EtoileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isPassword: Boolean = false,
    isError: Boolean = false,
    enabled: Boolean = true,
    minLines: Int = 1,   // more than 1 = multi-line box (notes, itinerary)
) {
    var visible by remember { mutableStateOf(false) }
    val accent = Etoile.accent
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = minLines == 1,
        minLines = minLines,
        maxLines = if (minLines == 1) 1 else 12,
        enabled = enabled,
        isError = isError,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (visible) "Hide password" else "Show password",
                        tint = Muted,
                    )
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent.main,
            unfocusedBorderColor = Hairline,
            focusedLabelColor = accent.main,
            unfocusedLabelColor = Muted,
            cursorColor = accent.main,
            focusedTextColor = Cream,
            unfocusedTextColor = Cream,
            focusedContainerColor = Glass,
            unfocusedContainerColor = Glass,
            disabledTextColor = Dim,
            disabledBorderColor = Hairline,
        ),
    )
}

/** The main pill button, filled with the accent colour. Shows a spinner while [loading]. */
@Composable
fun EtoileButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
) {
    val accent = Etoile.accent
    Button(
        onClick = { if (!loading) onClick() },
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent.main,
            contentColor = Navy,
            disabledContainerColor = accent.main.copy(alpha = 0.35f),
            disabledContentColor = Navy,
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Navy, strokeWidth = 2.dp)
            else Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Quiet outlined pill button for secondary actions. */
@Composable
fun EtoileOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = Etoile.accent
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(50),
        border = BorderStroke(0.5.dp, accent.line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent.main),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
