package com.didit.application.achievement.required

import com.didit.domain.achievement.Badge
import com.didit.domain.achievement.BadgeConditionType
import com.didit.support.RepositoryTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BadgeRepositoryTest : RepositoryTestSupport() {
    @Autowired
    lateinit var badgeRepository: BadgeRepository

    private fun badge(conditionType: BadgeConditionType) =
        Badge.create(
            name = conditionType.name,
            description = "설명",
            conditionType = conditionType,
        )

    @Test
    fun `save - 배지를 저장한다`() {
        val saved = badgeRepository.save(badge(BadgeConditionType.FIRST_RETRO))

        assertThat(saved.conditionType).isEqualTo(BadgeConditionType.FIRST_RETRO)
    }

    @Test
    fun `findAll - 전체 배지를 반환한다`() {
        badgeRepository.save(badge(BadgeConditionType.FIRST_RETRO))
        badgeRepository.save(badge(BadgeConditionType.STREAK_3_DAYS))

        val result = badgeRepository.findAll()

        assertThat(result).hasSize(2)
    }
}
