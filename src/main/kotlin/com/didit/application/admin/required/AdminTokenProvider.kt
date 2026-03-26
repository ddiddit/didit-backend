package com.didit.application.admin.required

import java.time.LocalDateTime
import java.util.UUID

interface AdminTokenProvider {
    fun generateAccessToken(adminId: UUID): String

    fun generateRefreshToken(): String

    fun getRefreshTokenExpiresAt(): LocalDateTime
}
