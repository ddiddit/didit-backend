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
    }
}
