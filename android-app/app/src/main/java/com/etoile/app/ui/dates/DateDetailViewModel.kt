package com.etoile.app.ui.dates

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etoile.app.data.api.AgreeRequest
import com.etoile.app.data.api.CancelRequest
import com.etoile.app.data.api.DatePlan
import com.etoile.app.data.api.DatesApi
import com.etoile.app.data.api.NewTimeRequest
import com.etoile.app.data.api.SuggestionRequest
import com.etoile.app.data.api.VoteRequest
import com.etoile.app.data.api.friendlyMessage
import com.etoile.app.push.AppEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.Instant

/** One date's page. Every action sends one request and shows the updated date the server returns. */
class DateDetailViewModel(
    private val api: DatesApi,
    private val id: String,
    markSeen: (String) -> Unit,
    events: Flow<AppEvent>,
) : ViewModel() {

    var plan by mutableStateOf<DatePlan?>(null); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    init {
        markSeen(id)   // opening the date clears its notification
        load()
        viewModelScope.launch {
            events.collect { if (it.dateId == id) { markSeen(id); load() } }
        }
    }

    fun load() {
        viewModelScope.launch {
            runCatching { api.get(id) }
                .onSuccess { plan = it; error = null }
                .onFailure { error = it.friendlyMessage() }
        }
    }

    /** Runs one action; [onDone] only after it worked (e.g. to close a dialog). */
    private fun act(onDone: () -> Unit = {}, call: suspend () -> DatePlan) {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            runCatching { call() }
                .onSuccess { plan = it; onDone() }
                .onFailure { error = it.friendlyMessage() }
            busy = false
        }
    }

    fun clearError() { error = null }

    // ----- Answer / cancel -----
    fun accept() = act { api.accept(id) }
    fun reject() = act { api.reject(id) }
    fun cancel(sorryCount: Int, onDone: () -> Unit) = act(onDone) { api.cancel(id, CancelRequest(sorryCount)) }

    // ----- Reschedule -----
    fun requestReschedule() = act { api.requestReschedule(id) }
    fun acknowledgeReschedule() = act { api.acknowledgeReschedule(id) }
    fun cancelReschedule() = act { api.cancelReschedule(id) }
    fun setNewTime(start: Instant, end: Instant?, onDone: () -> Unit) =
        act(onDone) { api.setNewTime(id, NewTimeRequest(start.toString(), end?.toString())) }

    // ----- Movies -----
    fun suggest(movie: SuggestionRequest, onDone: () -> Unit) = act(onDone) { api.suggest(id, movie) }
    fun shelve(suggestionId: String) = act { api.shelve(id, suggestionId) }
    fun bringBack(suggestionId: String) = act { api.bringBack(id, suggestionId) }
    fun vote(suggestionId: String) = act { api.vote(id, VoteRequest(suggestionId)) }
    fun agreeOn(suggestionId: String, onDone: () -> Unit) = act(onDone) { api.agree(id, AgreeRequest(suggestionId = suggestionId)) }
    fun agreeOnNew(movie: SuggestionRequest, onDone: () -> Unit) = act(onDone) { api.agree(id, AgreeRequest(newMovie = movie)) }
    fun randomPick() = act { api.randomPick(id) }
}
