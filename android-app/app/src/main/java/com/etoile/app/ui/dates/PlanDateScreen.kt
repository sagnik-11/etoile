package com.etoile.app.ui.dates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etoile.app.data.api.CreateMeetingRequest
import com.etoile.app.data.api.CreateMovieRequest
import com.etoile.app.data.api.CreateTripRequest
import com.etoile.app.data.api.DateType
import com.etoile.app.data.api.DatesApi
import com.etoile.app.data.api.SuggestionRequest
import com.etoile.app.data.api.friendlyMessage
import com.etoile.app.ui.components.DateTimeField
import com.etoile.app.ui.components.ErrorText
import com.etoile.app.ui.components.EtoileButton
import com.etoile.app.ui.components.EtoileTextField
import com.etoile.app.ui.components.ScreenColumn
import com.etoile.app.ui.components.ScreenHeader
import com.etoile.app.ui.components.SectionLabel
import com.etoile.app.ui.label
import com.etoile.app.ui.theme.Etoile
import com.etoile.app.ui.theme.Glass
import com.etoile.app.ui.theme.Hairline
import com.etoile.app.ui.theme.Muted
import kotlinx.coroutines.launch
import java.time.Instant

class PlanDateViewModel(private val api: DatesApi) : ViewModel() {
    var type by mutableStateOf(DateType.MOVIE)
    var startsAt by mutableStateOf<Instant?>(null)
    var endsAt by mutableStateOf<Instant?>(null)
    var place by mutableStateOf("")
    var activity by mutableStateOf("")
    var mapsLink by mutableStateOf("")
    var itinerary by mutableStateOf("")
    val movies = mutableStateListOf<SuggestionRequest>()

    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    /** Checks the form like the server does; null = all good. */
    private fun problem(): String? {
        val start = startsAt ?: return "Pick a date and time."
        if (!start.isAfter(Instant.now())) return "That time has already passed."
        if (type != DateType.MOVIE && place.isBlank()) return if (type == DateType.TRIP) "Where are you going?" else "Where are you meeting?"
        if (type == DateType.TRIP) {
            val end = endsAt ?: return "Pick when the trip ends."
            if (!end.isAfter(start)) return "The trip must end after it starts."
        }
        val link = mapsLink.trim()
        if (type == DateType.MEETING && link.isNotEmpty() && !link.startsWith("http")) return "The map link should start with http."
        return null
    }

    fun submit(onCreated: (String) -> Unit) {
        error = problem()
        if (error != null || busy) return
        busy = true
        viewModelScope.launch {
            runCatching {
                val start = startsAt!!.toString()
                when (type) {
                    DateType.MOVIE -> api.createMovie(CreateMovieRequest(start, movies.toList()))
                    DateType.MEETING -> api.createMeeting(
                        CreateMeetingRequest(start, place.trim(), mapsLink.trim().ifEmpty { null }, activity.trim().ifEmpty { null }),
                    )
                    DateType.TRIP -> api.createTrip(CreateTripRequest(start, endsAt!!.toString(), place.trim(), itinerary.trim().ifEmpty { null }))
                }
            }.onSuccess { onCreated(it.id) }
                .onFailure { error = it.friendlyMessage() }
            busy = false
        }
    }
}

/** Plan a movie night, a meeting or a trip. */
@Composable
fun PlanDateScreen(vm: PlanDateViewModel, onBack: () -> Unit, onCreated: (String) -> Unit) {
    var addingMovie by rememberSaveable { mutableStateOf(false) }
    ScreenColumn {
        ScreenHeader("Plan a date", onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            SectionLabel("What kind")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DateType.entries.forEach { t ->
                    TypeChoice(t, selected = vm.type == t, onClick = { vm.type = t }, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(22.dp))
            DateTimeField(if (vm.type == DateType.TRIP) "Leaving" else "When", vm.startsAt, onChange = { vm.startsAt = it })
            if (vm.type == DateType.TRIP) {
                Spacer(Modifier.height(12.dp))
                DateTimeField("Back home", vm.endsAt, onChange = { vm.endsAt = it })
            }
            Spacer(Modifier.height(16.dp))

            when (vm.type) {
                DateType.MEETING -> {
                    EtoileTextField(
                        vm.place, { vm.place = it.take(255) }, "Where",
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    )
                    Spacer(Modifier.height(10.dp))
                    EtoileTextField(
                        vm.activity, { vm.activity = it.take(255) }, "What we'll do (optional)",
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    )
                    Spacer(Modifier.height(10.dp))
                    EtoileTextField(
                        vm.mapsLink, { vm.mapsLink = it.take(1000) }, "Google Maps link (optional)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    )
                }
                DateType.TRIP -> {
                    EtoileTextField(
                        vm.place, { vm.place = it.take(255) }, "Where to",
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    )
                    Spacer(Modifier.height(10.dp))
                    EtoileTextField(
                        vm.itinerary, { vm.itinerary = it.take(10_000) }, "Plan / itinerary (optional)",
                        minLines = 4,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )
                }
                DateType.MOVIE -> {
                    SectionLabel("Your movie ideas · optional, up to 2")
                    vm.movies.forEachIndexed { i, m ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Glass)
                                .border(BorderStroke(0.5.dp, Hairline), RoundedCornerShape(14.dp))
                                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${m.title} (${m.releaseYear})", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            IconButton(onClick = { vm.movies.removeAt(i) }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Remove", tint = Muted, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (vm.movies.size < 2) {
                        val accent = Etoile.accent
                        Row(
                            Modifier.clip(RoundedCornerShape(50)).clickable { addingMovie = true }.padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, tint = accent.main, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add a movie", style = MaterialTheme.typography.labelLarge.copy(color = accent.main))
                        }
                    }
                    Text(
                        "You can both add movies after the date is accepted, until 5 hours before.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            ErrorText(vm.error, Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(8.dp))
            EtoileButton("Send invite 💌", onClick = { vm.submit(onCreated) }, loading = vm.busy)
            Spacer(Modifier.height(28.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (addingMovie) {
        SuggestionFormDialog(
            heading = "Add a movie",
            confirmLabel = "Add",
            onDismiss = { addingMovie = false },
            onSubmit = { vm.movies.add(it); addingMovie = false },
        )
    }
}

@Composable
private fun TypeChoice(type: DateType, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = Etoile.accent
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) accent.soft else Glass)
            .border(BorderStroke(0.5.dp, if (selected) accent.line else Hairline), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(type.icon(), contentDescription = null, tint = if (selected) accent.main else Muted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(type.label(), style = MaterialTheme.typography.labelLarge.copy(color = if (selected) accent.main else Muted))
    }
}
