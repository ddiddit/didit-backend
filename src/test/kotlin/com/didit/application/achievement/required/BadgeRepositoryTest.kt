package com.didit.application.achievement.required

import com.didit.domain.achievement.BadgeConditionType
import com.didit.support.BadgeFixture
import com.didit.support.RepositoryTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BadgeRepositoryTest : RepositoryTestSupport() {
    @Autowired
    lateinit var badgeRepository: BadgeRepository

    @Test
    fun `save - 배지를 저장한다`() {
        val saved = badgeRepository.save(BadgeFixture.cumulativeRetro(1))

        assertThat(saved.conditionType).isEqualTo(BadgeConditionType.CUMULATIVE_RETRO)
        assertThat(saved.condition.threshold).isEqualTo(1)
    }

    @Test
    fun `save - WEEKLY_STREAK 배지는 params JSON을 보존한다`() {
        val saved = badgeRepository.save(BadgeFixture.weeklyStreak(threshold = 3, weeklyMin = 3))

        assertThat(saved.condition.weeklyMinCount()).isEqualTo(3)
    }
}
