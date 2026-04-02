package com.didit.application.achievement.provided

import com.didit.domain.achievement.Badge
import java.time.LocalDate
import java.util.UUID

interface BadgeAwarder {
    fun awardBadges(
        userId: UUID,
        retroDate: LocalDate,
    ): List<Badge>
}
