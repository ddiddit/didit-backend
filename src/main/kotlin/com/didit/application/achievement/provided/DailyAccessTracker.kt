package com.didit.application.achievement.provided

import com.didit.domain.achievement.DailyAccessStreak
import java.time.LocalDate
import java.util.UUID

interface DailyAccessTracker {
    fun recordAccess(
        userId: UUID,
        accessDateKst: LocalDate,
    ): DailyAccessStreak
}
