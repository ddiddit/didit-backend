package com.didit.application.admin

import com.didit.application.achievement.required.BadgeRepository
import com.didit.application.achievement.required.UserBadgeRepository
import com.didit.application.admin.provided.AdminBadgeCreateCommand
import com.didit.application.admin.provided.AdminBadgeUpdateCommand
import com.didit.application.common.exception.BusinessException
import com.didit.domain.achievement.Badge
import com.didit.domain.achievement.BadgeCategory
import com.didit.domain.achievement.BadgeCondition
import com.didit.domain.achievement.BadgeConditionType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminBadgeManagementServiceTest {
    @Mock
    lateinit var badgeRepository: BadgeRepository

    @Mock
    lateinit var userBadgeRepository: UserBadgeRepository

    private lateinit var service: AdminBadgeManagementService

    private fun setUp() {
        service = AdminBadgeManagementService(badgeRepository, userBadgeRepository)
    }

    private fun createCommand(
        category: String = "CONSISTENCY",
        conditionType: String = "CUMULATIVE_RETRO",
        threshold: Int = 10,
        params: Map<String, Any>? = null,
    ) = AdminBadgeCreateCommand(
        name = "10회 기록",
        description = "회고 10회 작성",
        category = category,
        conditionType = conditionType,
        threshold = threshold,
        params = params,
    )

    @Test
    fun `배지를 생성한다`() {
        setUp()
        whenever(badgeRepository.save(any())).thenAnswer { it.arguments[0] as Badge }
        whenever(userBadgeRepository.countByBadgeId(any())).thenReturn(0L)

        val result = service.create(createCommand())

        assertThat(result.name).isEqualTo("10회 기록")
        assertThat(result.category).isEqualTo("CONSISTENCY")
        assertThat(result.conditionType).isEqualTo("CUMULATIVE_RETRO")
        assertThat(result.threshold).isEqualTo(10)
        assertThat(result.active).isTrue()
        assertThat(result.acquiredCount).isEqualTo(0L)
    }

    @Test
    fun `WEEKLY_STREAK params를 보존한다`() {
        setUp()
        whenever(badgeRepository.save(any())).thenAnswer { it.arguments[0] as Badge }
        whenever(userBadgeRepository.countByBadgeId(any())).thenReturn(0L)

        val result =
            service.create(
                createCommand(
                    category = "PATTERN",
                    conditionType = "WEEKLY_STREAK",
                    threshold = 3,
                    params = mapOf("weeklyMinCount" to 3),
                ),
            )

        assertThat(result.params).isEqualTo(mapOf("weeklyMinCount" to 3))
    }

    @Test
    fun `WEEKLY_STREAK weeklyMinCount가 1·3이 아니면 예외`() {
        setUp()

        assertThatThrownBy {
            service.create(
                createCommand(
                    category = "PATTERN",
                    conditionType = "WEEKLY_STREAK",
                    threshold = 3,
                    params = mapOf("weeklyMinCount" to 5),
                ),
            )
        }.isInstanceOf(BusinessException::class.java)
    }

    @Test
    fun `유효하지 않은 category면 예외`() {
        setUp()

        assertThatThrownBy { service.create(createCommand(category = "UNKNOWN")) }
            .isInstanceOf(BusinessException::class.java)
    }

    @Test
    fun `유효하지 않은 conditionType이면 예외`() {
        setUp()

        assertThatThrownBy { service.create(createCommand(conditionType = "UNKNOWN")) }
            .isInstanceOf(BusinessException::class.java)
    }

    @Test
    fun `threshold가 1 미만이면 예외`() {
        setUp()

        assertThatThrownBy { service.create(createCommand(threshold = 0)) }
            .isInstanceOf(BusinessException::class.java)
    }

    @Test
    fun `배지를 수정한다`() {
        setUp()
        val badgeId = UUID.randomUUID()
        val badge =
            Badge(
                id = badgeId,
                name = "옛 이름",
                description = "옛 설명",
                category = BadgeCategory.CONSISTENCY,
                condition = BadgeCondition(BadgeConditionType.CUMULATIVE_RETRO, 10),
            )
        whenever(badgeRepository.findById(badgeId)).thenReturn(badge)
        whenever(badgeRepository.save(any())).thenAnswer { it.arguments[0] as Badge }
        whenever(userBadgeRepository.countByBadgeId(any())).thenReturn(5L)

        val result =
            service.update(
                badgeId,
                AdminBadgeUpdateCommand(
                    name = "새 이름",
                    description = "새 설명",
                    category = "CONSISTENCY",
                    conditionType = "CUMULATIVE_RETRO",
                    threshold = 30,
                ),
            )

        assertThat(result.name).isEqualTo("새 이름")
        assertThat(result.threshold).isEqualTo(30)
        assertThat(badge.name).isEqualTo("새 이름")
        assertThat(badge.condition.threshold).isEqualTo(30)
    }

    @Test
    fun `존재하지 않는 배지 수정 시 예외`() {
        setUp()
        val badgeId = UUID.randomUUID()
        whenever(badgeRepository.findById(badgeId)).thenReturn(null)

        assertThatThrownBy {
            service.update(
                badgeId,
                AdminBadgeUpdateCommand(
                    name = "x",
                    description = "x",
                    category = "CONSISTENCY",
                    conditionType = "CUMULATIVE_RETRO",
                    threshold = 1,
                ),
            )
        }.isInstanceOf(BusinessException::class.java)
    }

    @Test
    fun `배지 활성 상태를 변경한다`() {
        setUp()
        val badgeId = UUID.randomUUID()
        val badge =
            Badge(
                id = badgeId,
                name = "배지",
                description = "설명",
                category = BadgeCategory.CONSISTENCY,
                condition = BadgeCondition(BadgeConditionType.CUMULATIVE_RETRO, 10),
            )
        whenever(badgeRepository.findById(badgeId)).thenReturn(badge)
        whenever(badgeRepository.save(any())).thenAnswer { it.arguments[0] as Badge }
        whenever(userBadgeRepository.countByBadgeId(any())).thenReturn(0L)

        val result = service.changeActive(badgeId, false)

        assertThat(result.active).isFalse()
        assertThat(badge.active).isFalse()
    }
}
