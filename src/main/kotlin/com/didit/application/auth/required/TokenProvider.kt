package com.didit.application.auth.required

import java.time.LocalDateTime
import java.util.UUID

interface TokenProvider {
    fun generateAccessToken(userId: UUID): String

    fun generateRefreshToken(): String

    fun getRefreshTokenExpiresAt(): LocalDateTime
}
