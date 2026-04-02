package com.didit.domain.achievement

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BadgeTest {
    private fun badge(conditionType: BadgeConditionType) =
        Badge.create(
            name = "첫 기록",
            description = "첫 회고 저장 완료",
            conditionType = conditionType,
        )

    @Test
    fun `isSameCondition - 같은 조건 타입이면 true를 반환한다`() {
        val badge = badge(BadgeConditionType.FIRST_RETRO)

        assertThat(badge.isSameCondition(BadgeConditionType.FIRST_RETRO)).isTrue()
    }

    @Test
    fun `isSameCondition - 다른 조건 타입이면 false를 반환한다`() {
        val badge = badge(BadgeConditionType.FIRST_RETRO)

        assertThat(badge.isSameCondition(BadgeConditionType.TOTAL_30)).isFalse()
    }
}
