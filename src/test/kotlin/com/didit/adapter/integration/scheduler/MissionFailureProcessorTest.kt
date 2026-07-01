package com.didit.adapter.integration.scheduler

import com.didit.application.achievement.required.UserMissionRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.achievement.MissionStatus
import com.didit.domain.achievement.UserMission
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
class MissionFailureProcessorTest {
    @Mock
    lateinit var userMissionRepository: UserMissionRepository

    @Mock
    lateinit var retrospectiveRepository: RetrospectiveRepository

    @InjectMocks
    lateinit var missionFailureProcessor: MissionFailureProcessor

    private fun mission() =
        UserMission(
            userId = UUID.randomUUID(),
            missionId = UUID.randomUUID(),
            status = MissionStatus.IN_PROGRESS,
        )

    @Test
    fun `failExpiredLv2 - 실패 대기 상태로 저장한다`() {
        val userMission = mission()
        whenever(userMissionRepository.save(any())).thenReturn(userMission)

        missionFailureProcessor.failExpiredLv2(userMission)

        verify(userMissionRepository).save(userMission)
    }

    @Test
    fun `failIfNoRetroLastWeek - 지난주 회고가 없으면 실패 처리한다`() {
        val userMission = mission()
        whenever(retrospectiveRepository.countCompletedByUserIdAndPeriod(any(), any(), any())).thenReturn(0)
        whenever(userMissionRepository.save(any())).thenReturn(userMission)

        missionFailureProcessor.failIfNoRetroLastWeek(userMission)

        verify(userMissionRepository).save(userMission)
    }

    @Test
    fun `failIfNoRetroLastWeek - 지난주 회고가 있으면 실패 처리하지 않는다`() {
        val userMission = mission()
        whenever(retrospectiveRepository.countCompletedByUserIdAndPeriod(any(), any(), any())).thenReturn(1)

        missionFailureProcessor.failIfNoRetroLastWeek(userMission)

        verify(userMissionRepository, never()).save(any())
    }
}
