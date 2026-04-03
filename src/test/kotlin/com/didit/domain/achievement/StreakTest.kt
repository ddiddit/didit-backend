package com.didit.domain.achievement

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class StreakTest {
    private val userId = UUID.randomUUID()

    @Test
    fun `연속 기록 - 첫 회고 시 streak이 1이 된다`() {
        val streak = Streak.create(userId)

        streak.update(LocalDate.now())

        assertThat(streak.currentStreak).isEqualTo(1)
        assertThat(streak.longestStreak).isEqualTo(1)
    }

    @Test
    fun `연속 기록 - 연속된 날짜면 streak이 증가한다`() {
        val streak = Streak.create(userId)
        val today = LocalDate.now()

        streak.update(today.minusDays(1))
        streak.update(today)

        assertThat(streak.currentStreak).isEqualTo(2)
        assertThat(streak.longestStreak).isEqualTo(2)
    }

    @Test
    fun `연속 기록 - 연속되지 않으면 streak이 1로 초기화된다`() {
        val streak = Streak.create(userId)
        val today = LocalDate.now()

        streak.update(today.minusDays(3))
        streak.update(today)

        assertThat(streak.currentStreak).isEqualTo(1)
        assertThat(streak.longestStreak).isEqualTo(1)
    }

    @Test
    fun `연속 기록 - 같은 날 중복 호출 시 streak이 변하지 않는다`() {
        val streak = Streak.create(userId)
        val today = LocalDate.now()

        streak.update(today)
        streak.update(today)

        assertThat(streak.currentStreak).isEqualTo(1)
    }

    @Test
    fun `연속 기록 - longestStreak은 최댓값을 유지한다`() {
        val streak = Streak.create(userId)
        val today = LocalDate.now()

        streak.update(today.minusDays(2))
        streak.update(today.minusDays(1))
        streak.update(today)
        streak.update(today.plusDays(2)) // 끊김

        assertThat(streak.currentStreak).isEqualTo(1)
        assertThat(streak.longestStreak).isEqualTo(3)
    }

    @Test
    fun `isStreak - 조건 충족 시 true를 반환한다`() {
        val streak = Streak.create(userId)
        val today = LocalDate.now()

        streak.update(today.minusDays(2))
        streak.update(today.minusDays(1))
        streak.update(today)

        assertThat(streak.isStreak(3)).isTrue()
    }

    @Test
    fun `isStreak - 조건 미충족 시 false를 반환한다`() {
        val streak = Streak.create(userId)

        streak.update(LocalDate.now())

        assertThat(streak.isStreak(3)).isFalse()
    }
}
