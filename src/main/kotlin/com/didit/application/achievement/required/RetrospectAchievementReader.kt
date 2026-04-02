package com.didit.application.achievement.required

import java.time.DayOfWeek
import java.util.UUID

interface RetrospectAchievementReader {
    fun countCompletedRetros(userId: UUID): Int

    fun countDeepQuestionAnswers(userId: UUID): Int

    fun countByDayOfWeek(userId: UUID): Map<DayOfWeek, Int>

    fun countWeeklyGoalAchievedWeeks(userId: UUID): Int
}
