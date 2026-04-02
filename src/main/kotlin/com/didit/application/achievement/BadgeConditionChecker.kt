package com.didit.application.achievement

import com.didit.domain.achievement.BadgeConditionType
import org.springframework.stereotype.Component
import java.time.DayOfWeek

@Component
class BadgeConditionChecker {
    fun isSatisfied(
        conditionType: BadgeConditionType,
        context: BadgeCheckContext,
    ): Boolean =
        when (conditionType) {
            BadgeConditionType.FIRST_RETRO -> checkFirstRetro(context)
            BadgeConditionType.STREAK_3_DAYS -> checkStreak3Days(context)
            BadgeConditionType.TOTAL_30 -> checkTotal30(context)
            BadgeConditionType.DEEP_QUESTION_1 -> checkDeepQuestion(context, 1)
            BadgeConditionType.DEEP_QUESTION_5 -> checkDeepQuestion(context, 5)
            BadgeConditionType.DEEP_QUESTION_10 -> checkDeepQuestion(context, 10)
            BadgeConditionType.WEEKLY_MON -> checkWeeklyDay(context, DayOfWeek.MONDAY)
            BadgeConditionType.WEEKLY_TUE -> checkWeeklyDay(context, DayOfWeek.TUESDAY)
            BadgeConditionType.WEEKLY_WED -> checkWeeklyDay(context, DayOfWeek.WEDNESDAY)
            BadgeConditionType.WEEKLY_THU -> checkWeeklyDay(context, DayOfWeek.THURSDAY)
            BadgeConditionType.WEEKLY_FRI -> checkWeeklyDay(context, DayOfWeek.FRIDAY)
            BadgeConditionType.WEEKLY_SAT -> checkWeeklyDay(context, DayOfWeek.SATURDAY)
            BadgeConditionType.WEEKLY_SUN -> checkWeeklyDay(context, DayOfWeek.SUNDAY)
            BadgeConditionType.WEEKLY_3_FIRST -> checkWeekly3First(context)
            BadgeConditionType.WEEKLY_3_THREE_WEEKS -> checkWeekly3ThreeWeeks(context)
        }

    private fun checkFirstRetro(context: BadgeCheckContext): Boolean = context.totalRetroCount == 1

    private fun checkStreak3Days(context: BadgeCheckContext): Boolean = context.streak.isStreak(3)

    private fun checkTotal30(context: BadgeCheckContext): Boolean = context.totalRetroCount >= 30

    private fun checkDeepQuestion(
        context: BadgeCheckContext,
        count: Int,
    ): Boolean = context.deepQuestionCount >= count

    private fun checkWeeklyDay(
        context: BadgeCheckContext,
        dayOfWeek: DayOfWeek,
    ): Boolean = (context.weeklyCountByDayOfWeek[dayOfWeek] ?: 0) >= 3

    private fun checkWeekly3First(context: BadgeCheckContext): Boolean = context.weeklyGoalAchievedWeeks >= 1

    private fun checkWeekly3ThreeWeeks(context: BadgeCheckContext): Boolean = context.weeklyGoalAchievedWeeks >= 3
}
