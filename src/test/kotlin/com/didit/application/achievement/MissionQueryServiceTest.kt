package com.didit.application.achievement

import com.didit.application.achievement.exception.CurrentMissionNotFoundException
import com.didit.application.achievement.exception.MissionDefinitionNotFoundException
import com.didit.application.achievement.exception.UserLevelNotFoundException
import com.didit.application.achievement.required.MissionRepository
import com.didit.application.achievement.required.UserLevelRepository
import com.didit.application.achievement.required.UserMissionRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.achievement.Mission
import com.didit.domain.achievement.MissionStatus
import com.didit.domain.achievement.UserLevel
import com.didit.domain.achievement.UserMission
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class MissionQueryServiceTest {
    @Mock
    lateinit var userLevelRepository: UserLevelRepository

    @Mock
    lateinit var userMissionRepository: UserMissionRepository

    @Mock
    lateinit var missionRepository: MissionRepository

    @Mock
    lateinit var retrospectiveRepository: RetrospectiveRepository

    private lateinit var missionQueryService: MissionQueryService

    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        missionQueryService =
            MissionQueryService(
                userLevelRepository = userLevelRepository,
                userMissionRepository = userMissionRepository,
                missionRepository = missionRepository,
                retrospectiveRepository = retrospectiveRepository,
            )
    }

    @Test
    fun `Lv1 FIRST_RETRO 미션을 조회한다`() {
        val userLevel = UserLevel(userId = userId, currentLevel = 1)
        val mission = Mission.firstRetro()
        val userMission =
            UserMission(
                userId = userId,
                missionId = mission.id,
                status = MissionStatus.IN_PROGRESS,
                progress = 0,
                levelUpPopupShown = true,
                failurePopupShown = true,
            )

        whenever(userLevelRepository.findByUserId(userId)).thenReturn(userLevel)
        whenever(userMissionRepository.findCurrentMissionByUserId(userId)).thenReturn(userMission)
        whenever(missionRepository.findByLevel(1)).thenReturn(mission)

        val result = missionQueryService.getCurrentMission(userId)

        assertThat(result.currentLevel).isEqualTo(1)
        assertThat(result.mission.type).isEqualTo("FIRST_RETRO")
        assertThat(result.mission.progress).isEqualTo(0)
        assertThat(result.mission.target).isEqualTo(1)
        assertThat(result.mission.remainingDays).isNull()
        assertThat(result.weeklyStatus).isNull()
        assertThat(result.popup.exists).isFalse()
    }

    @Test
    fun `Lv2 TIME_LIMITED 미션의 남은 일수를 포함한다`() {
        val userLevel = UserLevel(userId = userId, currentLevel = 2)
        val mission = Mission.timeLimited()
        val startedAt = LocalDateTime.now().minusDays(2)
        val userMission =
            UserMission(
                userId = userId,
                missionId = mission.id,
                status = MissionStatus.IN_PROGRESS,
                progress = 1,
                startedAt = startedAt,
            )

        whenever(userLevelRepository.findByUserId(userId)).thenReturn(userLevel)
        whenever(userMissionRepository.findCurrentMissionByUserId(userId)).thenReturn(userMission)
        whenever(missionRepository.findByLevel(2)).thenReturn(mission)

        val result = missionQueryService.getCurrentMission(userId)

        assertThat(result.currentLevel).isEqualTo(2)
        assertThat(result.mission.type).isEqualTo("TIME_LIMITED")
        assertThat(result.mission.remainingDays).isEqualTo(5)
        assertThat(result.weeklyStatus).isNull()
    }

    @Test
    fun `Lv3 CONSECUTIVE_WEEK 미션의 주간 회고 현황을 포함한다`() {
        val userLevel = UserLevel(userId = userId, currentLevel = 3)
        val mission = Mission.consecutiveWeek(level = 3, weeks = 2)
        val userMission =
            UserMission(
                userId = userId,
                missionId = mission.id,
                status = MissionStatus.IN_PROGRESS,
                progress = 1,
            )

        whenever(userLevelRepository.findByUserId(userId)).thenReturn(userLevel)
        whenever(userMissionRepository.findCurrentMissionByUserId(userId)).thenReturn(userMission)
        whenever(missionRepository.findByLevel(3)).thenReturn(mission)
        whenever(retrospectiveRepository.findCompletedByUserIdAndPeriod(eq(userId), any(), any())).thenReturn(
            emptyList(),
        )

        val result = missionQueryService.getCurrentMission(userId)

        assertThat(result.currentLevel).isEqualTo(3)
        assertThat(result.mission.type).isEqualTo("CONSECUTIVE_WEEK")
        assertThat(result.weeklyStatus).isNotNull()
        assertThat(result.weeklyStatus!!.days).hasSize(7)
    }

    @Test
    fun `사용자 레벨이 없으면 UserLevelNotFoundException을 발생시킨다`() {
        whenever(userLevelRepository.findByUserId(userId)).thenReturn(null)

        assertThatThrownBy { missionQueryService.getCurrentMission(userId) }
            .isInstanceOf(UserLevelNotFoundException::class.java)
    }

    @Test
    fun `현재 미션이 없으면 CurrentMissionNotFoundException을 발생시킨다`() {
        val userLevel = UserLevel(userId = userId, currentLevel = 1)
        whenever(userLevelRepository.findByUserId(userId)).thenReturn(userLevel)
        whenever(userMissionRepository.findCurrentMissionByUserId(userId)).thenReturn(null)

        assertThatThrownBy { missionQueryService.getCurrentMission(userId) }
            .isInstanceOf(CurrentMissionNotFoundException::class.java)
    }

    @Test
    fun `미션 정의가 없으면 MissionDefinitionNotFoundException을 발생시킨다`() {
        val userLevel = UserLevel(userId = userId, currentLevel = 1)
        val userMission = UserMission(userId = userId, missionId = UUID.randomUUID())

        whenever(userLevelRepository.findByUserId(userId)).thenReturn(userLevel)
        whenever(userMissionRepository.findCurrentMissionByUserId(userId)).thenReturn(userMission)
        whenever(missionRepository.findByLevel(1)).thenReturn(null)

        assertThatThrownBy { missionQueryService.getCurrentMission(userId) }
            .isInstanceOf(MissionDefinitionNotFoundException::class.java)
    }

    @Test
    fun `레벨업 팝업을 반환한다`() {
        val userLevel = UserLevel(userId = userId, currentLevel = 1)
        val mission = Mission.firstRetro()
        val userMission =
            UserMission(
                userId = userId,
                missionId = mission.id,
                status = MissionStatus.IN_PROGRESS,
                levelUpPopupShown = false,
                failurePopupShown = true,
            )

        whenever(userLevelRepository.findByUserId(userId)).thenReturn(userLevel)
        whenever(userMissionRepository.findCurrentMissionByUserId(userId)).thenReturn(userMission)
        whenever(missionRepository.findByLevel(1)).thenReturn(mission)

        val result = missionQueryService.getCurrentMission(userId)

        assertThat(result.popup.exists).isTrue()
        assertThat(result.popup.type).isEqualTo("LEVEL_UP")
    }

    @Test
    fun `실패 팝업을 반환한다`() {
        val userLevel = UserLevel(userId = userId, currentLevel = 1)
        val mission = Mission.firstRetro()
        val userMission =
            UserMission(
                userId = userId,
                missionId = mission.id,
                status = MissionStatus.IN_PROGRESS,
                levelUpPopupShown = true,
                failurePopupShown = false,
            )

        whenever(userLevelRepository.findByUserId(userId)).thenReturn(userLevel)
        whenever(userMissionRepository.findCurrentMissionByUserId(userId)).thenReturn(userMission)
        whenever(missionRepository.findByLevel(1)).thenReturn(mission)

        val result = missionQueryService.getCurrentMission(userId)

        assertThat(result.popup.exists).isTrue()
        assertThat(result.popup.type).isEqualTo("FAILURE")
    }

    @Test
    fun `팝업이 없으면 exists가 false이다`() {
        val userLevel = UserLevel(userId = userId, currentLevel = 1)
        val mission = Mission.firstRetro()
        val userMission =
            UserMission(
                userId = userId,
                missionId = mission.id,
                status = MissionStatus.IN_PROGRESS,
                levelUpPopupShown = true,
                failurePopupShown = true,
            )

        whenever(userLevelRepository.findByUserId(userId)).thenReturn(userLevel)
        whenever(userMissionRepository.findCurrentMissionByUserId(userId)).thenReturn(userMission)
        whenever(missionRepository.findByLevel(1)).thenReturn(mission)

        val result = missionQueryService.getCurrentMission(userId)

        assertThat(result.popup.exists).isFalse()
        assertThat(result.popup.type).isNull()
    }
}
