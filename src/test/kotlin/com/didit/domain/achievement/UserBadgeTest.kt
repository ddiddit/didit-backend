package com.didit.domain.achievement

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class UserBadgeTest {
    @Test
    fun `create - userId와 badgeId로 UserBadge를 생성한다`() {
        val userId = UUID.randomUUID()
        val badgeId = UUID.randomUUID()

        val userBadge = UserBadge.create(userId, badgeId)

        assertThat(userBadge.userId).isEqualTo(userId)
        assertThat(userBadge.badgeId).isEqualTo(badgeId)
        assertThat(userBadge.isNotified).isFalse()
    }

    @Test
    fun `markAsNotified - isNotified가 true로 변경된다`() {
        val userBadge = UserBadge.create(UUID.randomUUID(), UUID.randomUUID())

        userBadge.markAsNotified()

        assertThat(userBadge.isNotified).isTrue()
    }
}
