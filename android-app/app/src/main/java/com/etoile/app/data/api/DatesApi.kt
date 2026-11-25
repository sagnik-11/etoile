package com.etoile.app.data.api

import com.etoile.app.data.Role
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.Instant

/** dates-service (port 8082 locally). Every action answers with the updated date. */
interface DatesApi {
    /** scope = "upcoming" (soonest first) or "past" (newest first). */
    @GET("api/dates")
    suspend fun list(@Query("scope") scope: String = "upcoming"): List<DatePlan>

    @GET("api/dates/{id}")
    suspend fun get(@Path("id") id: String): DatePlan

    // ----- Plan -----
    @POST("api/dates/movie")
    suspend fun createMovie(@Body body: CreateMovieRequest): DatePlan

    @POST("api/dates/meeting")
    suspend fun createMeeting(@Body body: CreateMeetingRequest): DatePlan

    @POST("api/dates/trip")
    suspend fun createTrip(@Body body: CreateTripRequest): DatePlan

    // ----- Answer / cancel -----
    @POST("api/dates/{id}/accept")
    suspend fun accept(@Path("id") id: String): DatePlan

    @POST("api/dates/{id}/reject")
    suspend fun reject(@Path("id") id: String): DatePlan

    @POST("api/dates/{id}/cancel")
    suspend fun cancel(@Path("id") id: String, @Body body: CancelRequest): DatePlan

    // ----- Reschedule -----
    @POST("api/dates/{id}/reschedule")
    suspend fun requestReschedule(@Path("id") id: String): DatePlan

    @POST("api/dates/{id}/reschedule/acknowledge")
    suspend fun acknowledgeReschedule(@Path("id") id: String): DatePlan

    @POST("api/dates/{id}/reschedule/new-time")
    suspend fun setNewTime(@Path("id") id: String, @Body body: NewTimeRequest): DatePlan

    @POST("api/dates/{id}/reschedule/cancel")
    suspend fun cancelReschedule(@Path("id") id: String): DatePlan

    // ----- Movies -----
    @POST("api/dates/{id}/suggestions")
    suspend fun suggest(@Path("id") id: String, @Body body: SuggestionRequest): DatePlan

    @POST("api/dates/{id}/suggestions/{suggestionId}/shelve")
    suspend fun shelve(@Path("id") id: String, @Path("suggestionId") suggestionId: String): DatePlan

    @POST("api/dates/{id}/suggestions/{suggestionId}/bring-back")
    suspend fun bringBack(@Path("id") id: String, @Path("suggestionId") suggestionId: String): DatePlan

    @POST("api/dates/{id}/vote")
    suspend fun vote(@Path("id") id: String, @Body body: VoteRequest): DatePlan

    @POST("api/dates/{id}/agree")
    suspend fun agree(@Path("id") id: String, @Body body: AgreeRequest): DatePlan

    @POST("api/dates/{id}/random-pick")
    suspend fun randomPick(@Path("id") id: String): DatePlan
}

enum class DateType { MOVIE, MEETING, TRIP }

enum class DateStatus { PROPOSED, ACCEPTED, REJECTED, CANCELLED }

/** How the movie was decided. */
enum class DecisionMethod { ONLY_ONE, VOTE, AGREED_NARROWED, AGREED_OTHER, RANDOM }

/** Same list as the backend, in the same order. */
enum class Genre(val label: String) {
    ACTION("Action"), ADVENTURE("Adventure"), ANIMATION("Animation"), COMEDY("Comedy"),
    CRIME("Crime"), DOCUMENTARY("Documentary"), DRAMA("Drama"), FAMILY("Family"),
    FANTASY("Fantasy"), HORROR("Horror"), MUSICAL("Musical"), MYSTERY("Mystery"),
    ROMANCE("Romance"), SCI_FI("Sci-fi"), THRILLER("Thriller"),
    ANIME("Anime"), SHONEN("Shōnen"), SHOJO("Shōjo"), SEINEN("Seinen"), JOSEI("Josei"),
    ISEKAI("Isekai"), MECHA("Mecha"), SLICE_OF_LIFE("Slice of life"), MAGICAL_GIRL("Magical girl"),
    OTHER("Other"),
}

/** One planned date, as the backend sends it. Times are ISO text like "2026-11-26T13:30:00Z". */
@Serializable
data class DatePlan(
    val id: String,
    val type: DateType,
    val status: DateStatus,
    val proposedBy: Role,
    val cancelledBy: Role? = null,
    val startsAt: String,
    val endsAt: String? = null,
    val place: String? = null,
    val mapsLink: String? = null,
    val activity: String? = null,
    val itinerary: String? = null,
    val shortlist: List<Suggestion> = emptyList(),
    val shelf: List<Suggestion> = emptyList(),
    val decision: Decision? = null,
    val voting: Voting? = null,
    val reschedule: Reschedule? = null,
) {
    val start: Instant get() = Instant.parse(startsAt)
    val end: Instant? get() = endsAt?.let(Instant::parse)

    /** Short name for cards: the movie title, the activity, or the trip place. */
    val title: String
        get() = when (type) {
            DateType.MOVIE -> decision?.title ?: "Movie night"
            DateType.MEETING -> activity?.takeIf { it.isNotBlank() } ?: place ?: "A date"
            DateType.TRIP -> place?.let { "Trip to $it" } ?: "A trip"
        }

    val isActive: Boolean get() = status == DateStatus.PROPOSED || status == DateStatus.ACCEPTED
    fun hasStarted(now: Instant = Instant.now()): Boolean = !now.isBefore(start)
}

@Serializable
data class Suggestion(
    val id: String,
    val suggestedBy: Role,
    val title: String,
    val releaseYear: Int,
    val synopsis: String? = null,
    val youtubeUrl: String? = null,
    val genres: List<Genre> = emptyList(),
    val shortlisted: Boolean = true,
)

@Serializable
data class Decision(
    val movieId: String? = null,
    val title: String? = null,
    val method: DecisionMethod? = null,
    val isFinal: Boolean = false,
    val herPickId: String? = null,   // the 2 narrowed movies after a split vote
    val himPickId: String? = null,
) {
    /** The vote split and nothing is decided yet: time for a call (or a random pick). */
    val isSplit: Boolean get() = movieId == null && herPickId != null && himPickId != null
}

/** Votes are secret: the other person's choice stays null until you've both voted. */
@Serializable
data class Voting(
    val himVoted: Boolean = false,
    val herVoted: Boolean = false,
    val himVote: String? = null,
    val herVote: String? = null,
) {
    fun voted(role: Role) = if (role == Role.HIM) himVoted else herVoted
    fun voteOf(role: Role) = if (role == Role.HIM) himVote else herVote
}

/** Present only while a reschedule is open. */
@Serializable
data class Reschedule(
    val id: String,
    val requestedBy: Role,
    val requestedAt: String,
    val acknowledgedAt: String? = null,
)

// ----- Request bodies -----

@Serializable
data class SuggestionRequest(
    val title: String,
    val releaseYear: Int,
    val synopsis: String? = null,
    val youtubeUrl: String? = null,
    val genres: List<Genre> = emptyList(),
)

@Serializable
data class CreateMovieRequest(val startsAt: String, val suggestions: List<SuggestionRequest> = emptyList())

@Serializable
data class CreateMeetingRequest(val startsAt: String, val place: String, val mapsLink: String? = null, val activity: String? = null)

@Serializable
data class CreateTripRequest(val startsAt: String, val endsAt: String, val place: String, val itinerary: String? = null)

@Serializable
data class CancelRequest(val sorryCount: Int)

@Serializable
data class NewTimeRequest(val startsAt: String, val endsAt: String? = null)

@Serializable
data class VoteRequest(val suggestionId: String)

/** Either an existing movie's id, or a brand-new movie, never both. */
@Serializable
data class AgreeRequest(val suggestionId: String? = null, val newMovie: SuggestionRequest? = null)
