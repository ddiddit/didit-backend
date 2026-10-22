package com.didit.application.admin

import com.didit.application.auth.required.UserRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.InputType
import com.didit.domain.retrospect.RetroStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class AdminRetrospectiveStatsServiceTest {
    @Mock
    lateinit var retrospectiveRepository: RetrospectiveRepository

    @Mock
    lateinit var userRepository: UserRepository

    @Test
    fun `전체 회고 수에는 결과 없이 종료한 회고도 포함한다`() {
        whenever(retrospectiveRepository.countByStatusAndDeletedAtIsNull(RetroStatus.COMPLETED)).thenReturn(4)
        whenever(retrospectiveRepository.countByStatusAndDeletedAtIsNull(RetroStatus.PENDING)).thenReturn(1)
        whenever(retrospectiveRepository.countByStatusAndDeletedAtIsNull(RetroStatus.IN_PROGRESS)).thenReturn(2)
        whenever(retrospectiveRepository.countByStatusAndDeletedAtIsNull(RetroStatus.ABANDONED)).thenReturn(3)
        whenever(userRepository.countByDeletedAtIsNull()).thenReturn(2)
        whenever(retrospectiveRepository.countUserAnswersByInputType(InputType.TEXT)).thenReturn(5)
        whenever(retrospectiveRepository.countUserAnswersByInputType(InputType.STT)).thenReturn(1)
        whenever(retrospectiveRepository.findWeeklyRetroTrend(any())).thenReturn(emptyList())

        val result = AdminRetrospectiveStatsService(retrospectiveRepository, userRepository).getRetrospectiveStats()

        assertThat(result.total).isEqualTo(10)
        assertThat(result.completed).isEqualTo(4)
        assertThat(result.inProgress).isEqualTo(3)
        assertThat(result.completionRate).isEqualTo(40.0)
        assertThat(result.avgPerUser).isEqualTo(2.0)
    }
}
