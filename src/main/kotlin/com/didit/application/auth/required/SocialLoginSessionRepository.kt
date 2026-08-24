package com.didit.application.auth.required

import com.didit.domain.auth.SocialLoginSession
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface SocialLoginSessionRepository : Repository<SocialLoginSession, UUID> {
    fun save(session: SocialLoginSession): SocialLoginSession

    fun findByTokenHash(tokenHash: String): SocialLoginSession?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SocialLoginSession s WHERE s.tokenHash = :tokenHash")
    fun findByTokenHashForUpdate(
        @Param("tokenHash") tokenHash: String,
    ): SocialLoginSession?

    @Modifying
    @Query("DELETE FROM SocialLoginSession s WHERE s.expiresAt < :cutoff")
    fun deleteAllExpiredBefore(
        @Param("cutoff") cutoff: LocalDateTime,
    ): Int
}
