package com.didit.domain.auth

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "social_login_sessions")
@Entity
class SocialLoginSession(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    val tokenHash: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val provider: Provider,
    @Column(name = "provider_id", nullable = false)
    val providerId: String,
    @Column(name = "provider_email")
    val providerEmail: String?,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,
    @Column(name = "consumed_at")
    var consumedAt: LocalDateTime? = null,
) : BaseEntity() {
    val isConsumed: Boolean get() = consumedAt != null

    fun isExpired(now: LocalDateTime = LocalDateTime.now()): Boolean = !now.isBefore(expiresAt)

    fun consume(now: LocalDateTime = LocalDateTime.now()) {
        check(!isConsumed) { "이미 사용된 소셜 로그인 세션입니다." }
        consumedAt = now
    }
}
