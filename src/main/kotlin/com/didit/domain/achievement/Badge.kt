package com.didit.domain.achievement

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "badges")
@Entity
class Badge(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 50)
    val name: String,
    @Column(nullable = false, length = 255)
    val description: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val conditionType: BadgeConditionType,
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun isSameCondition(conditionType: BadgeConditionType): Boolean = this.conditionType == conditionType

    companion object {
        fun create(
            name: String,
            description: String,
            conditionType: BadgeConditionType,
        ): Badge = Badge(name = name, description = description, conditionType = conditionType)
    }
}
