package com.didit.domain.achievement

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "user_badges")
@Entity
class UserBadge(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val userId: UUID,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val badgeId: UUID,
    @Column(nullable = false)
    val acquiredAt: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = false)
    var isNotified: Boolean = false,
) {
    fun markAsNotified() {
        isNotified = true
    }

    companion object {
        fun create(
            userId: UUID,
            badgeId: UUID,
        ): UserBadge = UserBadge(userId = userId, badgeId = badgeId)
    }
}
