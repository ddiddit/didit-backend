package com.didit.application.achievement.required

import java.util.UUID

interface RetrospectAchievementReader {
    fun countCompletedRetros(userId: UUID): Int

    fun countDeepQuestionAnswers(userId: UUID): Int

    fun countWeeklyGoalAchievedWeeks(userId: UUID): Int
}
