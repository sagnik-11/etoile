package com.etoile.app.data

/** The two accounts in Étoile. Comes from the login token. */
enum class Role {
    HIM, HER;

    fun other(): Role = if (this == HIM) HER else HIM
}
