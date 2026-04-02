package com.didit.application.achievement

import com.didit.domain.achievement.BadgeConditionType
import com.didit.domain.achievement.Streak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

class BadgeConditionCheckerTest {
    private lateinit var checker: BadgeConditionChecker
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        checker = BadgeConditionChecker()
    }

    private fun context(
        totalRetroCount: Int = 1,
        streak: Streak = Streak.create(userId),
        deepQuestionCount: Int = 0,
        retroDate: LocalDate = LocalDate.now(),
        weeklyCountByDayOfWeek: Map<DayOfWeek, Int> = emptyMap(),
        weeklyGoalAchievedWeeks: Int = 0,
    ) = BadgeCheckContext(
        userId = userId,
        totalRetroCount = totalRetroCount,
        streak = streak,
        deepQuestionCount = deepQuestionCount,
        retroDate = retroDate,
        weeklyCountByDayOfWeek = weeklyCountByDayOfWeek,
        weeklyGoalAchievedWeeks = weeklyGoalAchievedWeeks,
    )

    @Test
    fun `FIRST_RETRO - 첫 번째 회고이면 true를 반환한다`() {
        val context = context(totalRetroCount = 1)

        assertThat(checker.isSatisfied(BadgeConditionType.FIRST_RETRO, context)).isTrue()
    }

    @Test
    fun `FIRST_RETRO - 첫 번째 회고가 아니면 false를 반환한다`() {
        val context = context(totalRetroCount = 2)

        assertThat(checker.isSatisfied(BadgeConditionType.FIRST_RETRO, context)).isFalse()
    }

    @Test
    fun `STREAK_3_DAYS - 3일 연속이면 true를 반환한다`() {
        val streak =
            Streak.create(userId).apply {
                val today = LocalDate.now()
                update(today.minusDays(2))
                update(today.minusDays(1))
                update(today)
            }
        val context = context(streak = streak)

        assertThat(checker.isSatisfied(BadgeConditionType.STREAK_3_DAYS, context)).isTrue()
    }

    @Test
    fun `STREAK_3_DAYS - 3일 연속이 아니면 false를 반환한다`() {
        val streak =
            Streak.create(userId).apply {
                update(LocalDate.now())
            }
        val context = context(streak = streak)

        assertThat(checker.isSatisfied(BadgeConditionType.STREAK_3_DAYS, context)).isFalse()
    }

    @Test
    fun `TOTAL_30 - 누적 30회 이상이면 true를 반환한다`() {
        val context = context(totalRetroCount = 30)

        assertThat(checker.isSatisfied(BadgeConditionType.TOTAL_30, context)).isTrue()
    }

    @Test
    fun `TOTAL_30 - 누적 30회 미만이면 false를 반환한다`() {
        val context = context(totalRetroCount = 29)

        assertThat(checker.isSatisfied(BadgeConditionType.TOTAL_30, context)).isFalse()
    }

    @Test
    fun `DEEP_QUESTION_1 - 심화질문 1회 이상이면 true를 반환한다`() {
        val context = context(deepQuestionCount = 1)

        assertThat(checker.isSatisfied(BadgeConditionType.DEEP_QUESTION_1, context)).isTrue()
    }

    @Test
    fun `DEEP_QUESTION_5 - 심화질문 5회 이상이면 true를 반환한다`() {
        val context = context(deepQuestionCount = 5)

        assertThat(checker.isSatisfied(BadgeConditionType.DEEP_QUESTION_5, context)).isTrue()
    }

    @Test
    fun `DEEP_QUESTION_10 - 심화질문 10회 이상이면 true를 반환한다`() {
        val context = context(deepQuestionCount = 10)

        assertThat(checker.isSatisfied(BadgeConditionType.DEEP_QUESTION_10, context)).isTrue()
    }

    @Test
    fun `DEEP_QUESTION_5 - 심화질문 5회 미만이면 false를 반환한다`() {
        val context = context(deepQuestionCount = 4)

        assertThat(checker.isSatisfied(BadgeConditionType.DEEP_QUESTION_5, context)).isFalse()
    }

    @Test
    fun `WEEKLY_MON - 월요일 3회 이상이면 true를 반환한다`() {
        val context = context(weeklyCountByDayOfWeek = mapOf(DayOfWeek.MONDAY to 3))

        assertThat(checker.isSatisfied(BadgeConditionType.WEEKLY_MON, context)).isTrue()
    }

    @Test
    fun `WEEKLY_MON - 월요일 3회 미만이면 false를 반환한다`() {
        val context = context(weeklyCountByDayOfWeek = mapOf(DayOfWeek.MONDAY to 2))

        assertThat(checker.isSatisfied(BadgeConditionType.WEEKLY_MON, context)).isFalse()
    }

    @Test
    fun `WEEKLY_3_FIRST - 주 3회 첫 달성이면 true를 반환한다`() {
        val context = context(weeklyGoalAchievedWeeks = 1)

        assertThat(checker.isSatisfied(BadgeConditionType.WEEKLY_3_FIRST, context)).isTrue()
    }

    @Test
    fun `WEEKLY_3_THREE_WEEKS - 3주 이상 주 3회 달성이면 true를 반환한다`() {
        val context = context(weeklyGoalAchievedWeeks = 3)

        assertThat(checker.isSatisfied(BadgeConditionType.WEEKLY_3_THREE_WEEKS, context)).isTrue()
    }

    @Test
    fun `WEEKLY_3_THREE_WEEKS - 3주 미만이면 false를 반환한다`() {
        val context = context(weeklyGoalAchievedWeeks = 2)

        assertThat(checker.isSatisfied(BadgeConditionType.WEEKLY_3_THREE_WEEKS, context)).isFalse()
    }
}
