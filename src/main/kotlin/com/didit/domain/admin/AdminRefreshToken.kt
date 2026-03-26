package com.didit.domain.admin

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "admin_refresh_tokens")
@Entity
class AdminRefreshToken(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val adminId: UUID,
    @Column(nullable = false, unique = true, length = 512)
    var token: String,
    @Column(nullable = false)
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
            adminId: UUID,
            token: String,
            expiresAt: LocalDateTime,
        ) = AdminRefreshToken(adminId = adminId, token = token, expiresAt = expiresAt)
    }
}
