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
    fun `saveAll - 배지를 저장한다`() {
        val badges =
            listOf(
                badge(BadgeConditionType.FIRST_RETRO),
                badge(BadgeConditionType.TOTAL_30),
            )

        val saved = badgeRepository.saveAll(badges)

        assertThat(saved).hasSize(2)
    }

    @Test
    fun `findAll - 전체 배지를 반환한다`() {
        badgeRepository.saveAll(
            listOf(
                badge(BadgeConditionType.FIRST_RETRO),
                badge(BadgeConditionType.STREAK_3_DAYS),
            ),
        )

        val result = badgeRepository.findAll()

        assertThat(result).hasSize(2)
    }
}
