package com.didit.application.achievement

import com.didit.domain.achievement.BadgeCondition
import com.didit.domain.achievement.BadgeConditionType
import org.springframework.stereotype.Component

@Component
class BadgeConditionChecker {
    fun isSatisfied(
        condition: BadgeCondition,
        context: BadgeCheckContext,
    ): Boolean =
        when (condition.conditionType) {
            BadgeConditionType.CUMULATIVE_RETRO -> context.totalRetroCount >= condition.threshold
            BadgeConditionType.WEEKLY_RETRO_COUNT -> context.currentWeekRetroCount >= condition.threshold
            BadgeConditionType.WEEKLY_STREAK -> checkWeeklyStreak(condition, context)
            BadgeConditionType.DAILY_ACCESS_STREAK -> context.dailyAccessStreak.currentStreak >= condition.threshold
            BadgeConditionType.PROJECT_COUNT -> context.projectCount >= condition.threshold
            BadgeConditionType.PROJECT_TAGGED_RETRO -> context.projectAssignedRetroCount >= condition.threshold
            BadgeConditionType.PROJECT_RETRO_IN_ONE -> context.maxRetroInOneProject >= condition.threshold
        }

    private fun checkWeeklyStreak(
        condition: BadgeCondition,
        context: BadgeCheckContext,
    ): Boolean {
        val consecutive =
            when (condition.weeklyMinCount()) {
                1 -> context.weeklyRetroStreak.currentWeeks
                3 -> context.weeklyStreakWithMin3
                else -> return false
            }
        return consecutive >= condition.threshold
    }
}
