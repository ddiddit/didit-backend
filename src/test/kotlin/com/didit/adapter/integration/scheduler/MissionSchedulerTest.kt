package com.didit.adapter.integration.scheduler

import com.didit.application.achievement.required.UserMissionRepository
import com.didit.domain.achievement.MissionStatus
import com.didit.domain.achievement.UserMission
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class MissionSchedulerTest {
    @Mock
    lateinit var userMissionRepository: UserMissionRepository

    @Mock
    lateinit var missionFailureProcessor: MissionFailureProcessor

    @InjectMocks
    lateinit var missionScheduler: MissionScheduler

    private val userId = UUID.randomUUID()
    private val missionId = UUID.randomUUID()

    private fun mission() =
        UserMission(
            userId = userId,
            missionId = missionId,
            status = MissionStatus.IN_PROGRESS,
        )

    @Test
    fun `만료된 Lv2 미션을 프로세서에 위임한다`() {
        val expiredMission = mission()
        whenever(userMissionRepository.findExpiredLv2Missions()).thenReturn(listOf(expiredMission))

        missionScheduler.checkExpiredLv2Missions()

        verify(missionFailureProcessor).failExpiredLv2(expiredMission)
    }

    @Test
    fun `만료된 Lv2 미션이 없으면 프로세서를 호출하지 않는다`() {
        whenever(userMissionRepository.findExpiredLv2Missions()).thenReturn(emptyList())

        missionScheduler.checkExpiredLv2Missions()

        verify(userMissionRepository).findExpiredLv2Missions()
    }

    @Test
    fun `한 Lv2 미션 처리가 실패해도 나머지 미션은 처리된다`() {
        val failing = mission()
        val succeeding = mission()
        whenever(userMissionRepository.findExpiredLv2Missions()).thenReturn(listOf(failing, succeeding))
        doThrow(RuntimeException("처리 실패")).whenever(missionFailureProcessor).failExpiredLv2(failing)

        missionScheduler.checkExpiredLv2Missions()

        verify(missionFailureProcessor).failExpiredLv2(failing)
        verify(missionFailureProcessor).failExpiredLv2(succeeding)
    }

    @Test
    fun `연속주차 미션을 프로세서에 위임한다`() {
        val consecutiveWeekMission = mission()
        whenever(userMissionRepository.findConsecutiveWeekMissionsInProgress())
            .thenReturn(listOf(consecutiveWeekMission))

        missionScheduler.checkConsecutiveWeekFailure()

        verify(missionFailureProcessor).failIfNoRetroLastWeek(consecutiveWeekMission)
    }

    @Test
    fun `연속주차 진행 미션이 없으면 프로세서를 호출하지 않는다`() {
        whenever(userMissionRepository.findConsecutiveWeekMissionsInProgress()).thenReturn(emptyList())

        missionScheduler.checkConsecutiveWeekFailure()

        verify(userMissionRepository).findConsecutiveWeekMissionsInProgress()
    }
}
