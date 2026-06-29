package com.didit.application.achievement

import com.didit.application.achievement.exception.CurrentMissionNotFoundException
import com.didit.application.achievement.required.MissionRepository
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserMissionServiceTest {
    @Mock
    lateinit var userMissionRepository: UserMissionRepository

    @Mock
    lateinit var missionRepository: MissionRepository

    private lateinit var userMissionService: UserMissionService

    private val userId = UUID.randomUUID()
    private val missionId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        userMissionService = UserMissionService(userMissionRepository, missionRepository)
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
        val userMission =
            UserMission(userId = userId, missionId = missionId, status = MissionStatus.IN_PROGRESS, progress = 2)
        val originalStartedAt = userMission.startedAt

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
