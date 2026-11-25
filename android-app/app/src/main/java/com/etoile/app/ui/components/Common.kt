package com.etoile.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.etoile.app.ui.theme.Cream
import com.etoile.app.ui.theme.Danger
import com.etoile.app.ui.theme.Etoile
import com.etoile.app.ui.theme.Glass
import com.etoile.app.ui.theme.Hairline
import com.etoile.app.ui.theme.Muted
import com.etoile.app.ui.theme.NavyRaised

/** Width limit so screens stay neat on tablets and big phones. */
val MaxContentWidth = 520.dp

/** Centers content and limits its width, for any screen size. */
@Composable
fun ScreenColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier.fillMaxSize().widthIn(max = MaxContentWidth), content = content)
    }
}

/** Title row at the top of a screen, with an optional back arrow and actions on the right. */
@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = if (onBack != null) 6.dp else 22.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Muted)
            }
            Spacer(Modifier.width(2.dp))
        }
        Column(Modifier.weight(1f)) {
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelSmall)
            Text(title, style = MaterialTheme.typography.headlineSmall)
        }
        actions()
    }
}

/** Soft see-through card with a thin border. [highlighted] uses the accent border. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val accent = Etoile.accent
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (highlighted) accent.soft else Glass)
            .border(BorderStroke(0.5.dp, if (highlighted) accent.line else Hairline), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(18.dp),
        content = content,
    )
}

/** Small capsule label. [highlighted] = accent colour (needs your attention). */
@Composable
fun Pill(text: String, highlighted: Boolean = false, color: Color? = null, modifier: Modifier = Modifier) {
    val accent = Etoile.accent
    val textColor = color ?: if (highlighted) accent.main else Muted
    Text(
        text,
        style = MaterialTheme.typography.labelSmall.copy(color = textColor, letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 0.4f),
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (highlighted) accent.soft else Color.Transparent)
            .border(BorderStroke(0.5.dp, if (highlighted) accent.line else Hairline), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Small spaced-out caps label above a section. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, modifier = modifier.padding(bottom = 8.dp))
}

/** Row of equal options, one selected (like Upcoming | Past). */
@Composable
fun SegmentedChoice(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val accent = Etoile.accent
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .border(BorderStroke(0.5.dp, Hairline), RoundedCornerShape(50))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { i, label ->
            val isSelected = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) accent.soft else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge.copy(color = if (isSelected) accent.main else Muted))
            }
        }
    }
}

@Composable
fun ErrorText(message: String?, modifier: Modifier = Modifier) {
    if (message == null) return
    Text(
        message,
        style = MaterialTheme.typography.bodySmall.copy(color = Danger),
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
    )
}

@Composable
fun CenteredLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(22.dp), color = Etoile.accent.main, strokeWidth = 1.5.dp)
    }
}

/** Quiet message for an empty list. */
@Composable
fun EmptyState(title: String, line: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(line, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

/** Yes/no dialog in the app's style. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Not now",
) {
    val accent = Etoile.accent
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyRaised,
        titleContentColor = Cream,
        textContentColor = Muted,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = accent.main) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel, color = Muted) } },
    )
}
