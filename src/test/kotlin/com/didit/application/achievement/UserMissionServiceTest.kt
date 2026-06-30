package com.didit.application.achievement

import com.didit.application.achievement.exception.CurrentMissionNotFoundException
import com.didit.application.achievement.required.MissionRepository
import com.didit.application.achievement.required.UserLevelRepository
import com.didit.application.achievement.required.UserMissionRepository
import com.didit.domain.achievement.Mission
import com.didit.domain.achievement.MissionStatus
import com.didit.domain.achievement.MissionType
import com.didit.domain.achievement.UserMission
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserMissionServiceTest {
    @Mock
    lateinit var userMissionRepository: UserMissionRepository

    @Mock
    lateinit var missionRepository: MissionRepository

    @Mock
    lateinit var userLevelRepository: UserLevelRepository

    private lateinit var userMissionService: UserMissionService

    private val userId = UUID.randomUUID()
    private val missionId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        userMissionService = UserMissionService(userMissionRepository, missionRepository, userLevelRepository)
    }

    @Test
    fun `레벨 정보가 없으면 UserLevel과 Lv1 미션을 생성한다`() {
        whenever(userLevelRepository.existsByUserId(userId)).thenReturn(false)
        whenever(missionRepository.findByLevel(1)).thenReturn(Mission.firstRetro())

        userMissionService.ensureInitialized(userId)

        verify(userLevelRepository).save(any())
        verify(userMissionRepository).save(any())
    }

    @Test
    fun `이미 레벨 정보가 있으면 아무것도 생성하지 않는다`() {
        whenever(userLevelRepository.existsByUserId(userId)).thenReturn(true)

        userMissionService.ensureInitialized(userId)

        verify(userLevelRepository, never()).save(any())
        verify(userMissionRepository, never()).save(any())
    }

    @Test
    fun `레벨업 팝업 확인을 처리한다`() {
        val userMission = UserMission(userId = userId, missionId = missionId)
        whenever(userMissionRepository.findCurrentMissionByUserId(userId)).thenReturn(userMission)
        whenever(userMissionRepository.save(any())).thenReturn(userMission)

        userMissionService.confirmLevelUp(userId)

        assertThat(userMission.levelUpPopupShown).isTrue()
        verify(userMissionRepository).save(userMission)
    }

    @Test
    fun `현재 미션이 없으면 CurrentMissionNotFoundException을 발생시킨다`() {
        whenever(userMissionRepository.findCurrentMissionByUserId(userId)).thenReturn(null)

        assertThatThrownBy { userMissionService.confirmLevelUp(userId) }
            .isInstanceOf(CurrentMissionNotFoundException::class.java)
    }

    @Test
    fun `Lv2 미션 재도전 시 진행률을 초기화하고 시작 시각을 갱신한다`() {
        val timeLimitedMission =
            Mission(
                id = missionId,
                level = 2,
                missionType = MissionType.TIME_LIMITED,
                targetCount = 3,
                title = "테스트",
                description = "테스트",
                durationDays = 7,
            )
        val originalStartedAt = LocalDateTime.now().minusDays(8)
        val userMission =
            UserMission(
                userId = userId,
                missionId = missionId,
                status = MissionStatus.IN_PROGRESS,
                progress = 2,
                startedAt = originalStartedAt,
            )

        whenever(userMissionRepository.findCurrentMissionByUserId(userId)).thenReturn(userMission)
        whenever(missionRepository.findAll()).thenReturn(listOf(timeLimitedMission))
        whenever(userMissionRepository.save(any())).thenReturn(userMission)

        userMissionService.retryMission(userId)

        assertThat(userMission.progress).isEqualTo(0)
        assertThat(userMission.startedAt).isNotEqualTo(originalStartedAt)
        assertThat(userMission.failurePopupShown).isTrue()
        verify(userMissionRepository).save(userMission)
    }

    @Test
    fun `Lv3 미션 재도전 시 진행률을 초기화한다`() {
        val consecutiveWeekMission =
            Mission(
                id = missionId,
                level = 3,
                missionType = MissionType.CONSECUTIVE_WEEK,
                targetCount = 2,
                title = "테스트",
                description = "테스트",
            )
        val userMission =
            UserMission(userId = userId, missionId = missionId, status = MissionStatus.IN_PROGRESS, progress = 1)

        whenever(userMissionRepository.findCurrentMissionByUserId(userId)).thenReturn(userMission)
        whenever(missionRepository.findAll()).thenReturn(listOf(consecutiveWeekMission))
        whenever(userMissionRepository.save(any())).thenReturn(userMission)

        userMissionService.retryMission(userId)

        assertThat(userMission.progress).isEqualTo(0)
        assertThat(userMission.failurePopupShown).isTrue()
        verify(userMissionRepository).save(userMission)
    }
}
