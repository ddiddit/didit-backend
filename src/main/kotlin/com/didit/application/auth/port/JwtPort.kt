package com.didit.application.auth.port

import com.didit.domain.auth.enums.Role
import java.util.UUID

interface JwtPort {
    fun createAccessToken(
        userId: UUID,
        role: Role,
    ): String

    fun createRefreshToken(userId: UUID): String

    fun getUserId(token: String): UUID
}
