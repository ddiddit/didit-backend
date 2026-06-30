package com.didit.application.admin

import com.didit.application.achievement.required.BadgeRepository
import com.didit.application.achievement.required.LevelCount
import com.didit.application.achievement.required.MissionLevelStatusCount
import com.didit.application.achievement.required.UserBadgeRepository
import com.didit.application.achievement.required.UserLevelRepository
import com.didit.application.achievement.required.UserMissionRepository
import com.didit.domain.achievement.Badge
import com.didit.domain.achievement.BadgeCategory
import com.didit.domain.achievement.BadgeCondition
import com.didit.domain.achievement.BadgeConditionType
import com.didit.domain.achievement.MissionStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminAchievementStatsServiceTest {
    @Mock
    lateinit var userLevelRepository: UserLevelRepository

    @Mock
    lateinit var userMissionRepository: UserMissionRepository

    @Mock
    lateinit var badgeRepository: BadgeRepository

    @Mock
    lateinit var userBadgeRepository: UserBadgeRepository

    private fun service() = AdminAchievementStatsService(userLevelRepository, userMissionRepository, badgeRepository, userBadgeRepository)

    private fun levelCount(
        level: Int,
        count: Long,
    ) = object : LevelCount {
        override fun getLevel() = level

        override fun getCount() = count
    }

    private fun missionCount(
        level: Int,
        status: MissionStatus,
        count: Long,
    ) = object : MissionLevelStatusCount {
        override fun getLevel() = level

        override fun getStatus() = status

        override fun getCount() = count
    }

    @Test
    fun `레벨 분포는 1부터 10까지 빠짐없이 채운다`() {
        whenever(userLevelRepository.countGroupByLevel()).thenReturn(
            listOf(levelCount(1, 5), levelCount(3, 2)),
        )

        val result = service().getLevelStats()

        assertThat(result).hasSize(10)
        assertThat(result.first { it.level == 1 }.userCount).isEqualTo(5)
        assertThat(result.first { it.level == 2 }.userCount).isEqualTo(0)
        assertThat(result.first { it.level == 3 }.userCount).isEqualTo(2)
    }

    @Test
    fun `미션 통계는 상태별로 집계하고 완료율을 계산한다`() {
        whenever(userMissionRepository.countGroupByLevelAndStatus()).thenReturn(
            listOf(
                missionCount(2, MissionStatus.IN_PROGRESS, 3),
                missionCount(2, MissionStatus.COMPLETED, 5),
                missionCount(2, MissionStatus.WAIT_CONFIRM, 1),
                missionCount(2, MissionStatus.FAILED, 1),
            ),
        )

        val result = service().getMissionStats()

        assertThat(result).hasSize(1)
        val lv2 = result.first { it.level == 2 }
        assertThat(lv2.inProgress).isEqualTo(3)
        assertThat(lv2.completed).isEqualTo(5)
        assertThat(lv2.failed).isEqualTo(2)
        assertThat(lv2.total).isEqualTo(10)
        assertThat(lv2.completionRate).isEqualTo(50.0)
    }

    @Test
    fun `배지 통계는 배지별 획득 수를 매핑한다`() {
        val badgeId = UUID.randomUUID()
        val badge =
            Badge(
                id = badgeId,
                name = "첫 기록",
                description = "설명",
                category = BadgeCategory.CONSISTENCY,
                condition = BadgeCondition(BadgeConditionType.CUMULATIVE_RETRO, 1),
            )
        whenever(badgeRepository.findAll()).thenReturn(listOf(badge))
        whenever(userBadgeRepository.countByBadgeId(any())).thenReturn(42L)

        val result = service().getBadgeStats()

        assertThat(result).hasSize(1)
        assertThat(result[0].badgeId).isEqualTo(badgeId)
        assertThat(result[0].name).isEqualTo("첫 기록")
        assertThat(result[0].acquiredCount).isEqualTo(42L)
    }
}
