package com.didit.application.achievement.required

import com.didit.domain.achievement.Badge
import com.didit.domain.achievement.BadgeConditionType
import com.didit.domain.achievement.UserBadge
import com.didit.support.RepositoryTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class UserBadgeRepositoryTest : RepositoryTestSupport() {
    @Autowired
    lateinit var userBadgeRepository: UserBadgeRepository

    @Autowired
    lateinit var badgeRepository: BadgeRepository

    private val userId = UUID.randomUUID()

    private fun savedBadge(conditionType: BadgeConditionType): Badge =
        badgeRepository
            .saveAll(
                listOf(
                    Badge.create(
                        name = conditionType.name,
                        description = "설명",
                        conditionType = conditionType,
                    ),
                ),
            ).first()

    @Test
    fun `save - 사용자 배지를 저장한다`() {
        val badge = savedBadge(BadgeConditionType.FIRST_RETRO)

        val userBadge = userBadgeRepository.save(UserBadge.create(userId, badge.id))

        assertThat(userBadge.userId).isEqualTo(userId)
        assertThat(userBadge.badgeId).isEqualTo(badge.id)
    }

    @Test
    fun `findAllByUserId - 사용자의 전체 배지를 반환한다`() {
        val badge1 = savedBadge(BadgeConditionType.FIRST_RETRO)
        val badge2 = savedBadge(BadgeConditionType.TOTAL_30)

        userBadgeRepository.save(UserBadge.create(userId, badge1.id))
        userBadgeRepository.save(UserBadge.create(userId, badge2.id))

        val result = userBadgeRepository.findAllByUserId(userId)

        assertThat(result).hasSize(2)
    }

    @Test
    fun `findAllByUserId - 다른 유저의 배지는 반환하지 않는다`() {
        val badge = savedBadge(BadgeConditionType.FIRST_RETRO)
        userBadgeRepository.save(UserBadge.create(userId, badge.id))
        userBadgeRepository.save(UserBadge.create(UUID.randomUUID(), badge.id))

        val result = userBadgeRepository.findAllByUserId(userId)

        assertThat(result).hasSize(1)
    }

    @Test
    fun `findTop3ByUserIdOrderByAcquiredAtDesc - 최근 3개만 반환한다`() {
        repeat(5) {
            val badge = savedBadge(BadgeConditionType.entries[it])
            userBadgeRepository.save(UserBadge.create(userId, badge.id))
        }

        val result = userBadgeRepository.findTop3ByUserIdOrderByAcquiredAtDesc(userId)

        assertThat(result).hasSize(3)
    }
}
