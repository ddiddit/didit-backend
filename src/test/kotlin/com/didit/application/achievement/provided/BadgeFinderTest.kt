package com.didit.application.achievement.provided

import com.didit.application.achievement.dto.BadgeResponse
import com.didit.domain.achievement.BadgeConditionType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class BadgeFinderTest {
    @Mock
    lateinit var badgeFinder: BadgeFinder

    private val userId = UUID.randomUUID()

    private fun badgeResponse(
        acquired: Boolean = false,
        conditionType: BadgeConditionType = BadgeConditionType.FIRST_RETRO,
    ) = BadgeResponse(
        id = UUID.randomUUID(),
        name = conditionType.name,
        description = "설명",
        conditionType = conditionType.name,
        acquired = acquired,
        acquiredAt = null,
    )

    @Test
    fun `findAll - 전체 배지 목록을 반환한다`() {
        val badges =
            listOf(
                badgeResponse(acquired = true),
                badgeResponse(acquired = false),
            )
        whenever(badgeFinder.findAll(userId)).thenReturn(badges)

        val result = badgeFinder.findAll(userId)

        verify(badgeFinder).findAll(userId)
        assertThat(result).hasSize(2)
    }

    @Test
    fun `findRecent - 최근 획득 배지 3개를 반환한다`() {
        val badges =
            listOf(
                badgeResponse(acquired = true),
                badgeResponse(acquired = true),
                badgeResponse(acquired = true),
            )
        whenever(badgeFinder.findRecent(userId)).thenReturn(badges)

        val result = badgeFinder.findRecent(userId)

        verify(badgeFinder).findRecent(userId)
        assertThat(result).hasSize(3)
        assertThat(result).allMatch { it.acquired }
    }

    @Test
    fun `findUnnotified - 미알림 배지 목록을 반환한다`() {
        val badges =
            listOf(
                badgeResponse(acquired = true, conditionType = BadgeConditionType.FIRST_RETRO),
                badgeResponse(acquired = true, conditionType = BadgeConditionType.STREAK_3_DAYS),
            )
        whenever(badgeFinder.findUnnotified(userId)).thenReturn(badges)

        val result = badgeFinder.findUnnotified(userId)

        verify(badgeFinder).findUnnotified(userId)
        assertThat(result).hasSize(2)
        assertThat(result).allMatch { it.acquired }
    }
}
