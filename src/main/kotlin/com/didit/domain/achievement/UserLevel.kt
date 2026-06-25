package com.didit.domain.achievement

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "user_levels")
@Entity
class UserLevel(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val userId: UUID,
    @Column(nullable = false)
    var currentLevel: Int = 1,
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun levelUp() {
        if (currentLevel < 10) {
            currentLevel += 1
            updatedAt = LocalDateTime.now()
        }
    }

    companion object {
        fun create(userId: UUID): UserLevel = UserLevel(userId = userId)
    }
}
