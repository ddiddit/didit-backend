package com.didit.application.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object SocialTokenCodec {
    private val secureRandom = SecureRandom()

    fun generate(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hash(rawToken: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(rawToken.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
