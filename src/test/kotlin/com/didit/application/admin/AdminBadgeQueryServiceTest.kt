package com.didit.application.admin

import com.didit.application.achievement.required.BadgeRepository
import com.didit.application.achievement.required.UserBadgeRepository
import com.didit.application.auth.required.UserRepository
import com.didit.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminBadgeQueryServiceTest {
    @Mock
    lateinit var badgeRepository: BadgeRepository

    @Mock
    lateinit var userBadgeRepository: UserBadgeRepository

    @Mock
    lateinit var userRepository: UserRepository

    @InjectMocks
    lateinit var adminBadgeQueryService: AdminBadgeQueryService

    @Test
    fun `배지 보유 유저 목록 - 보유자가 없으면 빈 목록 반환`() {
        val badgeId = UUID.randomUUID()
        whenever(userBadgeRepository.findAllByBadgeId(badgeId)).thenReturn(emptyList())

        val result = adminBadgeQueryService.findHolders(badgeId)

        assertThat(result).isEmpty()
        verify(userRepository, never()).findById(any())
    }

    @Test
    fun `배지 보유 유저 목록 - 유저 정보가 매핑된다`() {
        val badgeId = UUID.randomUUID()
        val user = UserFixture.createOnboarded()
        val userBadge =
            com.didit.domain.achievement
                .UserBadge(userId = user.id, badgeId = badgeId)

        whenever(userBadgeRepository.findAllByBadgeId(badgeId)).thenReturn(listOf(userBadge))
        whenever(userRepository.findById(user.id)).thenReturn(user)

        val result = adminBadgeQueryService.findHolders(badgeId)

        assertThat(result).hasSize(1)
        assertThat(result[0].userId).isEqualTo(user.id)
        assertThat(result[0].email).isEqualTo(user.email)
    }

    @Test
    fun `조건 타입 메타는 7종 조건 타입과 4종 카테고리를 반환한다`() {
        val meta = adminBadgeQueryService.findMeta()

        assertThat(meta.conditionTypes).hasSize(7)
        assertThat(meta.categories).hasSize(4)
        val weeklyStreak = meta.conditionTypes.first { it.conditionType == "WEEKLY_STREAK" }
        assertThat(weeklyStreak.params).anyMatch { it.key == "weeklyMinCount" }
    }

    @Test
    fun `존재하지 않는 배지 상세 조회 시 예외`() {
        val badgeId = UUID.randomUUID()
        whenever(badgeRepository.findById(badgeId)).thenReturn(null)

        assertThatThrownBy { adminBadgeQueryService.findById(badgeId) }
            .isInstanceOf(com.didit.application.common.exception.BusinessException::class.java)
    }
}
