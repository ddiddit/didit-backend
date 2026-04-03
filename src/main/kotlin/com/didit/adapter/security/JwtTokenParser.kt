package com.didit.adapter.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenParser(
    @param:Value("\${jwt.secret}") private val secret: String,
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun getUserId(token: String): UUID? =
        runCatching {
            val claims =
                Jwts
                    .parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .payload
            UUID.fromString(claims.subject)
        }.getOrNull()
}
