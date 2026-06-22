package com.didit.application.admin

import com.didit.application.audit.AuditReader
import com.didit.application.auth.required.UserRepository
import com.didit.application.inquiry.required.InquiryRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.inquiry.InquiryStatus
import com.didit.domain.retrospect.InputType
import com.didit.domain.retrospect.RetroStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest

@ExtendWith(MockitoExtension::class)
class AdminStatsServiceTest {
    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var inquiryRepository: InquiryRepository

    @Mock
    private lateinit var retrospectiveRepository: RetrospectiveRepository

    @Mock
    private lateinit var auditReader: AuditReader

    @InjectMocks
    private lateinit var adminStatsService: AdminStatsService

    @Test
    fun `대시보드 통계 조회 시 각 항목이 집계된다`() {
        whenever(userRepository.countByDeletedAtIsNull()).thenReturn(100L)
        whenever(userRepository.countByCreatedAtAfterAndDeletedAtIsNull(any())).thenReturn(3L)
        whenever(retrospectiveRepository.countByStatusAndDeletedAtIsNull(RetroStatus.COMPLETED)).thenReturn(500L)
        whenever(inquiryRepository.countByStatusAndDeletedAtIsNull(InquiryStatus.PENDING)).thenReturn(5L)
        whenever(auditReader.countDau(any())).thenReturn(80L)
        whenever(retrospectiveRepository.countByCompletedAtBetweenAndDeletedAtIsNull(any(), any())).thenReturn(20L)
        whenever(retrospectiveRepository.findWeeklyRetroTrend(any())).thenReturn(emptyList())
        whenever(retrospectiveRepository.sumInputTokens()).thenReturn(12000L)
        whenever(retrospectiveRepository.sumOutputTokens()).thenReturn(8000L)
        whenever(retrospectiveRepository.countUserAnswersByInputType(InputType.TEXT)).thenReturn(300L)
        whenever(retrospectiveRepository.countUserAnswersByInputType(InputType.STT)).thenReturn(150L)
        whenever(userRepository.findRecentUsers(any<PageRequest>())).thenReturn(emptyList())
        whenever(inquiryRepository.findTop5ByDeletedAtIsNullOrderByCreatedAtDesc()).thenReturn(emptyList())

        val result = adminStatsService.getStats()

        assertThat(result.totalUsers).isEqualTo(100L)
        assertThat(result.newUsersToday).isEqualTo(3L)
        assertThat(result.totalRetrospects).isEqualTo(500L)
        assertThat(result.unansweredInquiries).isEqualTo(5L)
        assertThat(result.dau).isEqualTo(80L)
        assertThat(result.todayRetrospects).isEqualTo(20L)
        assertThat(result.weeklyRetroTrend).isEmpty()
        assertThat(result.totalInputTokens).isEqualTo(12000L)
        assertThat(result.totalOutputTokens).isEqualTo(8000L)
        assertThat(result.textAnswerCount).isEqualTo(300L)
        assertThat(result.voiceAnswerCount).isEqualTo(150L)
        assertThat(result.recentUsers).isEmpty()
        assertThat(result.recentInquiries).isEmpty()
    }
}
