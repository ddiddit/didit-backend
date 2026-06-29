package com.didit.application.achievement

import com.didit.domain.achievement.BadgeCondition
import com.didit.domain.achievement.BadgeConditionType
import com.didit.domain.achievement.DailyAccessStreak
import com.didit.domain.achievement.WeeklyRetroStreak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class BadgeConditionCheckerTest {
    private lateinit var checker: BadgeConditionChecker
    private val userId = UUID.randomUUID()
    private val today = LocalDate.now()

    @BeforeEach
    fun setUp() {
        checker = BadgeConditionChecker()
    }

    private fun ctx(
        totalRetroCount: Int = 0,
        currentWeekRetroCount: Int = 0,
        weeklyRetroStreak: WeeklyRetroStreak = WeeklyRetroStreak.create(userId),
        weeklyStreakWithMin3: Int = 0,
        dailyAccessStreak: DailyAccessStreak = DailyAccessStreak.create(userId),
        projectCount: Int = 0,
        projectAssignedRetroCount: Int = 0,
        maxRetroInOneProject: Int = 0,
    ) = BadgeCheckContext(
        userId = userId,
        retroDate = today,
        totalRetroCount = totalRetroCount,
        currentWeekRetroCount = currentWeekRetroCount,
        weeklyRetroStreak = weeklyRetroStreak,
        weeklyStreakWithMin3 = weeklyStreakWithMin3,
        dailyAccessStreak = dailyAccessStreak,
        projectCount = projectCount,
        projectAssignedRetroCount = projectAssignedRetroCount,
        maxRetroInOneProject = maxRetroInOneProject,
    )

    @Test
    fun `CUMULATIVE_RETRO - 누적 회고 수가 threshold 이상이면 true`() {
        val condition = BadgeCondition(BadgeConditionType.CUMULATIVE_RETRO, threshold = 10)

        assertThat(checker.isSatisfied(condition, ctx(totalRetroCount = 10))).isTrue()
        assertThat(checker.isSatisfied(condition, ctx(totalRetroCount = 9))).isFalse()
    }

    @Test
    fun `WEEKLY_RETRO_COUNT - 이번 주 회고 수가 threshold 이상이면 true`() {
        val condition = BadgeCondition(BadgeConditionType.WEEKLY_RETRO_COUNT, threshold = 3)

        assertThat(checker.isSatisfied(condition, ctx(currentWeekRetroCount = 3))).isTrue()
        assertThat(checker.isSatisfied(condition, ctx(currentWeekRetroCount = 2))).isFalse()
    }

    @Test
    fun `WEEKLY_STREAK - weeklyMin 1 일 때 currentWeeks 가 threshold 이상이면 true`() {
        val streak =
            WeeklyRetroStreak.create(userId).apply {
                recordRetro(LocalDate.of(2026, 6, 1))
                recordRetro(LocalDate.of(2026, 6, 8))
                recordRetro(LocalDate.of(2026, 6, 15))
                recordRetro(LocalDate.of(2026, 6, 22))
            }
        val condition =
            BadgeCondition(
                BadgeConditionType.WEEKLY_STREAK,
                threshold = 4,
                params = mapOf("weeklyMinCount" to 1),
            )

        assertThat(checker.isSatisfied(condition, ctx(weeklyRetroStreak = streak))).isTrue()
    }

    @Test
    fun `WEEKLY_STREAK - weeklyMin 3 일 때 weeklyStreakWithMin3 가 threshold 이상이면 true`() {
        val condition =
            BadgeCondition(
                BadgeConditionType.WEEKLY_STREAK,
                threshold = 3,
                params = mapOf("weeklyMinCount" to 3),
            )

        assertThat(checker.isSatisfied(condition, ctx(weeklyStreakWithMin3 = 3))).isTrue()
        assertThat(checker.isSatisfied(condition, ctx(weeklyStreakWithMin3 = 2))).isFalse()
    }

    @Test
    fun `WEEKLY_STREAK - 지원하지 않는 weeklyMin은 false를 반환한다`() {
        val condition =
            BadgeCondition(
                BadgeConditionType.WEEKLY_STREAK,
                threshold = 2,
                params = mapOf("weeklyMinCount" to 5),
            )

        assertThat(checker.isSatisfied(condition, ctx())).isFalse()
    }

    @Test
    fun `DAILY_ACCESS_STREAK - currentStreak가 threshold 이상이면 true`() {
        val streak =
            DailyAccessStreak.create(userId).apply {
                for (i in 6 downTo 0) recordAccess(today.minusDays(i.toLong()))
            }
        val condition = BadgeCondition(BadgeConditionType.DAILY_ACCESS_STREAK, threshold = 7)

        assertThat(checker.isSatisfied(condition, ctx(dailyAccessStreak = streak))).isTrue()
    }

    @Test
    fun `PROJECT_COUNT - 프로젝트 수가 threshold 이상이면 true`() {
        val condition = BadgeCondition(BadgeConditionType.PROJECT_COUNT, threshold = 3)

        assertThat(checker.isSatisfied(condition, ctx(projectCount = 3))).isTrue()
        assertThat(checker.isSatisfied(condition, ctx(projectCount = 2))).isFalse()
    }

    @Test
    fun `PROJECT_TAGGED_RETRO - 태그된 회고 수가 threshold 이상이면 true`() {
        val condition = BadgeCondition(BadgeConditionType.PROJECT_TAGGED_RETRO, threshold = 5)

        assertThat(checker.isSatisfied(condition, ctx(projectAssignedRetroCount = 5))).isTrue()
    }

    @Test
    fun `PROJECT_RETRO_IN_ONE - 한 프로젝트 최대 회고 수가 threshold 이상이면 true`() {
        val condition = BadgeCondition(BadgeConditionType.PROJECT_RETRO_IN_ONE, threshold = 3)

        assertThat(checker.isSatisfied(condition, ctx(maxRetroInOneProject = 3))).isTrue()
        assertThat(checker.isSatisfied(condition, ctx(maxRetroInOneProject = 2))).isFalse()
    }
}
