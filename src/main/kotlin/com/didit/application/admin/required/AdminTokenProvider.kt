package com.didit.application.admin.required

import com.didit.domain.admin.AdminRole
import java.time.LocalDateTime
import java.util.UUID

interface AdminTokenProvider {
    fun generateAccessToken(
        adminId: UUID,
        role: AdminRole,
    ): String

    fun generateRefreshToken(): String

    fun getRefreshTokenExpiresAt(): LocalDateTime
}
