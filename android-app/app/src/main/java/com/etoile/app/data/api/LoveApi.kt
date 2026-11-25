package com.etoile.app.data.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Love part of notification-service (port 8083 locally). */
interface LoveApi {
    @GET("api/love")
    suspend fun status(): LoveStatus

    /** One burst of taps (the app groups quick taps together). */
    @POST("api/love/send")
    suspend fun send(@Body body: SendLoveRequest): LoveStatus

    @POST("api/love/need")
    suspend fun need(): LoveStatus

    /** Opening the love tab: clears the ×N, the highlight and the Need-love reminders. */
    @POST("api/love/seen")
    suspend fun seen(): LoveStatus
}

/**
 * loveReceived: the ×N on your heart. theyNeedYou: the other person pressed "Need love".
 * youAreWaiting: you pressed "Need love" and they haven't opened it yet.
 */
@Serializable
data class LoveStatus(
    val loveReceived: Int = 0,
    val theyNeedYou: Boolean = false,
    val youAreWaiting: Boolean = false,
)

@Serializable
data class SendLoveRequest(val taps: Int)
