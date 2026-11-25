package com.etoile.app.data

/** Who is logged in on this phone. Saved so the app remembers the login. */
data class Session(
    val token: String,
    val userId: String,
    val displayName: String,
    val role: Role,
    /** When the token stops working, in epoch seconds (from the token's "exp"). */
    val expiresAt: Long,
) {
    fun isExpired(nowSeconds: Long = System.currentTimeMillis() / 1000): Boolean = nowSeconds >= expiresAt
}
