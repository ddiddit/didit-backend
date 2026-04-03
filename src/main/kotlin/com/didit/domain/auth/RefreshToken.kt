package com.didit.domain.auth

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "refresh_tokens")
@Entity
class RefreshToken(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    val userId: UUID,
    @Column(nullable = false, unique = true, length = 512)
    var token: String,
    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,
) : BaseEntity() {
    fun isExpired(now: LocalDateTime = LocalDateTime.now()) = now.isAfter(expiresAt)

    fun rotate(
        newToken: String,
        newExpiresAt: LocalDateTime,
    ) {
        this.token = newToken
        this.expiresAt = newExpiresAt
    }

    companion object {
        fun create(
            userId: UUID,
            token: String,
            expiresAt: LocalDateTime,
        ) = RefreshToken(userId = userId, token = token, expiresAt = expiresAt)
    }
}
