package com.etoile.app.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etoile.app.data.AuthRepository
import com.etoile.app.data.api.DatePlan
import com.etoile.app.data.api.DatesApi
import com.etoile.app.data.api.LoveApi
import com.etoile.app.data.api.LoveStatus
import com.etoile.app.data.api.friendlyMessage
import com.etoile.app.push.AppEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class HomeViewModel(
    private val dates: DatesApi,
    private val love: LoveApi,
    private val auth: AuthRepository,
    events: Flow<AppEvent>,
) : ViewModel() {

    init {
        // A push arrived while the app is open: reload
        viewModelScope.launch { events.collect { refresh() } }
    }

    var upcoming by mutableStateOf<List<DatePlan>>(emptyList())
        private set
    var loveStatus by mutableStateOf(LoveStatus())
        private set
    var loaded by mutableStateOf(false)      // false until the first answer arrives
        private set
    var refreshing by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** The soonest date that's still coming (the list is already sorted soonest first). */
    val next: DatePlan? get() = upcoming.firstOrNull()

    fun refresh() {
        if (refreshing) return
        refreshing = true
        viewModelScope.launch {
            // Ask both services at the same time
            val datesCall = async { runCatching { dates.list("upcoming") } }
            val loveCall = async { runCatching { love.status() } }
            val datesResult = datesCall.await()
            val loveResult = loveCall.await()

            datesResult.onSuccess { upcoming = it }
            loveResult.onSuccess { loveStatus = it }
            val failure = datesResult.exceptionOrNull() ?: loveResult.exceptionOrNull()
            if ((failure as? HttpException)?.code() == 401) auth.logout()   // token no longer valid
            error = failure?.friendlyMessage()
            loaded = true
            refreshing = false
        }
    }
}
