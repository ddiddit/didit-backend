package com.didit.application.achievement

import com.didit.application.achievement.required.BadgeRepository
import com.didit.application.achievement.required.UserBadgeRepository
import com.didit.domain.achievement.Badge
import com.didit.domain.achievement.BadgeConditionType
import com.didit.domain.achievement.UserBadge
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class BadgeQueryServiceTest {
    @Mock lateinit var badgeRepository: BadgeRepository

    @Mock lateinit var userBadgeRepository: UserBadgeRepository

    private lateinit var badgeQueryService: BadgeQueryService

    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        badgeQueryService =
            BadgeQueryService(
                badgeRepository = badgeRepository,
                userBadgeRepository = userBadgeRepository,
            )
    }

    private fun badge(conditionType: BadgeConditionType) =
        Badge.create(
            name = conditionType.name,
            description = "설명",
            conditionType = conditionType,
        )

    @Test
    fun `findAll - 전체 배지 목록을 반환한다`() {
        val badges =
            listOf(
                badge(BadgeConditionType.FIRST_RETRO),
                badge(BadgeConditionType.TOTAL_30),
            )
        whenever(badgeRepository.findAll()).thenReturn(badges)
        whenever(userBadgeRepository.findAllByUserId(userId)).thenReturn(emptyList())

        val result = badgeQueryService.findAll(userId)

        assertThat(result).hasSize(2)
        assertThat(result).allMatch { !it.acquired }
    }

    @Test
    fun `findAll - 획득한 배지는 acquired가 true이다`() {
        val badge = badge(BadgeConditionType.FIRST_RETRO)
        whenever(badgeRepository.findAll()).thenReturn(listOf(badge))
        whenever(userBadgeRepository.findAllByUserId(userId)).thenReturn(
            listOf(UserBadge.create(userId, badge.id)),
        )

        val result = badgeQueryService.findAll(userId)

        assertThat(result).hasSize(1)
        assertThat(result[0].acquired).isTrue()
        assertThat(result[0].acquiredAt).isNotNull()
    }

    @Test
    fun `findAll - 미획득 배지는 acquired가 false이다`() {
        val badge = badge(BadgeConditionType.FIRST_RETRO)
        whenever(badgeRepository.findAll()).thenReturn(listOf(badge))
        whenever(userBadgeRepository.findAllByUserId(userId)).thenReturn(emptyList())

        val result = badgeQueryService.findAll(userId)

        assertThat(result).hasSize(1)
        assertThat(result[0].acquired).isFalse()
        assertThat(result[0].acquiredAt).isNull()
    }

    @Test
    fun `findRecent - 최근 획득 배지 3개를 반환한다`() {
        val badge1 = badge(BadgeConditionType.FIRST_RETRO)
        val badge2 = badge(BadgeConditionType.STREAK_3_DAYS)
        val badge3 = badge(BadgeConditionType.TOTAL_30)

        whenever(userBadgeRepository.findTop3ByUserIdOrderByAcquiredAtDesc(userId)).thenReturn(
            listOf(
                UserBadge.create(userId, badge1.id),
                UserBadge.create(userId, badge2.id),
                UserBadge.create(userId, badge3.id),
            ),
        )
        whenever(badgeRepository.findAll()).thenReturn(listOf(badge1, badge2, badge3))

        val result = badgeQueryService.findRecent(userId)

        assertThat(result).hasSize(3)
        assertThat(result).allMatch { it.acquired }
    }

    @Test
    fun `findRecent - 획득한 배지가 없으면 빈 목록을 반환한다`() {
        whenever(userBadgeRepository.findTop3ByUserIdOrderByAcquiredAtDesc(userId)).thenReturn(emptyList())
        whenever(badgeRepository.findAll()).thenReturn(emptyList())

        val result = badgeQueryService.findRecent(userId)

        assertThat(result).isEmpty()
    }
}
