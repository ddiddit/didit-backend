package com.didit.application.achievement.dto

import com.didit.domain.achievement.Badge
import com.didit.domain.achievement.UserBadge
import java.time.LocalDateTime
import java.util.UUID

data class BadgeResponse(
    val id: UUID,
    val name: String,
    val description: String,
    val conditionType: String,
    val acquired: Boolean,
    val acquiredAt: LocalDateTime?,
) {
    companion object {
        fun of(
            badge: Badge,
            userBadge: UserBadge?,
        ): BadgeResponse =
            BadgeResponse(
                id = badge.id,
                name = badge.name,
                description = badge.description,
                conditionType = badge.conditionType.name,
                acquired = userBadge != null,
                acquiredAt = userBadge?.acquiredAt,
            )
    }
}
