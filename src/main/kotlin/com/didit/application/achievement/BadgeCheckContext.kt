package com.didit.application.achievement

import com.didit.domain.achievement.DailyAccessStreak
import com.didit.domain.achievement.WeeklyRetroStreak
import java.time.LocalDate
import java.util.UUID

data class BadgeCheckContext(
    val userId: UUID,
    val retroDate: LocalDate,
    val totalRetroCount: Int,
    val currentWeekRetroCount: Int,
    val weeklyRetroStreak: WeeklyRetroStreak,
    val weeklyStreakWithMin3: Int,
    val dailyAccessStreak: DailyAccessStreak,
    val projectCount: Int,
    val projectAssignedRetroCount: Int,
    val maxRetroInOneProject: Int,
)
