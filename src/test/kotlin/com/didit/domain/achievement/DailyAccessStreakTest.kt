package com.didit.domain.achievement

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DailyAccessStreakTest {
    private val userId = UUID.randomUUID()

    @Test
    fun `일일 접속 - 첫 접속 시 currentStreak이 1이 된다`() {
        val streak = DailyAccessStreak.create(userId)

        streak.recordAccess(LocalDate.now())

        assertThat(streak.currentStreak).isEqualTo(1)
        assertThat(streak.longestStreak).isEqualTo(1)
    }

    @Test
    fun `일일 접속 - 연속된 날짜면 currentStreak이 증가한다`() {
        val streak = DailyAccessStreak.create(userId)
        val today = LocalDate.now()

        streak.recordAccess(today.minusDays(1))
        streak.recordAccess(today)

        assertThat(streak.currentStreak).isEqualTo(2)
        assertThat(streak.longestStreak).isEqualTo(2)
    }

    @Test
    fun `일일 접속 - 같은 날 중복 호출 시 변하지 않는다`() {
        val streak = DailyAccessStreak.create(userId)
        val today = LocalDate.now()

        streak.recordAccess(today)
        streak.recordAccess(today)

        assertThat(streak.currentStreak).isEqualTo(1)
        assertThat(streak.lastAccessDate).isEqualTo(today)
    }

    @Test
    fun `일일 접속 - 하루 건너뛰면 currentStreak이 1로 초기화된다`() {
        val streak = DailyAccessStreak.create(userId)
        val today = LocalDate.now()

        streak.recordAccess(today.minusDays(3))
        streak.recordAccess(today)

        assertThat(streak.currentStreak).isEqualTo(1)
        assertThat(streak.longestStreak).isEqualTo(1)
    }

    @Test
    fun `일일 접속 - longestStreak은 최댓값을 유지한다`() {
        val streak = DailyAccessStreak.create(userId)
        val today = LocalDate.now()

        streak.recordAccess(today.minusDays(6))
        streak.recordAccess(today.minusDays(5))
        streak.recordAccess(today.minusDays(4))
        streak.recordAccess(today)

        assertThat(streak.currentStreak).isEqualTo(1)
        assertThat(streak.longestStreak).isEqualTo(3)
    }

    @Test
    fun `isStreak - 7일 연속 시 디딧 러버 조건을 충족한다`() {
        val streak = DailyAccessStreak.create(userId)
        val today = LocalDate.now()

        for (i in 6 downTo 0) {
            streak.recordAccess(today.minusDays(i.toLong()))
        }

        assertThat(streak.currentStreak).isEqualTo(7)
        assertThat(streak.isStreak(7)).isTrue()
    }

    @Test
    fun `isStreak - 조건 미충족 시 false를 반환한다`() {
        val streak = DailyAccessStreak.create(userId)

        streak.recordAccess(LocalDate.now())

        assertThat(streak.isStreak(7)).isFalse()
    }
}
