package com.etoile.app.data.api

import com.etoile.app.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Shared JSON settings: ignore fields the app doesn't use yet. */
val AppJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/**
 * Builds the HTTP clients. [tokenProvider] gives the current login token (or null),
 * which is sent as "Authorization: Bearer ..." on every request.
 */
class Network(tokenProvider: () -> String?) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val token = tokenProvider()
            val request = if (token == null) chain.request()
            else chain.request().newBuilder().header("Authorization", "Bearer $token").build()
            chain.proceed(request)
        }
        .apply {
            if (BuildConfig.DEBUG) {
                // Logs requests in Logcat while developing. Hides the Authorization header.
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                    redactHeader("Authorization")
                })
            }
        }
        .build()

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(AppJson.asConverterFactory("application/json".toMediaType()))
        .build()

    val auth: AuthApi = retrofit(BuildConfig.AUTH_URL).create(AuthApi::class.java)
    val dates: DatesApi = retrofit(BuildConfig.DATES_URL).create(DatesApi::class.java)
    private val notificationsRetrofit = retrofit(BuildConfig.NOTIFICATIONS_URL)
    val love: LoveApi = notificationsRetrofit.create(LoveApi::class.java)
    val notifications: NotificationsApi = notificationsRetrofit.create(NotificationsApi::class.java)
}

/** Turns any network error into a short, friendly sentence for the screen. */
fun Throwable.friendlyMessage(): String = when (this) {
    is HttpException -> serverDetail() ?: when (code()) {
        401 -> "Please sign in again."
        403 -> "You can't do that."
        404 -> "That doesn't exist anymore."
        in 500..599 -> "The server had a problem. Try again."
        else -> "Something went wrong (${code()})."
    }
    is IOException -> "Can't reach the server. Check your internet."
    else -> "Something went wrong."
}

/** Reads "detail" from the backend's error body (Spring ProblemDetail). */
private fun HttpException.serverDetail(): String? = runCatching {
    val body = response()?.errorBody()?.string() ?: return null
    AppJson.parseToJsonElement(body).jsonObject["detail"]?.jsonPrimitive?.content
}.getOrNull()?.takeIf { it.isNotBlank() }
