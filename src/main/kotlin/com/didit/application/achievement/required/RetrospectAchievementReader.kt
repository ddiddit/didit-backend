package com.didit.application.achievement.required

import java.time.LocalDate
import java.util.UUID

interface RetrospectAchievementReader {
    fun countCompletedRetros(userId: UUID): Int

    fun countRetrosInWeek(
        userId: UUID,
        weekStartKst: LocalDate,
    ): Int

    fun countConsecutiveWeeksWithMinRetros(
        userId: UUID,
        weeklyMinCount: Int,
    ): Int
}
