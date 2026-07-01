package com.didit.domain.achievement

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class WeeklyRetroStreakTest {
    private val userId = UUID.randomUUID()

    // 2026-06-22(월) ~ 2026-06-28(일) 을 기준 주차로 사용
    private val monday = LocalDate.of(2026, 6, 22)
    private val wednesday = LocalDate.of(2026, 6, 24)
    private val sunday = LocalDate.of(2026, 6, 28)
    private val nextMonday = LocalDate.of(2026, 6, 29)
    private val nextSunday = LocalDate.of(2026, 7, 5)
    private val twoWeeksLaterMonday = LocalDate.of(2026, 7, 6)

    @Test
    fun `주간 회고 - 첫 회고 시 currentWeeks가 1이 된다`() {
        val streak = WeeklyRetroStreak.create(userId)

        streak.recordRetro(wednesday)

        assertThat(streak.currentWeeks).isEqualTo(1)
        assertThat(streak.longestWeeks).isEqualTo(1)
        assertThat(streak.lastAchievedWeek).isEqualTo(monday)
    }

    @Test
    fun `주간 회고 - 같은 주 추가 회고는 카운트가 변하지 않는다`() {
        val streak = WeeklyRetroStreak.create(userId)

        streak.recordRetro(monday)
        streak.recordRetro(wednesday)
        streak.recordRetro(sunday)

        assertThat(streak.currentWeeks).isEqualTo(1)
        assertThat(streak.lastAchievedWeek).isEqualTo(monday)
    }

    @Test
    fun `주간 회고 - 다음 주 첫 회고면 currentWeeks가 증가한다`() {
        val streak = WeeklyRetroStreak.create(userId)

        streak.recordRetro(wednesday)
        streak.recordRetro(nextMonday)

        assertThat(streak.currentWeeks).isEqualTo(2)
        assertThat(streak.longestWeeks).isEqualTo(2)
        assertThat(streak.lastAchievedWeek).isEqualTo(nextMonday)
    }

    @Test
    fun `주간 회고 - 한 주를 건너뛰면 currentWeeks가 1로 초기화된다`() {
        val streak = WeeklyRetroStreak.create(userId)

        streak.recordRetro(monday)
        streak.recordRetro(twoWeeksLaterMonday)

        assertThat(streak.currentWeeks).isEqualTo(1)
        assertThat(streak.longestWeeks).isEqualTo(1)
        assertThat(streak.lastAchievedWeek).isEqualTo(twoWeeksLaterMonday)
    }

    @Test
    fun `주간 회고 - longestWeeks는 최댓값을 유지한다`() {
        val streak = WeeklyRetroStreak.create(userId)

        streak.recordRetro(monday)
        streak.recordRetro(nextMonday)
        streak.recordRetro(LocalDate.of(2026, 7, 20)) // 갭 → 리셋

        assertThat(streak.currentWeeks).isEqualTo(1)
        assertThat(streak.longestWeeks).isEqualTo(2)
    }

    @Test
    fun `주간 회고 - 일요일 저장과 그 다음 월요일 저장은 다른 주로 인식한다`() {
        val streak = WeeklyRetroStreak.create(userId)

        streak.recordRetro(sunday)
        streak.recordRetro(nextMonday)

        assertThat(streak.currentWeeks).isEqualTo(2)
        assertThat(streak.lastAchievedWeek).isEqualTo(nextMonday)
    }

    @Test
    fun `주간 회고 - 주 내 어느 요일에 저장해도 lastAchievedWeek은 그 주 월요일이다`() {
        val streak = WeeklyRetroStreak.create(userId)

        streak.recordRetro(nextSunday)

        assertThat(streak.lastAchievedWeek).isEqualTo(nextMonday)
    }

    @Test
    fun `isStreak - 조건 충족 시 true를 반환한다`() {
        val streak = WeeklyRetroStreak.create(userId)

        streak.recordRetro(monday)
        streak.recordRetro(nextMonday)

        assertThat(streak.isStreak(2)).isTrue()
    }

    @Test
    fun `isStreak - 조건 미충족 시 false를 반환한다`() {
        val streak = WeeklyRetroStreak.create(userId)

        streak.recordRetro(monday)

        assertThat(streak.isStreak(2)).isFalse()
    }
}
