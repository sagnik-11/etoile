package com.etoile.app.data.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

/** Device registration and "seen" in notification-service (port 8083 locally). */
interface NotificationsApi {
    /** Tells the server where to send pushes for this phone. */
    @POST("api/devices")
    suspend fun register(@Body body: DeviceRequest)

    /** On log out: stop sending pushes to this phone. */
    @POST("api/devices/unregister")
    suspend fun unregister(@Body body: DeviceRequest)

    /** Opening a date: resets its merged-notification count ("date-<id>"). */
    @POST("api/notifications/seen")
    suspend fun seen(@Body body: SeenRequest)
}

@Serializable
data class DeviceRequest(val token: String)

@Serializable
data class SeenRequest(val groupKey: String)
