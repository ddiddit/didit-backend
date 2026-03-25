package com.didit.domain.auth

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "user_consents")
@Entity
class UserConsent(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val userId: UUID,
    @Column(name = "service_terms_agreed_at", nullable = false)
    val serviceTermsAgreedAt: LocalDateTime,
    @Column(name = "privacy_agreed_at", nullable = false)
    val privacyAgreedAt: LocalDateTime,
    @Column(name = "marketing_agreed", nullable = false)
    var marketingAgreed: Boolean = false,
    @Column(name = "marketing_agreed_at")
    var marketingAgreedAt: LocalDateTime? = null,
    @Column(name = "marketing_revoked_at")
    var marketingRevokedAt: LocalDateTime? = null,
) : BaseEntity() {
    fun updateMarketing(
        agreed: Boolean,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        marketingAgreed = agreed
        marketingAgreedAt = now.takeIf { agreed }
        marketingRevokedAt = now.takeIf { !agreed }
    }

    companion object {
        fun create(
            userId: UUID,
            marketingAgreed: Boolean,
            now: LocalDateTime = LocalDateTime.now(),
        ) = UserConsent(
            userId = userId,
            serviceTermsAgreedAt = now,
            privacyAgreedAt = now,
            marketingAgreed = marketingAgreed,
            marketingAgreedAt = if (marketingAgreed) now else null,
        )
    }
}
