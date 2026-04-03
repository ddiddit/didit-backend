package com.didit.application.achievement.required

import com.didit.domain.achievement.Streak
import com.didit.support.RepositoryTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class StreakRepositoryTest : RepositoryTestSupport() {
    @Autowired
    lateinit var streakRepository: StreakRepository

    private val userId = UUID.randomUUID()

    @Test
    fun `save - 스트릭을 저장한다`() {
        val streak = Streak.create(userId)

        val saved = streakRepository.save(streak)

        assertThat(saved.userId).isEqualTo(userId)
        assertThat(saved.currentStreak).isEqualTo(0)
    }

    @Test
    fun `findByUserId - 스트릭을 반환한다`() {
        streakRepository.save(Streak.create(userId))

        val found = streakRepository.findByUserId(userId)

        assertThat(found).isNotNull
        assertThat(found!!.userId).isEqualTo(userId)
    }

    @Test
    fun `findByUserId - 존재하지 않으면 null을 반환한다`() {
        val found = streakRepository.findByUserId(UUID.randomUUID())

        assertThat(found).isNull()
    }

    @Test
    fun `save - 스트릭을 업데이트한다`() {
        val streak = streakRepository.save(Streak.create(userId))
        streak.update(LocalDate.now())

        val updated = streakRepository.save(streak)

        assertThat(updated.currentStreak).isEqualTo(1)
        assertThat(updated.lastRetroDate).isEqualTo(LocalDate.now())
    }
}
