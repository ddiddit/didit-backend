package com.didit.application.auth.required

import com.didit.domain.auth.RefreshToken
import org.springframework.data.repository.Repository
import java.util.UUID

interface RefreshTokenRepository : Repository<RefreshToken, UUID> {
    fun save(token: RefreshToken): RefreshToken

    fun findByToken(token: String): RefreshToken?

    fun deleteByUserId(userId: UUID)
}
