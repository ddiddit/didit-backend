package com.didit.adapter.security

import com.didit.application.auth.required.TokenProvider
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @param:Value("\${jwt.secret}") private val secret: String,
    @param:Value("\${jwt.access-token-expiry-ms}") private val accessTokenExpiryMs: Long,
    @param:Value("\${jwt.refresh-token-expiry-days}") private val refreshTokenExpiryDays: Long,
) : TokenProvider {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    override fun generateAccessToken(userId: UUID): String =
        Jwts
            .builder()
            .subject(userId.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + accessTokenExpiryMs))
            .signWith(key)
            .compact()

    override fun generateRefreshToken(): String = UUID.randomUUID().toString()

    override fun getRefreshTokenExpiresAt(): LocalDateTime = LocalDateTime.now().plusDays(refreshTokenExpiryDays)
}
