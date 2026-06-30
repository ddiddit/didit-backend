package com.didit.application.achievement.required

import com.didit.domain.achievement.WeeklyRetroStreak
import com.didit.support.RepositoryTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class WeeklyRetroStreakRepositoryTest : RepositoryTestSupport() {
    @Autowired
    lateinit var weeklyRetroStreakRepository: WeeklyRetroStreakRepository

    private val userId = UUID.randomUUID()

    @Test
    fun `save - 주간 회고 스트릭을 저장한다`() {
        val streak = WeeklyRetroStreak.create(userId)

        val saved = weeklyRetroStreakRepository.save(streak)

        assertThat(saved.userId).isEqualTo(userId)
        assertThat(saved.currentWeeks).isEqualTo(0)
    }

    @Test
    fun `findByUserId - 저장된 스트릭을 반환한다`() {
        weeklyRetroStreakRepository.save(WeeklyRetroStreak.create(userId))

        val found = weeklyRetroStreakRepository.findByUserId(userId)

        assertThat(found).isNotNull
        assertThat(found!!.userId).isEqualTo(userId)
    }

    @Test
    fun `findByUserId - 존재하지 않으면 null을 반환한다`() {
        val found = weeklyRetroStreakRepository.findByUserId(UUID.randomUUID())

        assertThat(found).isNull()
    }

    @Test
    fun `save - 주간 스트릭을 업데이트한다`() {
        val streak = weeklyRetroStreakRepository.save(WeeklyRetroStreak.create(userId))
        streak.recordRetro(LocalDate.of(2026, 6, 24))

        val updated = weeklyRetroStreakRepository.save(streak)

        assertThat(updated.currentWeeks).isEqualTo(1)
        assertThat(updated.lastAchievedWeek).isEqualTo(LocalDate.of(2026, 6, 22))
    }
}
