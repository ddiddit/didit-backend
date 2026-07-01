package com.didit.adapter.integration.scheduler

import com.didit.application.achievement.required.MissionRepository
import com.didit.application.achievement.required.UserMissionRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.achievement.MissionStatus
import com.didit.domain.achievement.UserMission
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class MissionSchedulerTest {
    @Mock
    lateinit var userMissionRepository: UserMissionRepository

    @Mock
    lateinit var missionRepository: MissionRepository

    @Mock
    lateinit var retrospectiveRepository: RetrospectiveRepository

    private lateinit var missionScheduler: MissionScheduler

    private val userId = UUID.randomUUID()
    private val missionId = UUID.randomUUID()

    @Test
    fun `만료된 Lv2 미션을 WAIT_CONFIRM 상태로 변경한다`() {
        val expiredMission =
            UserMission(
                userId = userId,
                missionId = missionId,
                status = MissionStatus.IN_PROGRESS,
                progress = 1,
                startedAt = LocalDateTime.now().minusDays(8),
            )

        missionScheduler = MissionScheduler(userMissionRepository, missionRepository, retrospectiveRepository)

        whenever(userMissionRepository.findExpiredLv2Missions()).thenReturn(listOf(expiredMission))
        whenever(userMissionRepository.save(any())).thenReturn(expiredMission)

        missionScheduler.checkExpiredLv2Missions()

        verify(userMissionRepository).findExpiredLv2Missions()
        verify(userMissionRepository).save(expiredMission)
    }

    @Test
    fun `만료된 Lv2 미션이 없으면 아무것도 변경하지 않는다`() {
        missionScheduler = MissionScheduler(userMissionRepository, missionRepository, retrospectiveRepository)

        whenever(userMissionRepository.findExpiredLv2Missions()).thenReturn(emptyList())

        missionScheduler.checkExpiredLv2Missions()

        verify(userMissionRepository).findExpiredLv2Missions()
    }

    @Test
    fun `지난주 회고가 없는 연속주차 미션을 WAIT_CONFIRM 상태로 변경한다`() {
        val consecutiveWeekMission =
            UserMission(
                userId = userId,
                missionId = missionId,
                status = MissionStatus.IN_PROGRESS,
                progress = 0,
            )

        missionScheduler = MissionScheduler(userMissionRepository, missionRepository, retrospectiveRepository)

        whenever(userMissionRepository.findConsecutiveWeekMissionsInProgress()).thenReturn(
            listOf(consecutiveWeekMission),
        )
        whenever(retrospectiveRepository.countCompletedByUserIdAndPeriod(any(), any(), any())).thenReturn(0)
        whenever(userMissionRepository.save(any())).thenReturn(consecutiveWeekMission)

        missionScheduler.checkConsecutiveWeekFailure()

        verify(userMissionRepository).findConsecutiveWeekMissionsInProgress()
        verify(retrospectiveRepository).countCompletedByUserIdAndPeriod(any(), any(), any())
        verify(userMissionRepository).save(consecutiveWeekMission)
    }

    @Test
    fun `지난주 회고가 있는 연속주차 미션은 변경하지 않는다`() {
        val consecutiveWeekMission =
            UserMission(
                userId = userId,
                missionId = missionId,
                status = MissionStatus.IN_PROGRESS,
                progress = 0,
            )

        missionScheduler = MissionScheduler(userMissionRepository, missionRepository, retrospectiveRepository)

        whenever(userMissionRepository.findConsecutiveWeekMissionsInProgress()).thenReturn(
            listOf(consecutiveWeekMission),
        )
        whenever(retrospectiveRepository.countCompletedByUserIdAndPeriod(any(), any(), any())).thenReturn(1)

        missionScheduler.checkConsecutiveWeekFailure()

        verify(userMissionRepository).findConsecutiveWeekMissionsInProgress()
        verify(retrospectiveRepository).countCompletedByUserIdAndPeriod(any(), any(), any())
    }

    @Test
    fun `연속주차 진행 미션이 없으면 아무것도 변경하지 않는다`() {
        missionScheduler = MissionScheduler(userMissionRepository, missionRepository, retrospectiveRepository)

        whenever(userMissionRepository.findConsecutiveWeekMissionsInProgress()).thenReturn(emptyList())

        missionScheduler.checkConsecutiveWeekFailure()

        verify(userMissionRepository).findConsecutiveWeekMissionsInProgress()
    }
}
