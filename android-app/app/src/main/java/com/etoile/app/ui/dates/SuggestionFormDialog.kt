package com.etoile.app.ui.dates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.etoile.app.data.api.Genre
import com.etoile.app.data.api.SuggestionRequest
import com.etoile.app.ui.components.EtoileButton
import com.etoile.app.ui.components.EtoileOutlineButton
import com.etoile.app.ui.components.EtoileTextField
import com.etoile.app.ui.components.ErrorText
import com.etoile.app.ui.components.SectionLabel
import com.etoile.app.ui.theme.Etoile
import com.etoile.app.ui.theme.Hairline
import com.etoile.app.ui.theme.Muted
import com.etoile.app.ui.theme.NavyRaised

private val youtubePattern = Regex("^https://(www\\.|m\\.)?(youtube\\.com|youtu\\.be)/.+$")
private const val MAX_GENRES = 5

/**
 * Form for one movie: title, year, genres, trailer link and a short synopsis.
 * Checks the same rules as the server, so mistakes show before sending.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuggestionFormDialog(
    heading: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onSubmit: (SuggestionRequest) -> Unit,
    busy: Boolean = false,
    serverError: String? = null,
) {
    val accent = Etoile.accent
    var title by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var trailer by rememberSaveable { mutableStateOf("") }
    var synopsis by rememberSaveable { mutableStateOf("") }
    val genres = remember { mutableStateListOf<Genre>() }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val y = year.trim().toIntOrNull()
        val link = trailer.trim()
        error = when {
            title.isBlank() -> "Add the movie's name."
            y == null || y !in 1888..2100 -> "Add the year it came out (like 2024)."
            link.isNotEmpty() && !youtubePattern.matches(link) -> "The trailer must be a YouTube link (https://youtube.com/...)."
            else -> null
        }
        if (error == null) {
            onSubmit(
                SuggestionRequest(
                    title = title.trim(),
                    releaseYear = y!!,
                    synopsis = synopsis.trim().ifEmpty { null },
                    youtubeUrl = link.ifEmpty { null },
                    genres = Genre.entries.filter { it in genres },   // keep the server's order
                ),
            )
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(16.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(NavyRaised)
                .border(BorderStroke(0.5.dp, Hairline), RoundedCornerShape(24.dp))
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(heading, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            EtoileTextField(
                value = title, onValueChange = { title = it.take(255) }, label = "Movie name",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(10.dp))
            EtoileTextField(
                value = year, onValueChange = { year = it.filter(Char::isDigit).take(4) }, label = "Year",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(16.dp))
            SectionLabel("Genres · up to $MAX_GENRES")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Genre.entries.forEach { g ->
                    val on = g in genres
                    Text(
                        g.label,
                        style = MaterialTheme.typography.bodySmall.copy(color = if (on) accent.main else Muted),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (on) accent.soft else Color.Transparent)
                            .border(BorderStroke(0.5.dp, if (on) accent.line else Hairline), RoundedCornerShape(50))
                            .clickable {
                                if (on) genres.remove(g) else if (genres.size < MAX_GENRES) genres.add(g)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            EtoileTextField(
                value = trailer, onValueChange = { trailer = it.take(1000) }, label = "YouTube trailer (optional)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(10.dp))
            EtoileTextField(
                value = synopsis, onValueChange = { synopsis = it.take(5000) }, label = "What's it about? (optional)",
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            ErrorText(error ?: serverError)
            Spacer(Modifier.height(6.dp))
            EtoileButton(confirmLabel, onClick = ::submit, loading = busy)
            Spacer(Modifier.height(8.dp))
            EtoileOutlineButton("Cancel", onClick = onDismiss)
        }
    }
}
