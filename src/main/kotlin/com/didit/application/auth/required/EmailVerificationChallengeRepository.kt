package com.didit.application.auth.required

import com.didit.domain.auth.EmailVerificationChallenge
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.util.UUID

interface EmailVerificationChallengeRepository : Repository<EmailVerificationChallenge, UUID> {
    fun save(challenge: EmailVerificationChallenge): EmailVerificationChallenge

    fun findAllBySessionIdAndVerifiedAtIsNullAndInvalidatedAtIsNull(sessionId: UUID): List<EmailVerificationChallenge>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT c FROM EmailVerificationChallenge c
        WHERE c.sessionId = :sessionId
          AND c.verifiedAt IS NULL
          AND c.invalidatedAt IS NULL
        ORDER BY c.createdAt DESC
        """,
    )
    fun findActiveBySessionIdForUpdate(
        @Param("sessionId") sessionId: UUID,
    ): List<EmailVerificationChallenge>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT c FROM EmailVerificationChallenge c
        WHERE c.email = :email
        ORDER BY c.createdAt DESC
        """,
    )
    fun findLatestByEmailForUpdate(
        @Param("email") email: String,
    ): List<EmailVerificationChallenge>
}
