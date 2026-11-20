package com.didit.domain.auth

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Table(
    name = "social_identities",
    uniqueConstraints = [UniqueConstraint(columnNames = ["provider", "provider_id"])],
)
@Entity
class SocialIdentity(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    val userId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val provider: Provider,
    @Column(name = "provider_id", nullable = false)
    val providerId: String,
) : BaseEntity() {
    @Column(name = "apple_refresh_token", columnDefinition = "TEXT")
    private var protectedAppleRefreshToken: String? = null

    @Column(name = "apple_token_client_id")
    private var tokenClientId: String? = null

    val appleRefreshToken: String? get() = protectedAppleRefreshToken
    val appleTokenClientId: String? get() = tokenClientId

    fun updateAppleToken(
        protectedRefreshToken: String,
        tokenClientId: String,
    ) {
        check(provider == Provider.APPLE) { "Apple 식별자에만 Apple 토큰을 저장할 수 있습니다." }
        protectedAppleRefreshToken = protectedRefreshToken
        this.tokenClientId = tokenClientId
    }

    companion object {
        fun create(
            userId: UUID,
            provider: Provider,
            providerId: String,
            appleRefreshToken: String? = null,
            appleTokenClientId: String? = null,
        ) = SocialIdentity(userId = userId, provider = provider, providerId = providerId).also { identity ->
            if (appleRefreshToken != null && appleTokenClientId != null) {
                identity.updateAppleToken(appleRefreshToken, appleTokenClientId)
            }
        }
    }
}
