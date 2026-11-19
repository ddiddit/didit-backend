package com.didit.application.auth.required

interface AppleTokenProtector {
    fun protect(refreshToken: String): String

    fun unprotect(protectedToken: String): String
}
