package com.didit.domain.achievement

import com.didit.support.BadgeFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BadgeTest {
    @Test
    fun `isSameCondition - 같은 조건 타입이면 true를 반환한다`() {
        val badge = BadgeFixture.cumulativeRetro(1)

        assertThat(badge.isSameCondition(BadgeConditionType.CUMULATIVE_RETRO)).isTrue()
    }

    @Test
    fun `isSameCondition - 다른 조건 타입이면 false를 반환한다`() {
        val badge = BadgeFixture.cumulativeRetro(1)

        assertThat(badge.isSameCondition(BadgeConditionType.DAILY_ACCESS_STREAK)).isFalse()
    }

    @Test
    fun `conditionType - condition VO의 conditionType을 노출한다`() {
        val badge = BadgeFixture.weeklyStreak(threshold = 3, weeklyMin = 3)

        assertThat(badge.conditionType).isEqualTo(BadgeConditionType.WEEKLY_STREAK)
        assertThat(badge.condition.threshold).isEqualTo(3)
        assertThat(badge.condition.weeklyMinCount()).isEqualTo(3)
    }
}
