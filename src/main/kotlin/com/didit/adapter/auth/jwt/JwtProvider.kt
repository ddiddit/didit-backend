package com.didit.adapter.auth.jwt

import com.didit.application.auth.port.JwtPort
import com.didit.domain.auth.enums.Role
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID

@Component
class JwtProvider(
    @Value("\${jwt.secret}")
    private val jwtSecret: String,
    @Value("\${jwt.access-token-expiration}")
    private val accessTokenExpiration: Long,
    @Value("\${jwt.refresh-token-expiration}")
    private val refreshTokenExpiration: Long,
) : JwtPort {
    private val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray())

    override fun createAccessToken(
        userId: UUID,
        role: Role,
    ): String {
        val now = Date()
        val expired = Date(now.time + accessTokenExpiration)

        return Jwts
            .builder()
            .setSubject(userId.toString())
            .claim("role", role.name)
            .setIssuedAt(now)
            .setExpiration(expired)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    override fun createRefreshToken(userId: UUID): String {
        val now = Date()
        return Jwts
            .builder()
            .setSubject(userId.toString())
            .setIssuedAt(now)
            .setExpiration(Date(now.time + refreshTokenExpiration))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    override fun getUserId(token: String): UUID {
        val claims = parseClaims(token)
        return UUID.fromString(claims.subject)
    }

    fun getRole(token: String): Role {
        val claims = parseClaims(token)
        return Role.valueOf(claims["role"] as String)
    }

    fun validateToken(token: String): Boolean =
        try {
            Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            true
        } catch (e: Exception) {
            false
        }

    private fun parseClaims(token: String): Claims =
        Jwts
            .parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
}
