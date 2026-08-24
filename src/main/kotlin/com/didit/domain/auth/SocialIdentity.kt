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
    companion object {
        fun create(
            userId: UUID,
            provider: Provider,
            providerId: String,
        ) = SocialIdentity(userId = userId, provider = provider, providerId = providerId)
    }
}
