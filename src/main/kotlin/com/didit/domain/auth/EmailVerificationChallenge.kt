package com.didit.domain.auth

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "email_verification_challenges")
@Entity
class EmailVerificationChallenge(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(name = "session_id", nullable = false, columnDefinition = "BINARY(16)")
    val sessionId: UUID,
    @Column(nullable = false)
    val email: String,
    @Column(name = "otp_hash", nullable = false)
    val otpHash: String,
    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,
    @Column(name = "verified_at")
    var verifiedAt: LocalDateTime? = null,
    @Column(name = "invalidated_at")
    var invalidatedAt: LocalDateTime? = null,
) : BaseEntity() {
    val isVerified: Boolean get() = verifiedAt != null
    val isInvalidated: Boolean get() = invalidatedAt != null

    fun isExpired(now: LocalDateTime = LocalDateTime.now()): Boolean = !now.isBefore(expiresAt)

    fun recordFailure() {
        attemptCount++
    }

    fun verify(now: LocalDateTime = LocalDateTime.now()) {
        verifiedAt = now
    }

    fun invalidate(now: LocalDateTime = LocalDateTime.now()) {
        if (!isVerified && !isInvalidated) invalidatedAt = now
    }
}
