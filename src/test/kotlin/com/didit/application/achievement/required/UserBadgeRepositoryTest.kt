package com.didit.application.achievement.required

import com.didit.domain.achievement.Badge
import com.didit.domain.achievement.UserBadge
import com.didit.support.BadgeFixture
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

    private fun savedCumulativeBadge(threshold: Int): Badge = badgeRepository.save(BadgeFixture.cumulativeRetro(threshold))

    @Test
    fun `save - 사용자 배지를 저장한다`() {
        val badge = savedCumulativeBadge(1)

        val userBadge = userBadgeRepository.save(UserBadge.create(userId, badge.id))

        assertThat(userBadge.userId).isEqualTo(userId)
        assertThat(userBadge.badgeId).isEqualTo(badge.id)
    }

    @Test
    fun `findAllByUserId - 사용자의 전체 배지를 반환한다`() {
        val badge1 = savedCumulativeBadge(1)
        val badge2 = savedCumulativeBadge(30)

        userBadgeRepository.save(UserBadge.create(userId, badge1.id))
        userBadgeRepository.save(UserBadge.create(userId, badge2.id))

        val result = userBadgeRepository.findAllByUserId(userId)

        assertThat(result).hasSize(2)
    }

    @Test
    fun `findAllByUserId - 다른 유저의 배지는 반환하지 않는다`() {
        val badge = savedCumulativeBadge(1)

        userBadgeRepository.save(UserBadge.create(userId, badge.id))
        userBadgeRepository.save(UserBadge.create(UUID.randomUUID(), badge.id))

        val result = userBadgeRepository.findAllByUserId(userId)

        assertThat(result).hasSize(1)
    }

    @Test
    fun `findAllByUserIdAndIsNotifiedFalse - 미알림 배지만 반환한다`() {
        val badge1 = savedCumulativeBadge(1)
        val badge2 = savedCumulativeBadge(10)

        userBadgeRepository.save(UserBadge.create(userId, badge1.id))

        val userBadge = userBadgeRepository.save(UserBadge.create(userId, badge2.id))
        userBadge.markAsNotified()
        userBadgeRepository.save(userBadge)

        val result = userBadgeRepository.findAllByUserIdAndIsNotifiedFalse(userId)

        assertThat(result).hasSize(1)
        assertThat(result[0].badgeId).isEqualTo(badge1.id)
    }

    @Test
    fun `findTop3ByUserIdOrderByAcquiredAtDesc - 최근 3개만 반환한다`() {
        val badges =
            listOf(
                BadgeFixture.cumulativeRetro(1),
                BadgeFixture.cumulativeRetro(10),
                BadgeFixture.cumulativeRetro(30),
                BadgeFixture.projectCount(3),
                BadgeFixture.dailyAccessStreak(7),
            )
        badges.forEach { badge ->
            badgeRepository.save(badge)
            userBadgeRepository.save(UserBadge.create(userId, badge.id))
        }

        val result = userBadgeRepository.findTop3ByUserIdOrderByAcquiredAtDesc(userId)

        assertThat(result).hasSize(3)
    }
}
