package com.didit.support

import com.didit.domain.achievement.Badge
import com.didit.domain.achievement.BadgeCategory
import com.didit.domain.achievement.BadgeCondition
import com.didit.domain.achievement.BadgeConditionType

object BadgeFixture {
    fun cumulativeRetro(threshold: Int): Badge =
        Badge.create(
            name = "${threshold}회 기록",
            description = "누적 회고 ${threshold}회",
            category = BadgeCategory.CONSISTENCY,
            condition = BadgeCondition(BadgeConditionType.CUMULATIVE_RETRO, threshold),
        )

    fun weeklyRetroCount(threshold: Int): Badge =
        Badge.create(
            name = "루틴 첫걸음",
            description = "한 주에 회고 ${threshold}회",
            category = BadgeCategory.PATTERN,
            condition = BadgeCondition(BadgeConditionType.WEEKLY_RETRO_COUNT, threshold),
        )

    fun weeklyStreak(
        threshold: Int,
        weeklyMin: Int,
    ): Badge =
        Badge.create(
            name = if (weeklyMin >= 3) "루틴의 힘" else "루틴의 지속",
            description = "${threshold}주 연속 주 ${weeklyMin}회",
            category = BadgeCategory.PATTERN,
            condition =
                BadgeCondition(
                    conditionType = BadgeConditionType.WEEKLY_STREAK,
                    threshold = threshold,
                    params = mapOf("weeklyMinCount" to weeklyMin),
                ),
        )

    fun dailyAccessStreak(threshold: Int): Badge =
        Badge.create(
            name = "디딧 러버",
            description = "${threshold}일 연속 접속",
            category = BadgeCategory.ACCESS,
            condition = BadgeCondition(BadgeConditionType.DAILY_ACCESS_STREAK, threshold),
        )

    fun projectCount(threshold: Int): Badge =
        Badge.create(
            name = "컬렉터",
            description = "프로젝트 ${threshold}개",
            category = BadgeCategory.PROJECT,
            condition = BadgeCondition(BadgeConditionType.PROJECT_COUNT, threshold),
        )

    fun projectTaggedRetro(threshold: Int): Badge =
        Badge.create(
            name = "피커",
            description = "지정 회고 ${threshold}회",
            category = BadgeCategory.PROJECT,
            condition = BadgeCondition(BadgeConditionType.PROJECT_TAGGED_RETRO, threshold),
        )

    fun projectRetroInOne(threshold: Int): Badge =
        Badge.create(
            name = "디깅",
            description = "한 프로젝트 ${threshold}회",
            category = BadgeCategory.PROJECT,
            condition = BadgeCondition(BadgeConditionType.PROJECT_RETRO_IN_ONE, threshold),
        )
}
