package com.didit.application.achievement.required

import com.didit.domain.achievement.DailyAccessStreak
import com.didit.support.RepositoryTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class DailyAccessStreakRepositoryTest : RepositoryTestSupport() {
    @Autowired
    lateinit var dailyAccessStreakRepository: DailyAccessStreakRepository

    private val userId = UUID.randomUUID()

    @Test
    fun `save - 일일 접속 스트릭을 저장한다`() {
        val streak = DailyAccessStreak.create(userId)

        val saved = dailyAccessStreakRepository.save(streak)

        assertThat(saved.userId).isEqualTo(userId)
        assertThat(saved.currentStreak).isEqualTo(0)
    }

    @Test
    fun `findByUserId - 저장된 스트릭을 반환한다`() {
        dailyAccessStreakRepository.save(DailyAccessStreak.create(userId))

        val found = dailyAccessStreakRepository.findByUserId(userId)

        assertThat(found).isNotNull
        assertThat(found!!.userId).isEqualTo(userId)
    }

    @Test
    fun `findByUserId - 존재하지 않으면 null을 반환한다`() {
        val found = dailyAccessStreakRepository.findByUserId(UUID.randomUUID())

        assertThat(found).isNull()
    }

    @Test
    fun `save - 일일 스트릭을 업데이트한다`() {
        val streak = dailyAccessStreakRepository.save(DailyAccessStreak.create(userId))
        val today = LocalDate.now()
        streak.recordAccess(today)

        val updated = dailyAccessStreakRepository.save(streak)

        assertThat(updated.currentStreak).isEqualTo(1)
        assertThat(updated.lastAccessDate).isEqualTo(today)
    }
}
