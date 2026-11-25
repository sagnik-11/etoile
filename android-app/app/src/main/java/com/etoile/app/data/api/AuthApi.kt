package com.etoile.app.data.api

import com.etoile.app.data.Role
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

/** auth-service (port 8081 locally). */
interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    /** Needs the current password. Answers with a fresh token. */
    @POST("api/auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): AuthResponse
}

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: String,
    val displayName: String,
    val role: Role,
)
