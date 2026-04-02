package com.didit.application.achievement

import com.didit.domain.achievement.Streak
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

data class BadgeCheckContext(
    val userId: UUID,
    val totalRetroCount: Int,
    val streak: Streak,
    val deepQuestionCount: Int,
    val retroDate: LocalDate,
    val weeklyCountByDayOfWeek: Map<DayOfWeek, Int>,
    val weeklyGoalAchievedWeeks: Int,
)
