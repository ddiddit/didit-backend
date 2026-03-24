package com.didit.application.auth.required

import com.didit.domain.auth.entity.RefreshToken
import java.util.UUID

interface RefreshTokenRepository {
    fun save(refreshToken: RefreshToken): RefreshToken

    fun findByToken(token: String): RefreshToken?

    fun findByUserId(userId: UUID): RefreshToken?

    fun deleteByUserId(userId: UUID)
}
