package com.didit.domain.achievement

import jakarta.persistence.Column
import jakarta.persistence.Embedded
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
    @Column(nullable = false, length = 30)
    val category: BadgeCategory,
    @Embedded
    val condition: BadgeCondition,
    @Column(name = "icon_url", length = 500)
    val iconUrl: String? = null,
    @Column(name = "congrats_title", length = 100)
    val congratsTitle: String? = null,
    @Column(name = "congrats_message", length = 255)
    val congratsMessage: String? = null,
    @Column(nullable = false)
    var active: Boolean = true,
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    val conditionType: BadgeConditionType
        get() = condition.conditionType

    fun isSameCondition(conditionType: BadgeConditionType): Boolean = this.condition.conditionType == conditionType

    companion object {
        fun create(
            name: String,
            description: String,
            category: BadgeCategory,
            condition: BadgeCondition,
            iconUrl: String? = null,
            congratsTitle: String? = null,
            congratsMessage: String? = null,
        ): Badge =
            Badge(
                name = name,
                description = description,
                category = category,
                condition = condition,
                iconUrl = iconUrl,
                congratsTitle = congratsTitle,
                congratsMessage = congratsMessage,
            )
    }
}
