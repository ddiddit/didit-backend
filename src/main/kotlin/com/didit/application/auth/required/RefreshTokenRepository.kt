package com.didit.application.auth.required

import com.didit.domain.auth.RefreshToken
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface RefreshTokenRepository : Repository<RefreshToken, UUID> {
    fun save(token: RefreshToken): RefreshToken

    fun findByToken(token: String): RefreshToken?

    fun deleteByUserId(userId: UUID)

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :now")
    fun deleteAllExpiredBefore(
        @Param("now") now: LocalDateTime,
    ): Int
}
